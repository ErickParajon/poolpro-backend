package com.poolpro.manager.membership

data class UpdateMembershipPlanRequest(
    val amount: Double,
    val currency: String,
    val billingDay: Int,
    val channel: String,
    val message: String? = null
)

data class SendMembershipPlanRequest(
    val channel: String,
    val clientEmail: String? = null,
    val clientPhone: String? = null,
    val clientName: String? = null
)

data class AttachPaymentMethodRequest(
    // Opción 1: PaymentMethod ID de Stripe (recomendado - viene de la app Android)
    val paymentMethodId: String? = null,
    
    // Opción 2: Datos de tarjeta directos (fallback - no recomendado en producción)
    val cardNumber: String? = null,
    val expMonth: Int? = null,
    val expYear: Int? = null,
    val cvv: String? = null,
    val holderName: String? = null
)

data class CancelMembershipRequest(
    val reason: String? = null
)




