package com.poolpro.manager.notification

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import com.poolpro.manager.notification.config.TwilioProperties

@Service
@EnableConfigurationProperties(TwilioProperties::class)
class SmsService(
    private val twilioProperties: TwilioProperties
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    init {
        if (twilioProperties.accountSid.isNotBlank() && twilioProperties.authToken.isNotBlank()) {
            Twilio.init(twilioProperties.accountSid, twilioProperties.authToken)
            logger.info("Twilio inicializado correctamente")
        } else {
            logger.warn("Twilio no configurado - el envío de SMS estará deshabilitado")
        }
    }
    
    suspend fun sendMembershipPlan(
        toPhone: String,
        clientName: String,
        planAmount: Double,
        planCurrency: String,
        billingDay: Int,
        message: String?
    ): Boolean {
        if (twilioProperties.accountSid.isBlank() || twilioProperties.authToken.isBlank()) {
            logger.warn("Twilio no configurado - simulando envío de SMS a $toPhone")
            return false
        }
        
        return try {
            val currencySymbol = when (planCurrency.uppercase()) {
                "USD" -> "$"
                "EUR" -> "€"
                "MXN" -> "$"
                else -> planCurrency
            }
            
            val smsBody = buildMembershipPlanSmsBody(
                clientName = clientName,
                planAmount = planAmount,
                currencySymbol = currencySymbol,
                billingDay = billingDay,
                message = message
            )
            
            val messagingServiceSid = twilioProperties.messagingServiceSid
            
            if (messagingServiceSid.isBlank()) {
                logger.error("No se puede enviar SMS: Messaging Service SID no configurado")
                return false
            }
            
            // Usar Messaging Service de Twilio (recomendado para producción)
            // La API de Twilio tiene una sobrecarga que acepta MessagingServiceSid como String
            val message = Message.creator(
                PhoneNumber(toPhone),
                messagingServiceSid, // Messaging Service SID (String)
                smsBody
            ).create()
            
            logger.info("SMS enviado exitosamente a $toPhone - SID: ${message.sid}")
            true
        } catch (e: Exception) {
            logger.error("Excepción al enviar SMS a $toPhone", e)
            false
        }
    }
    
    private fun buildMembershipPlanSmsBody(
        clientName: String,
        planAmount: Double,
        currencySymbol: String,
        billingDay: Int,
        message: String?
    ): String {
        val baseMessage = """
            Hola $clientName,
            
            Plan de Membresía:
            Monto: $currencySymbol${String.format("%.2f", planAmount)}/mes
            Facturación: Día $billingDay
            
            ${message?.let { "Mensaje: $it\n" } ?: ""}
            Para completar su suscripción, agregue su método de pago.
            
            PoolPro Manager
        """.trimIndent()
        
        // Twilio tiene un límite de 1600 caracteres, pero es mejor mantenerlo corto para SMS
        return if (baseMessage.length > 160) {
            baseMessage.take(157) + "..."
        } else {
            baseMessage
        }
    }
}

