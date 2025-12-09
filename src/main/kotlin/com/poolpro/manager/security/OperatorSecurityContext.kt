package com.poolpro.manager.security

import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import reactor.core.publisher.Mono
import java.util.UUID

object OperatorSecurityContext {

    fun currentOperatorId(): Mono<UUID> =
        ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val operatorId =
                    (context.authentication?.principal as? Jwt)
                        ?.claims
                        ?.get("operator_id") as? String
                Mono.justOrEmpty(operatorId)
            }
            .flatMap { id ->
                Mono.justOrEmpty(runCatching { UUID.fromString(id) }.getOrNull())
            }
}

