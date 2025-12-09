package com.poolpro.manager.membership

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.DialectResolver
import org.springframework.r2dbc.core.DatabaseClient

@Configuration
class R2dbcConfiguration(
    private val databaseClient: DatabaseClient
) {
    
    @Bean
    fun r2dbcCustomConversions(): R2dbcCustomConversions {
        val converters = listOf<Converter<*, *>>(
            MembershipStatusWritingConverter(),
            MembershipStatusReadingConverter()
        )
        val dialect = DialectResolver.getDialect(databaseClient.connectionFactory)
        return R2dbcCustomConversions(
            org.springframework.data.convert.CustomConversions.StoreConversions.of(dialect.simpleTypeHolder),
            converters
        )
    }
}

