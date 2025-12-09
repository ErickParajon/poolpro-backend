package com.poolpro.manager.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties(prefix = "twilio")
data class TwilioProperties(
    @DefaultValue("") val accountSid: String,
    @DefaultValue("") val authToken: String,
    @DefaultValue("") val messagingServiceSid: String
)


