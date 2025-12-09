package com.poolpro.manager.billing.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties(prefix = "stripe")
data class StripeProperties(
    @DefaultValue("") val apiKey: String,
    @DefaultValue("") val webhookSecret: String,
    @DefaultValue("") val publishableKey: String
)









