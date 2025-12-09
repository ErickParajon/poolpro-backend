package com.poolpro.manager.membership

import com.poolpro.manager.billing.stripe.StripeService
import com.poolpro.manager.billing.stripe.StripeServiceException
import com.poolpro.manager.common.ApiEnvelope
import com.poolpro.manager.security.currentOperatorId
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/v1")
@Suppress("unused")
class MembershipController(
    private val membershipService: MembershipService,
    private val stripeService: StripeService? = null,
    private val stripePublishableKey: String? = null
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/memberships")
    suspend fun listMemberships(
        @AuthenticationPrincipal jwt: Jwt?
    ): ResponseEntity<ApiEnvelope<List<MembershipDto>>> {
        logger.info("=== GET /v1/memberships recibido ===")
        
        if (jwt == null) {
            logger.warn("Request sin JWT - no autenticado")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        
        logger.info("JWT recibido: subject={}, claims={}", jwt.subject, jwt.claims.keys)
        
        val operatorId = currentOperatorId(jwt).awaitSingleOrNull()
        if (operatorId == null) {
            logger.warn("No se pudo extraer operatorId del JWT")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        
        logger.info("Listing memberships for operator: {}", operatorId.take(8) + "...")
        
        val memberships = membershipService.listMemberships(operatorId)
        
        logger.info("Retornando {} membresías", memberships.size)
        return ResponseEntity.ok(ApiEnvelope(data = memberships))
    }

    @GetMapping("/customers/{clientId}/membership")
    suspend fun getMembership(
        @PathVariable clientId: String,
        @AuthenticationPrincipal jwt: Jwt?
    ): ResponseEntity<ApiEnvelope<MembershipDto>> {
        if (jwt == null) {
            logger.warn("Request sin JWT - no autenticado")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        
        val operatorId = currentOperatorId(jwt).awaitSingleOrNull() 
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        
        logger.debug("Getting membership for client: {} by operator: {}", clientId, operatorId.take(8) + "...")
        
        val membership = membershipService.getMembership(clientId, operatorId)
            ?: membershipService.createOrUpdateMembership(
                clientId = clientId,
                operatorId = operatorId,
                status = MembershipStatus.NOT_CONFIGURED
            )
        
        return ResponseEntity.ok(ApiEnvelope(data = membership))
    }

    @PutMapping("/customers/{clientId}/membership/plan")
    suspend fun upsertMembershipPlan(
        @PathVariable clientId: String,
        @RequestBody request: UpdateMembershipPlanRequest,
        @AuthenticationPrincipal jwt: Jwt?
    ): ResponseEntity<ApiEnvelope<MembershipDto>> {
        logger.info("=== PUT /v1/customers/{}/membership/plan recibido ===", clientId)
        logger.info("Request: amount={}, currency={}, billingDay={}, channel={}", 
            request.amount, request.currency, request.billingDay, request.channel)
        
        if (jwt == null) {
            logger.warn("Request sin JWT - no autenticado")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        
        val operatorId = currentOperatorId(jwt).awaitSingleOrNull() 
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        
        logger.info("Upserting membership plan for client: {} by operator: {}", clientId, operatorId.take(8) + "...")
        
        return try {
            val membership = membershipService.updateMembershipPlan(
                clientId = clientId,
                operatorId = operatorId,
                amount = request.amount,
                currency = request.currency ?: "USD",
                billingDay = request.billingDay,
                channel = request.channel,
                message = request.message
            )
            
            logger.info("Plan actualizado exitosamente para cliente: {}", clientId)
            ResponseEntity.ok(ApiEnvelope(data = membership))
        } catch (e: Exception) {
            logger.error("Error al actualizar plan de membresía: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiEnvelope(error = "Error al guardar plan: ${e.message}"))
        }
    }

    @PostMapping("/customers/{clientId}/membership/plan/send")
    suspend fun sendMembershipPlan(
        @PathVariable clientId: String,
        @RequestBody request: SendMembershipPlanRequest,
        @AuthenticationPrincipal jwt: Jwt?
    ): ResponseEntity<ApiEnvelope<MembershipDto>> {
        logger.info("=== POST /v1/customers/{}/membership/plan/send recibido ===", clientId)
        logger.info("Request: channel={}", request.channel)
        
        if (jwt == null) {
            logger.warn("Request sin JWT - no autenticado")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        
        val operatorId = currentOperatorId(jwt).awaitSingleOrNull() 
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        
        logger.info("Sending membership plan for client: {} via channel: {} by operator: {}", 
            clientId, request.channel, operatorId.take(8) + "...")
        
        return try {
            val membership = membershipService.sendMembershipPlan(
                clientId = clientId,
                operatorId = operatorId,
                channel = request.channel,
                clientEmail = request.clientEmail,
                clientPhone = request.clientPhone,
                clientName = request.clientName
            )
            
            logger.info("Plan enviado exitosamente para cliente: {} via {}", clientId, request.channel)
            
            ResponseEntity.ok(ApiEnvelope(data = membership))
        } catch (e: IllegalArgumentException) {
            logger.warn("Error al enviar plan (membership no encontrada): {}", e.message)
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiEnvelope(error = e.message))
        } catch (e: Exception) {
            logger.error("Error inesperado al enviar plan: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiEnvelope(error = "Error al enviar plan de membresía: ${e.message}"))
        }
    }

    @PostMapping("/customers/{clientId}/membership/payment-method/setup-intent")
    suspend fun createSetupIntent(
        @PathVariable clientId: String,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiEnvelope<SetupIntentDto>> {
        val operatorId = currentOperatorId(jwt).awaitSingleOrNull() 
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        
        logger.debug("Creating setup intent for client: {}", clientId)
        
        return try {
            if (stripeService == null) {
                logger.warn("StripeService no está disponible, usando mock")
                // Fallback a mock si Stripe no está configurado
                val setupIntent = SetupIntentDto(
                    clientSecret = "seti_mock_secret_${System.currentTimeMillis()}",
                    customerId = clientId,
                    ephemeralKey = "ephkey_mock_${System.currentTimeMillis()}",
                    publishableKey = stripePublishableKey,
                    merchantName = "PoolPro Manager"
                )
                return ResponseEntity.ok(ApiEnvelope(data = setupIntent))
            }
            
            // Obtener email del JWT si está disponible
            val email = jwt.claims["email"] as? String
            
            // Crear o obtener cliente de Stripe
            val customer = stripeService.getOrCreateCustomer(
                clientId = clientId,
                email = email
            )
            
            // Crear SetupIntent
            val setupIntent = stripeService.createSetupIntent(customer.id)
            
            // Crear EphemeralKey
            val ephemeralKey = stripeService.createEphemeralKey(customer.id)
            
            val setupIntentDto = SetupIntentDto(
                clientSecret = setupIntent.clientSecret,
                customerId = customer.id,
                ephemeralKey = ephemeralKey.secret,
                publishableKey = stripePublishableKey,
                merchantName = "PoolPro Manager"
            )
            
            logger.info("SetupIntent creado exitosamente para client: {} (Stripe customer: {})", 
                clientId, customer.id)
            
            ResponseEntity.ok(ApiEnvelope(data = setupIntentDto))
            
        } catch (e: StripeServiceException) {
            logger.error("Error al crear SetupIntent: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiEnvelope(error = "Error al crear SetupIntent: ${e.message}"))
        } catch (e: Exception) {
            logger.error("Error inesperado al crear SetupIntent: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiEnvelope(error = "Error al crear SetupIntent"))
        }
    }

    @PostMapping("/customers/{clientId}/membership/payment-method")
    suspend fun attachPaymentMethod(
        @PathVariable clientId: String,
        @RequestBody request: AttachPaymentMethodRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiEnvelope<MembershipDto>> {
        val operatorId = currentOperatorId(jwt).awaitSingleOrNull() 
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        
        logger.debug("Attaching payment method for client: {}", clientId)
        
        return try {
            var stripePaymentMethodId: String? = null
            var brand = "unknown"
            var last4 = ""
            var expMonth = request.expMonth ?: 12
            var expYear = request.expYear ?: 2025
            var holderName = request.holderName ?: ""
            
            if (stripeService != null && request.paymentMethodId != null) {
                // Si se proporciona un paymentMethodId de Stripe (desde la app Android)
                // Adjuntarlo al customer
                try {
                    val email = jwt.claims["email"] as? String
                    val customer = stripeService.getOrCreateCustomer(
                        clientId = clientId,
                        email = email
                    )
                    
                    val paymentMethod = stripeService.attachPaymentMethodToCustomer(
                        paymentMethodId = request.paymentMethodId,
                        customerId = customer.id
                    )
                    
                    stripePaymentMethodId = paymentMethod.id
                    brand = paymentMethod.card?.brand ?: "unknown"
                    last4 = paymentMethod.card?.last4 ?: ""
                    expMonth = paymentMethod.card?.expMonth?.toInt() ?: (request.expMonth ?: 12)
                    expYear = paymentMethod.card?.expYear?.toInt() ?: (request.expYear ?: 2025)
                    holderName = paymentMethod.billingDetails?.name ?: (request.holderName ?: "")
                    
                    logger.info("PaymentMethod de Stripe adjuntado: {} para client: {}", 
                        paymentMethod.id, clientId)
                        
                } catch (e: StripeServiceException) {
                    logger.warn("Error al adjuntar PaymentMethod de Stripe: {}", e.message)
                    // Continuar con los datos proporcionados en el request
                }
            } else if (stripeService != null && request.cardNumber != null) {
                // Fallback: crear PaymentMethod desde los datos de la tarjeta
                // NOTA: En producción, esto NO debería usarse. La tokenización debe hacerse en el cliente.
                logger.warn("Creando PaymentMethod desde datos de tarjeta (no recomendado en producción)")
                val cleanCardNumber = request.cardNumber.replace(Regex("[\\s-]"), "")
                
                val paymentMethod = stripeService.createPaymentMethod(
                    cardNumber = cleanCardNumber,
                    expMonth = request.expMonth ?: 12,
                    expYear = request.expYear ?: 2025,
                    cvc = request.cvv
                )
                
                val email = jwt.claims["email"] as? String
                val customer = stripeService.getOrCreateCustomer(
                    clientId = clientId,
                    email = email
                )
                
                stripeService.attachPaymentMethodToCustomer(
                    paymentMethodId = paymentMethod.id,
                    customerId = customer.id
                )
                
                stripePaymentMethodId = paymentMethod.id
                brand = paymentMethod.card?.brand ?: determineCardBrand(cleanCardNumber)
                last4 = paymentMethod.card?.last4 ?: cleanCardNumber.takeLast(4)
                expMonth = paymentMethod.card?.expMonth?.toInt() ?: (request.expMonth ?: 12)
                expYear = paymentMethod.card?.expYear?.toInt() ?: (request.expYear ?: 2025)
                
            } else {
                // Sin Stripe o sin datos suficientes, usar datos del request
                val cleanCardNumber = request.cardNumber?.replace(Regex("[\\s-]"), "") ?: ""
                brand = determineCardBrand(cleanCardNumber)
                last4 = cleanCardNumber.takeLast(4)
            }
            
            val membership = membershipService.attachPaymentMethod(
                clientId = clientId,
                operatorId = operatorId,
                brand = brand,
                last4 = last4,
                expMonth = expMonth,
                expYear = expYear,
                holderName = holderName,
                stripePaymentMethodId = stripePaymentMethodId
            )
            
            logger.info("Método de pago adjuntado para cliente: {} ({} ****{})", 
                clientId, brand, last4)
            
            ResponseEntity.ok(ApiEnvelope(data = membership))
            
        } catch (e: IllegalArgumentException) {
            logger.warn("Error al adjuntar método de pago: {}", e.message)
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiEnvelope(error = e.message))
        } catch (e: StripeServiceException) {
            logger.error("Error de Stripe al adjuntar método de pago: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiEnvelope(error = "Error al procesar método de pago: ${e.message}"))
        } catch (e: Exception) {
            logger.error("Error inesperado al adjuntar método de pago: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiEnvelope(error = "Error al adjuntar método de pago"))
        }
    }

    @PostMapping("/customers/{clientId}/membership/cancel")
    suspend fun cancelMembership(
        @PathVariable clientId: String,
        @RequestBody request: CancelMembershipRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiEnvelope<MembershipDto>> {
        val operatorId = currentOperatorId(jwt).awaitSingleOrNull() 
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        
        logger.debug("Cancelling membership for client: {} with reason: {}", 
            clientId, request.reason)
        
        return try {
            val membership = membershipService.cancelMembership(
                clientId = clientId,
                operatorId = operatorId,
                reason = request.reason
            )
            
            logger.info("Membresía cancelada para cliente: {}", clientId)
            ResponseEntity.ok(ApiEnvelope(data = membership))
        } catch (e: IllegalArgumentException) {
            logger.warn("Error al cancelar membresía: {}", e.message)
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiEnvelope(error = e.message))
        } catch (e: Exception) {
            logger.error("Error inesperado al cancelar membresía: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiEnvelope(error = "Error al cancelar membresía"))
        }
    }

    @PostMapping("/customers/{clientId}/membership/reactivate")
    suspend fun reactivateMembership(
        @PathVariable clientId: String,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiEnvelope<MembershipDto>> {
        val operatorId = currentOperatorId(jwt).awaitSingleOrNull() 
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        
        logger.debug("Reactivating membership for client: {}", clientId)
        
        return try {
            val membership = membershipService.reactivateMembership(
                clientId = clientId,
                operatorId = operatorId
            )
            
            logger.info("Membresía reactivada para cliente: {}", clientId)
            ResponseEntity.ok(ApiEnvelope(data = membership))
        } catch (e: IllegalArgumentException) {
            logger.warn("Error al reactivar membresía: {}", e.message)
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiEnvelope(error = e.message))
        } catch (e: IllegalStateException) {
            logger.warn("Error de estado al reactivar membresía: {}", e.message)
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiEnvelope(error = e.message))
        } catch (e: Exception) {
            logger.error("Error inesperado al reactivar membresía: {}", e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiEnvelope(error = "Error al reactivar membresía"))
        }
    }
    
    /**
     * Determina el brand de una tarjeta basado en su número (BIN - Bank Identification Number)
     */
    private fun determineCardBrand(cardNumber: String): String {
        if (cardNumber.isEmpty()) return "unknown"
        
        // Remover espacios y guiones
        val clean = cardNumber.replace(Regex("[\\s-]"), "")
        
        return when {
            clean.startsWith("4") -> "visa"
            clean.startsWith("5") && clean.length >= 2 -> {
                val firstTwo = clean.substring(0, 2).toIntOrNull() ?: 0
                if (firstTwo >= 51 && firstTwo <= 55) "mastercard" else "unknown"
            }
            clean.startsWith("34") || clean.startsWith("37") -> "amex"
            clean.startsWith("6") -> "discover"
            else -> "unknown"
        }
    }
}

