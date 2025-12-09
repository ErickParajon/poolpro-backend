package com.poolpro.manager.auth

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RefreshTokenService(
    private val repository: RefreshTokenRepository,
    private val properties: AuthProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        runBlocking {
            seedTokens()
        }
    }

    private suspend fun seedTokens() {
        properties.refreshToken.seeds
            .map(String::trim)
            .filter(String::isNotBlank)
            .forEach { seed ->
                val exists = repository.existsById(seed)
                if (!exists) {
                    repository.save(
                        RefreshTokenEntity(
                            token = seed,
                            ownerId = "seed-token",
                            issuedAt = Instant.now(),
                            expiresAt = null,
                            newRecord = true
                        )
                    ).markPersisted()
                    logger.info("Registered seed refresh token")
                }
            }
    }

    suspend fun validate(refreshToken: String): Boolean {
        val entity = repository.findById(refreshToken) ?: return false
        if (entity.expiresAt != null && entity.expiresAt.isBefore(Instant.now())) {
            repository.deleteById(refreshToken)
            return false
        }
        return true
    }

    suspend fun rotate(refreshToken: String): RefreshTokenPair? {
        val existing = repository.findById(refreshToken) ?: return null
        repository.deleteById(refreshToken)

        val pair = newPair(existing.ownerId, existing.expiresAt)
        repository.save(
            RefreshTokenEntity(
                token = pair.refreshToken,
                ownerId = existing.ownerId,
                issuedAt = Instant.now(),
                expiresAt = existing.expiresAt,
                newRecord = true
            )
        ).markPersisted()

        return pair
    }

    suspend fun register(
        ownerId: String,
        validitySeconds: Long? = 86_400
    ): RefreshTokenPair {
        val expiresAt = validitySeconds?.let { Instant.now().plusSeconds(it) }
        val pair = newPair(ownerId, expiresAt)
        repository.save(
            RefreshTokenEntity(
                token = pair.refreshToken,
                ownerId = ownerId,
                issuedAt = Instant.now(),
                expiresAt = expiresAt,
                newRecord = true
            )
        ).markPersisted()
        return pair
    }

    private fun newPair(ownerId: String, expiresAt: Instant?): RefreshTokenPair =
        RefreshTokenPair(
            accessToken = UUID.randomUUID().toString(),
            refreshToken = UUID.randomUUID().toString(),
            ownerId = ownerId,
            expiresAt = expiresAt
        )

    data class RefreshTokenPair(
        val accessToken: String,
        val refreshToken: String,
        val ownerId: String,
        val expiresAt: Instant?
    )
}

