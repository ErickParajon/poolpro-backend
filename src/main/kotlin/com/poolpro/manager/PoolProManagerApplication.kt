package com.poolpro.manager

import com.poolpro.manager.auth.AuthProperties
import com.poolpro.manager.firebase.FirebaseProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AuthProperties::class, FirebaseProperties::class)
class PoolProManagerApplication

fun main(args: Array<String>) {
    runApplication<PoolProManagerApplication>(*args)
}

