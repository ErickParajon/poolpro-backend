package com.poolpro.manager.auth

import com.poolpro.manager.common.ApiEnvelope
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/auth")
class AuthController(
    private val refreshTokenService: RefreshTokenService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/refresh")
    suspend fun refreshToken(
        @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<ApiEnvelope<RefreshTokenResponse>> {
        val refreshToken = request.refreshToken.trim()
        if (refreshToken.isEmpty() || !refreshTokenService.validate(refreshToken)) {
            logger.warn("Rejected refresh token request at {}", Instant.now())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        logger.debug("Refreshing token for {} at {}", refreshToken.take(6) + "***", Instant.now())

        val rotated = refreshTokenService.rotate(refreshToken)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val response = RefreshTokenResponse(
            accessToken = rotated.accessToken,
            refreshToken = rotated.refreshToken
        )

        return ResponseEntity.ok(ApiEnvelope(data = response))
    }
}

