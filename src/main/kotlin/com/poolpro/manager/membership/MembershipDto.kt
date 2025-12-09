package com.poolpro.manager.membership

data class MembershipDto(
    val clientId: String,
    val status: String,
    val plan: MembershipPlanDto? = null,
    val paymentMethod: PaymentMethodDto? = null,
    val nextChargeAt: String? = null,
    val lastSentAt: String? = null,
    val updatedAt: String? = null
)

data class MembershipPlanDto(
    val amount: Double,
    val currency: String,
    val billingDay: Int,
    val channel: String,
    val message: String? = null
)

data class PaymentMethodDto(
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val holderName: String
)

data class SetupIntentDto(
    val clientSecret: String,
    val customerId: String,
    val ephemeralKey: String,
    val publishableKey: String? = null,
    val merchantName: String? = null
)


