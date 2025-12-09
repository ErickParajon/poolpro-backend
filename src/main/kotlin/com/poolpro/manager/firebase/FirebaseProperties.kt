package com.poolpro.manager.firebase

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties(prefix = "firebase")
data class FirebaseProperties(
    @DefaultValue("") val projectId: String,
    @DefaultValue("") val credentialsPath: String,
    @DefaultValue("") val credentialsJson: String
)



