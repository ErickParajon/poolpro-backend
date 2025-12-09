package com.poolpro.manager.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties(prefix = "sendgrid")
data class SendGridProperties(
    @DefaultValue("") val apiKey: String
)


