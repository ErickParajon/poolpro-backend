package com.poolpro.manager.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.core.convert.converter.Converter
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import com.poolpro.manager.security.SimpleJwtDecoder
import com.poolpro.manager.firebase.FirebaseJwtDecoder
import com.google.firebase.auth.FirebaseAuth

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val firebaseAuth: FirebaseAuth? = null
) {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges.pathMatchers("/actuator/health", "/actuator/info").permitAll()
                exchanges.pathMatchers("/v1/auth/refresh").permitAll()
                exchanges.anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth ->
                oauth.jwt { jwt ->
                    // Usar Firebase JWT decoder si está configurado, sino usar SimpleJwtDecoder para desarrollo
                    val decoder = if (firebaseAuth != null) {
                        FirebaseJwtDecoder(firebaseAuth)
                    } else {
                        SimpleJwtDecoder()
                    }
                    jwt.jwtDecoder(decoder)
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            .build()

    @Bean
    fun jwtAuthenticationConverter(): ReactiveJwtAuthenticationConverterAdapter {
        val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName("roles")
            setAuthorityPrefix("ROLE_")
        }
        val converter = JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
        }
        return ReactiveJwtAuthenticationConverterAdapter(converter)
    }
}

fun currentOperatorId(jwt: Jwt): Mono<String> {
    // El SimpleJwtDecoder pone el token como operator_id
    val operatorId = jwt.claims["operator_id"] as? String
        ?: jwt.subject // Fallback al subject si no hay operator_id
    return Mono.justOrEmpty(operatorId)
}

