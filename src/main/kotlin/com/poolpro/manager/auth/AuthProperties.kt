package com.poolpro.manager.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties(prefix = "auth")
data class AuthProperties(
    @DefaultValue("{}") val refreshToken: RefreshTokenProperties = RefreshTokenProperties()
) {
    data class RefreshTokenProperties(
        @DefaultValue("") val seeds: List<String> = emptyList()
    )
}









