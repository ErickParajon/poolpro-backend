package com.poolpro.manager.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream
import java.io.StringReader

@Configuration
@EnableConfigurationProperties(FirebaseProperties::class)
class FirebaseConfiguration(
    private val firebaseProperties: FirebaseProperties
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @PostConstruct
    fun initializeFirebase() {
        if (firebaseProperties.projectId.isBlank()) {
            logger.warn("Firebase no está configurado. Usando modo desarrollo con tokens simples.")
            return
        }
        
        try {
            val credentials = when {
                firebaseProperties.credentialsPath.isNotBlank() -> {
                    // Cargar desde archivo
                    logger.info("Cargando credenciales de Firebase desde: ${firebaseProperties.credentialsPath}")
                    FileInputStream(firebaseProperties.credentialsPath).use { stream ->
                        GoogleCredentials.fromStream(stream)
                    }
                }
                firebaseProperties.credentialsJson.isNotBlank() -> {
                    // Cargar desde JSON string
                    logger.info("Cargando credenciales de Firebase desde variable de entorno")
                    StringReader(firebaseProperties.credentialsJson).use { reader ->
                        GoogleCredentials.fromStream(reader.readText().byteInputStream())
                    }
                }
                else -> {
                    // Usar Application Default Credentials (para GCP)
                    logger.info("Usando Application Default Credentials de Firebase")
                    GoogleCredentials.getApplicationDefault()
                }
            }
            
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(firebaseProperties.projectId)
                .build()
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                logger.info("Firebase inicializado correctamente para proyecto: ${firebaseProperties.projectId}")
            } else {
                logger.info("Firebase ya estaba inicializado")
            }
        } catch (e: Exception) {
            logger.error("Error al inicializar Firebase: ${e.message}", e)
            logger.warn("El backend continuará funcionando en modo desarrollo con tokens simples")
        }
    }
    
    @Bean
    fun firebaseAuth(): FirebaseAuth? {
        return if (FirebaseApp.getApps().isNotEmpty()) {
            FirebaseAuth.getInstance()
        } else {
            null
        }
    }
}

