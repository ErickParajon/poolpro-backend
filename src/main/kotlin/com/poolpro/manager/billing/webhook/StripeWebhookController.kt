package com.poolpro.manager.billing.webhook

import com.stripe.model.Event
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/webhooks/stripe")
class StripeWebhookController(
    private val stripeWebhookService: StripeWebhookService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    suspend fun handleStripeWebhook(
        @RequestHeader("Stripe-Signature", required = false) signature: String?,
        @RequestBody payload: String
    ): ResponseEntity<Void> {
        val event = stripeWebhookService.parseEvent(payload, signature)
        logger.info("Evento Stripe recibido: type={}, id={}", event.type, event.id)

        // TODO: delegar en un orquestador para manejar eventos relevantes
        processEvent(event)

        return ResponseEntity.status(HttpStatus.OK).build()
    }

    private fun processEvent(event: Event) {
        when (event.type) {
            "invoice.payment_succeeded" -> logger.debug("Procesar pago exitoso")
            "invoice.payment_failed" -> logger.debug("Procesar pago fallido")
            "customer.subscription.deleted" -> logger.debug("Procesar suscripción cancelada")
            else -> logger.debug("Evento Stripe ignorado: {}", event.type)
        }
    }
}


















