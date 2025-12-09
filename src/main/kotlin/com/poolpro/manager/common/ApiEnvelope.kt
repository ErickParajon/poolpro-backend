package com.poolpro.manager.common

import java.time.Instant
import java.util.UUID

data class ApiEnvelope<T>(
    val data: T? = null,
    val error: String? = null,
    val meta: ApiMeta = ApiMeta()
) {
    init {
        require(data != null || error != null) {
            "ApiEnvelope debe tener data o error"
        }
        require(data == null || error == null) {
            "ApiEnvelope no puede tener data y error al mismo tiempo"
        }
    }
}

data class ApiMeta(
    val requestId: String = UUID.randomUUID().toString(),
    val timestamp: String = Instant.now().toString()
)




