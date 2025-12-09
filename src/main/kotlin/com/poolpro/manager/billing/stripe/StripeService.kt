package com.poolpro.manager.billing.stripe

import com.stripe.exception.StripeException
import com.stripe.model.Customer
import com.stripe.model.EphemeralKey
import com.stripe.model.PaymentMethod
import com.stripe.model.SetupIntent
import com.stripe.param.CustomerCreateParams
import com.stripe.param.EphemeralKeyCreateParams
import com.stripe.param.PaymentMethodAttachParams
import com.stripe.param.PaymentMethodCreateParams
import com.stripe.param.SetupIntentCreateParams
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Servicio para interactuar con la API de Stripe
 */
@Service
class StripeService {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * Crea o obtiene un cliente de Stripe para un cliente de PoolPro
     * Usa el clientId como identificador externo para encontrar clientes existentes
     */
    fun getOrCreateCustomer(clientId: String, email: String? = null, name: String? = null): Customer {
        try {
            // Buscar cliente existente por metadata
            val existingCustomers = Customer.list(
                com.stripe.param.CustomerListParams.builder()
                    .setLimit(1)
                    .addExpand("data")
                    .build()
            )
            
            // Buscar por metadata clientId
            val existing = existingCustomers.data.firstOrNull { 
                it.metadata["poolpro_client_id"] == clientId 
            }
            
            if (existing != null) {
                logger.debug("Cliente Stripe existente encontrado: {}", existing.id)
                return existing
            }
            
            // Crear nuevo cliente
            val customerParams = CustomerCreateParams.builder()
                .putMetadata("poolpro_client_id", clientId)
                .apply {
                    email?.let { setEmail(it) }
                    name?.let { setName(it) }
                }
                .build()
            
            val customer = Customer.create(customerParams)
            logger.info("Cliente Stripe creado: {} para clientId: {}", customer.id, clientId)
            return customer
            
        } catch (e: StripeException) {
            logger.error("Error al crear/obtener cliente Stripe: {}", e.message, e)
            throw StripeServiceException("Error al crear cliente en Stripe: ${e.message}", e)
        }
    }
    
    /**
     * Crea un SetupIntent para que el cliente pueda agregar un método de pago
     */
    fun createSetupIntent(customerId: String): SetupIntent {
        try {
            val params = SetupIntentCreateParams.builder()
                .setCustomer(customerId)
                .addPaymentMethodType("card")
                .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION) // Para uso futuro (cobros recurrentes)
                .build()
            
            val setupIntent = SetupIntent.create(params as SetupIntentCreateParams)
            logger.info("SetupIntent creado: {} para customer: {}", setupIntent.id, customerId)
            return setupIntent
            
        } catch (e: StripeException) {
            logger.error("Error al crear SetupIntent: {}", e.message, e)
            throw StripeServiceException("Error al crear SetupIntent: ${e.message}", e)
        }
    }
    
    /**
     * Crea una clave efímera para el cliente
     * Necesaria para que la app Android pueda usar el SetupIntent de forma segura
     */
    fun createEphemeralKey(customerId: String, apiVersion: String = "2023-10-16"): EphemeralKey {
        try {
            val params = EphemeralKeyCreateParams.builder()
                .setCustomer(customerId)
                .setStripeVersion(apiVersion)
                .build()
            
            val ephemeralKey = EphemeralKey.create(params)
            logger.debug("EphemeralKey creada para customer: {}", customerId)
            return ephemeralKey
            
        } catch (e: StripeException) {
            logger.error("Error al crear EphemeralKey: {}", e.message, e)
            throw StripeServiceException("Error al crear EphemeralKey: ${e.message}", e)
        }
    }
    
    /**
     * Crea un PaymentMethod desde los datos de la tarjeta
     * Nota: En producción, esto debería hacerse en el cliente (Android) usando Stripe SDK
     * Este método es para casos donde necesitamos crear el payment method en el backend
     */
    fun createPaymentMethod(
        cardNumber: String,
        expMonth: Int,
        expYear: Int,
        cvc: String? = null
    ): PaymentMethod {
        try {
            // Usar Map para crear PaymentMethod (método alternativo compatible)
            val cardParams = mutableMapOf<String, Any>(
                "number" to cardNumber,
                "exp_month" to expMonth,
                "exp_year" to expYear
            )
            cvc?.let { cardParams["cvc"] = it }
            
            val params = mapOf(
                "type" to "card",
                "card" to cardParams
            )
            
            val paymentMethod = PaymentMethod.create(params)
            logger.info("PaymentMethod creado: {}", paymentMethod.id)
            return paymentMethod
            
        } catch (e: StripeException) {
            logger.error("Error al crear PaymentMethod: {}", e.message, e)
            throw StripeServiceException("Error al crear PaymentMethod: ${e.message}", e)
        }
    }
    
    /**
     * Adjunta un PaymentMethod a un Customer
     */
    fun attachPaymentMethodToCustomer(paymentMethodId: String, customerId: String): PaymentMethod {
        try {
            val params = PaymentMethodAttachParams.builder()
                .setCustomer(customerId)
                .build()
            
            val paymentMethod = PaymentMethod.retrieve(paymentMethodId)
            paymentMethod.attach(params)
            
            logger.info("PaymentMethod {} adjuntado a customer {}", paymentMethodId, customerId)
            return paymentMethod
            
        } catch (e: StripeException) {
            logger.error("Error al adjuntar PaymentMethod: {}", e.message, e)
            throw StripeServiceException("Error al adjuntar PaymentMethod: ${e.message}", e)
        }
    }
    
    /**
     * Obtiene un PaymentMethod por su ID
     */
    fun getPaymentMethod(paymentMethodId: String): PaymentMethod {
        try {
            return PaymentMethod.retrieve(paymentMethodId)
        } catch (e: StripeException) {
            logger.error("Error al obtener PaymentMethod: {}", e.message, e)
            throw StripeServiceException("Error al obtener PaymentMethod: ${e.message}", e)
        }
    }
    
    /**
     * Obtiene el publishable key de Stripe
     * Este método ya no es necesario ya que el publishable key se inyecta en el controller
     * Se mantiene por compatibilidad
     */
    fun getPublishableKey(): String? {
        return System.getenv("STRIPE_PUBLISHABLE_KEY")
            ?: System.getProperty("stripe.publishable.key")
    }
}

/**
 * Excepción personalizada para errores de Stripe
 */
class StripeServiceException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

