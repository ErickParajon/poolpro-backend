package com.poolpro.manager.billing.config

import com.stripe.Stripe
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(StripeProperties::class)
class StripeConfiguration(
    private val stripeProperties: StripeProperties
) {

    @PostConstruct
    fun configureStripe() {
        if (stripeProperties.apiKey.isNotBlank()) {
            Stripe.apiKey = stripeProperties.apiKey
        }
    }

    @Bean
    fun stripeWebhookSecret(): String = stripeProperties.webhookSecret
    
    @Bean
    fun stripePublishableKey(): String = stripeProperties.publishableKey
}

