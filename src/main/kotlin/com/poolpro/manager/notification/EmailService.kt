package com.poolpro.manager.notification

import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import com.poolpro.manager.notification.config.SendGridProperties

@Service
@EnableConfigurationProperties(SendGridProperties::class)
class EmailService(
    private val sendGridProperties: SendGridProperties
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    private val sendGrid: SendGrid? by lazy {
        if (sendGridProperties.apiKey.isNotBlank()) {
            SendGrid(sendGridProperties.apiKey)
        } else {
            logger.warn("SendGrid API key no configurada - el envío de emails estará deshabilitado")
            null
        }
    }
    
    suspend fun sendMembershipPlan(
        toEmail: String,
        clientName: String,
        planAmount: Double,
        planCurrency: String,
        billingDay: Int,
        message: String?
    ): Boolean {
        if (sendGrid == null) {
            logger.warn("SendGrid no configurado - simulando envío de email a $toEmail")
            return false
        }
        
        return try {
            val from = Email("noreply@poolpromanager.com") // TODO: Configurar email del remitente
            val to = Email(toEmail)
            val subject = "Plan de Membresía - PoolPro Manager"
            
            val emailBody = buildMembershipPlanEmailBody(
                clientName = clientName,
                planAmount = planAmount,
                planCurrency = planCurrency,
                billingDay = billingDay,
                message = message
            )
            
            val content = Content("text/html", emailBody)
            val mail = Mail(from, subject, to, content)
            
            val request = Request()
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            
            val response = sendGrid!!.api(request)
            
            if (response.statusCode in 200..299) {
                logger.info("Email enviado exitosamente a $toEmail")
                true
            } else {
                logger.error("Error al enviar email a $toEmail: ${response.statusCode} - ${response.body}")
                false
            }
        } catch (e: Exception) {
            logger.error("Excepción al enviar email a $toEmail", e)
            false
        }
    }
    
    private fun buildMembershipPlanEmailBody(
        clientName: String,
        planAmount: Double,
        planCurrency: String,
        billingDay: Int,
        message: String?
    ): String {
        val currencySymbol = when (planCurrency.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "MXN" -> "$"
            else -> planCurrency
        }
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .plan-details { background-color: white; padding: 15px; margin: 15px 0; border-left: 4px solid #4CAF50; }
                    .amount { font-size: 24px; font-weight: bold; color: #4CAF50; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Plan de Membresía</h1>
                    </div>
                    <div class="content">
                        <p>Estimado/a $clientName,</p>
                        <p>Le enviamos los detalles de su plan de membresía:</p>
                        <div class="plan-details">
                            <p><strong>Monto mensual:</strong> <span class="amount">$currencySymbol${String.format("%.2f", planAmount)}</span></p>
                            <p><strong>Día de facturación:</strong> Día $billingDay de cada mes</p>
                        </div>
                        ${message?.let { "<p><strong>Mensaje adicional:</strong><br>$it</p>" } ?: ""}
                        <p>Para completar su suscripción, por favor agregue su método de pago.</p>
                        <p>Gracias por confiar en nosotros.</p>
                    </div>
                    <div class="footer">
                        <p>PoolPro Manager - Sistema de Gestión de Piscinas</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}


