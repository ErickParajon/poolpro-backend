package com.poolpro.manager.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Decoder JWT reactivo para Firebase
 * Valida tokens JWT emitidos por Firebase Authentication
 */
class FirebaseJwtDecoder(
    private val firebaseAuth: FirebaseAuth
) : ReactiveJwtDecoder {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override fun decode(token: String): Mono<Jwt> {
        return Mono.fromCallable {
            try {
                // Verificar el token con Firebase
                // Firebase Admin SDK para Java usa Task que necesita ser convertido
                val task = firebaseAuth.verifyIdTokenAsync(token)
                
                // Usar reflection para acceder a métodos de Task
                // Firebase Admin SDK usa com.google.android.gms.tasks.Task
                val taskClass = task.javaClass
                val isCompleteMethod = taskClass.getMethod("isComplete")
                val isSuccessfulMethod = taskClass.getMethod("isSuccessful")
                val getExceptionMethod = taskClass.getMethod("getException")
                val getResultMethod = try {
                    taskClass.getMethod("getResult")
                } catch (e: NoSuchMethodException) {
                    // Intentar con getResult(Class) si existe
                    taskClass.getMethod("getResult", Class::class.java)
                }
                
                // Esperar a que el task complete (polling con timeout)
                var attempts = 0
                val maxAttempts = 100 // 10 segundos máximo
                while (!(isCompleteMethod.invoke(task) as Boolean) && attempts < maxAttempts) {
                    Thread.sleep(100)
                    attempts++
                }
                
                val isComplete = isCompleteMethod.invoke(task) as Boolean
                if (!isComplete) {
                    throw TimeoutException("Firebase Task timeout after 10 seconds")
                }
                
                val isSuccessful = isSuccessfulMethod.invoke(task) as Boolean
                val decodedToken: FirebaseToken = if (isSuccessful) {
                    // Obtener el resultado usando reflection
                    if (getResultMethod.parameterCount == 0) {
                        getResultMethod.invoke(task) as FirebaseToken
                    } else {
                        getResultMethod.invoke(task, FirebaseToken::class.java) as FirebaseToken
                    }
                } else {
                    val exception = getExceptionMethod.invoke(task) as? Exception
                        ?: Exception("Task failed without exception")
                    throw exception
                }
                
                // Extraer claims
                val uid = decodedToken.uid
                val email = decodedToken.email
                val operatorId = decodedToken.claims["operator_id"] as? String ?: uid
                
                // Obtener iat y exp de los claims (Firebase los incluye en los claims)
                val claims = decodedToken.claims
                val issuedAt = (claims["iat"] as? Number)?.toLong()?.let { Instant.ofEpochSecond(it) } 
                    ?: Instant.now()
                val expiresAt = (claims["exp"] as? Number)?.toLong()?.let { Instant.ofEpochSecond(it) } 
                    ?: Instant.now().plusSeconds(3600)
                
                // Crear JWT para Spring Security
                Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .header("typ", "JWT")
                    .claim("uid", uid)
                    .claim("operator_id", operatorId)
                    .claim("email", email)
                    .claim("sub", uid)
                    .issuedAt(issuedAt)
                    .expiresAt(expiresAt)
                    .build()
            } catch (e: FirebaseAuthException) {
                logger.warn("Token de Firebase inválido: ${e.message}")
                throw OAuth2AuthenticationException(
                    OAuth2Error("invalid_token", "Token de Firebase inválido: ${e.message}", null),
                    e
                )
            } catch (e: Exception) {
                logger.error("Error al decodificar token de Firebase: ${e.message}", e)
                throw OAuth2AuthenticationException(
                    OAuth2Error("invalid_token", "Error al procesar token: ${e.message}", null),
                    e
                )
            }
        }
    }
}

