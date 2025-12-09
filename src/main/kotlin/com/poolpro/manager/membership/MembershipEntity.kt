package com.poolpro.manager.membership

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Table("memberships")
data class MembershipEntity(
    @Id
    @Column("id")
    val membershipId: UUID? = null,
    
    @Column("client_id")
    val clientId: String,
    
    @Column("operator_id")
    val operatorId: String,
    
    @Column("status")
    val status: MembershipStatus,
    
    // Plan de membresía
    @Column("plan_amount")
    val planAmount: BigDecimal? = null,
    
    @Column("plan_currency")
    val planCurrency: String? = null,
    
    @Column("plan_billing_day")
    val planBillingDay: Int? = null,
    
    @Column("plan_channel")
    val planChannel: String? = null,
    
    @Column("plan_message")
    val planMessage: String? = null,
    
    // Método de pago
    @Column("payment_method_brand")
    val paymentMethodBrand: String? = null,
    
    @Column("payment_method_last4")
    val paymentMethodLast4: String? = null,
    
    @Column("payment_method_exp_month")
    val paymentMethodExpMonth: Int? = null,
    
    @Column("payment_method_exp_year")
    val paymentMethodExpYear: Int? = null,
    
    @Column("payment_method_holder_name")
    val paymentMethodHolderName: String? = null,
    
    @Column("payment_method_stripe_payment_method_id")
    val paymentMethodStripePaymentMethodId: String? = null,
    
    // Fechas
    @Column("next_charge_at")
    val nextChargeAt: OffsetDateTime? = null,
    
    @Column("last_sent_at")
    val lastSentAt: OffsetDateTime? = null,
    
    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    
    @Column("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
    
    @Transient
    private val newRecord: Boolean = false
) : Persistable<UUID> {
    
    override fun getId(): UUID {
        // Spring Data R2DBC requiere que getId() retorne un UUID no-null
        // Si membershipId es null, retornamos un UUID temporal
        // IMPORTANTE: Antes de guardar, debemos generar un UUID real en el servicio
        return membershipId ?: UUID(0, 0)
    }
    
    override fun isNew(): Boolean {
        // Si newRecord es true, la entidad es nueva (necesita INSERT)
        // Si membershipId es null, también es nueva
        // Si tenemos UUID pero newRecord es true, aún es nueva (se le asignó UUID pero no se ha guardado)
        return newRecord || membershipId == null
    }
    
    fun markPersisted(): MembershipEntity = copy(newRecord = false)
}

enum class MembershipStatus {
    NOT_CONFIGURED,
    PLAN_DRAFT,
    AWAITING_PAYMENT,
    ACTIVE,
    CANCELLED
}

