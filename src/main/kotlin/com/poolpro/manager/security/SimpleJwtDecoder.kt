package com.poolpro.manager.security

import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Decoder JWT simple reactivo para desarrollo que acepta tokens no-JWT (UUIDs simples)
 * En producción, esto debe ser reemplazado por un ReactiveJwtDecoder real
 */
class SimpleJwtDecoder : ReactiveJwtDecoder {
    
    override fun decode(token: String): Mono<Jwt> {
        // Para desarrollo: crear un JWT mock desde cualquier token
        // En producción, esto debe validar y decodificar JWT reales
        return Mono.fromCallable {
            try {
                Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("operator_id", token) // Usar el token como operator_id
                    .claim("sub", token)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build()
            } catch (e: Exception) {
                throw OAuth2AuthenticationException(
                    OAuth2Error("invalid_token", "Token inválido: ${e.message}", null),
                    e
                )
            }
        }
    }
}


