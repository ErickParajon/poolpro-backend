package com.poolpro.manager.membership

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@WritingConverter
class MembershipStatusWritingConverter : Converter<MembershipStatus, String> {
    override fun convert(source: MembershipStatus): String {
        return when (source) {
            MembershipStatus.NOT_CONFIGURED -> "not_configured"
            MembershipStatus.PLAN_DRAFT -> "plan_draft"
            MembershipStatus.AWAITING_PAYMENT -> "awaiting_payment"
            MembershipStatus.ACTIVE -> "active"
            MembershipStatus.CANCELLED -> "cancelled"
        }
    }
}

@ReadingConverter
class MembershipStatusReadingConverter : Converter<String, MembershipStatus> {
    override fun convert(source: String): MembershipStatus {
        return when (source.lowercase()) {
            "not_configured" -> MembershipStatus.NOT_CONFIGURED
            "plan_draft" -> MembershipStatus.PLAN_DRAFT
            "awaiting_payment" -> MembershipStatus.AWAITING_PAYMENT
            "active" -> MembershipStatus.ACTIVE
            "cancelled" -> MembershipStatus.CANCELLED
            else -> {
                // Fallback: intentar con el nombre del enum directamente
                try {
                    MembershipStatus.valueOf(source.uppercase())
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Invalid membership status: $source", e)
                }
            }
        }
    }
}

