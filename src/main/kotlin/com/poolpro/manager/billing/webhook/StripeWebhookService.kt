package com.poolpro.manager.billing.webhook

import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.net.Webhook
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StripeWebhookService(
    private val stripeWebhookSecret: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun parseEvent(payload: String, signatureHeader: String?): Event =
        try {
            Webhook.constructEvent(
                payload,
                signatureHeader,
                stripeWebhookSecret
            )
        } catch (ex: SignatureVerificationException) {
            logger.warn("Firma inválida en webhook de Stripe: {}", ex.message)
            throw ex
        }
}


















