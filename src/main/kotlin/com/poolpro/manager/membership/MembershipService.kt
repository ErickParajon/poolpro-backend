package com.poolpro.manager.membership

import com.poolpro.manager.notification.EmailService
import com.poolpro.manager.notification.SmsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MembershipService(
    private val repository: MembershipRepository,
    private val emailService: EmailService? = null,
    private val smsService: SmsService? = null
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    suspend fun listMemberships(operatorId: String): List<MembershipDto> {
        logger.debug("Listando membresías para operador: {}", operatorId)
        val entities = repository.findByOperatorId(operatorId)
        return entities.map { it.toDto() }
    }
    
    suspend fun getMembership(clientId: String, operatorId: String): MembershipDto? {
        logger.debug("Obteniendo membresía para cliente: {} del operador: {}", clientId, operatorId)
        val entity = repository.findByClientIdAndOperatorId(clientId, operatorId)
        return entity?.toDto()
    }
    
    suspend fun createOrUpdateMembership(
        clientId: String,
        operatorId: String,
        status: MembershipStatus = MembershipStatus.NOT_CONFIGURED
    ): MembershipDto {
        logger.debug("Creando o actualizando membresía para cliente: {} del operador: {}", clientId, operatorId)
        
        val existing = repository.findByClientIdAndOperatorId(clientId, operatorId)
        
        val entity = if (existing != null) {
            existing.copy(
                status = status,
                updatedAt = OffsetDateTime.now()
            )
        } else {
            MembershipEntity(
                membershipId = null, // La BD generará el UUID automáticamente
                clientId = clientId,
                operatorId = operatorId,
                status = status,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
                newRecord = true
            )
        }
        
        val saved = if (entity.isNew()) {
            // Para nuevas entidades, generar UUID antes de guardar
            val entityWithId = if (entity.membershipId == null) {
                entity.copy(membershipId = UUID.randomUUID())
            } else {
                entity
            }
            // NO llamar markPersisted() antes de guardar - Spring Data necesita saber que es nueva
            repository.save(entityWithId)
        } else {
            repository.save(entity.markPersisted())
        }
        
        return saved.toDto()
    }
    
    suspend fun updateMembershipPlan(
        clientId: String,
        operatorId: String,
        amount: Double,
        currency: String,
        billingDay: Int,
        channel: String,
        message: String?
    ): MembershipDto {
        logger.info("Actualizando plan de membresía para cliente: {} del operador: {}", clientId, operatorId)
        logger.debug("Datos del plan: amount={}, currency={}, billingDay={}, channel={}", 
            amount, currency, billingDay, channel)
        
        return try {
            val existing = repository.findByClientIdAndOperatorId(clientId, operatorId)
            logger.debug("Membresía existente: {}", if (existing != null) "encontrada" else "no encontrada, creando nueva")
            
            val entity = if (existing != null) {
                existing.copy(
                    planAmount = BigDecimal.valueOf(amount),
                    planCurrency = currency,
                    planBillingDay = billingDay,
                    planChannel = channel,
                    planMessage = message,
                    status = MembershipStatus.PLAN_DRAFT,
                    updatedAt = OffsetDateTime.now()
                )
            } else {
                // Crear nueva membresía si no existe
                logger.info("Creando nueva membresía para cliente: {}", clientId)
                // No establecer membershipId - la BD lo generará automáticamente con DEFAULT gen_random_uuid()
                MembershipEntity(
                    membershipId = null, // La BD generará el UUID automáticamente
                    clientId = clientId,
                    operatorId = operatorId,
                    status = MembershipStatus.PLAN_DRAFT,
                    planAmount = BigDecimal.valueOf(amount),
                    planCurrency = currency,
                    planBillingDay = billingDay,
                    planChannel = channel,
                    planMessage = message,
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now(),
                    newRecord = true
                )
            }
            
            val saved = try {
                if (entity.isNew()) {
                    logger.debug("Guardando nueva membresía (INSERT usando SQL personalizado)")
                    logger.debug("Datos a insertar: clientId={}, operatorId={}, status={}, amount={}, billingDay={}, channel={}", 
                        entity.clientId, entity.operatorId, entity.status, entity.planAmount, entity.planBillingDay, entity.planChannel)
                    
                    // Usar INSERT SQL personalizado para evitar problemas con Persistable y UUID
                    val result = try {
                        repository.insertNewMembership(
                            clientId = entity.clientId,
                            operatorId = entity.operatorId,
                            status = entity.status,
                            planAmount = entity.planAmount,
                            planCurrency = entity.planCurrency,
                            planBillingDay = entity.planBillingDay,
                            planChannel = entity.planChannel,
                            planMessage = entity.planMessage
                        )
                    } catch (insertError: Exception) {
                        logger.error("Error en insertNewMembership: {}", insertError.message, insertError)
                        throw insertError
                    }
                    
                    logger.debug("Entidad guardada usando INSERT personalizado, UUID resultante: {}", result.membershipId)
                    if (result.membershipId == null) {
                        logger.error("ERROR CRÍTICO: La membresía insertada no tiene UUID!")
                        throw IllegalStateException("La membresía insertada no tiene UUID asignado")
                    }
                    result
                } else {
                    logger.debug("Actualizando membresía existente (UPDATE) con UUID: {}", entity.membershipId)
                    repository.save(entity.markPersisted())
                }
            } catch (e: Exception) {
                logger.error("Error al guardar membresía en la base de datos: {}", e.message, e)
                logger.error("Stack trace completo:", e)
                logger.error("Entity details: clientId={}, operatorId={}, status={}, membershipId={}, isNew={}", 
                    entity.clientId, entity.operatorId, entity.status, entity.membershipId, entity.isNew())
                logger.error("Exception class: {}", e.javaClass.name)
                if (e.cause != null) {
                    logger.error("Caused by: {}", e.cause?.message)
                    logger.error("Cause stack trace:", e.cause)
                }
                throw e
            }
            
            logger.info("Plan guardado exitosamente para cliente: {}", clientId)
            try {
                saved.toDto()
            } catch (e: Exception) {
                logger.error("Error al convertir membresía a DTO: {}", e.message, e)
                throw e
            }
        } catch (e: Exception) {
            logger.error("Error al actualizar plan de membresía para cliente {}: {}", clientId, e.message, e)
            logger.error("Stack trace completo:", e)
            throw e
        }
    }
    
    suspend fun sendMembershipPlan(
        clientId: String,
        operatorId: String,
        channel: String?,
        clientEmail: String? = null,
        clientPhone: String? = null,
        clientName: String? = null
    ): MembershipDto {
        logger.debug("Enviando plan de membresía para cliente: {} via canal: {}", clientId, channel)
        
        val existing = repository.findByClientIdAndOperatorId(clientId, operatorId)
            ?: throw IllegalArgumentException("Membership not found for client: $clientId")
        
        if (existing.status != MembershipStatus.PLAN_DRAFT && existing.status != MembershipStatus.AWAITING_PAYMENT) {
            logger.warn("Intentando enviar plan para membresía con estado inválido: {}", existing.status)
        }
        
        // Validar que hay plan para enviar
        if (existing.planAmount == null || existing.planBillingDay == null || existing.planChannel == null) {
            throw IllegalStateException("El plan de membresía no está completo. Por favor guarda el plan primero.")
        }
        
        // Enviar notificación según el canal
        val notificationChannel = channel ?: existing.planChannel
        val notificationSent = when (notificationChannel.lowercase()) {
            "email" -> {
                if (clientEmail != null && clientName != null) {
                    emailService?.sendMembershipPlan(
                        toEmail = clientEmail,
                        clientName = clientName,
                        planAmount = existing.planAmount.toDouble(),
                        planCurrency = existing.planCurrency ?: "USD",
                        billingDay = existing.planBillingDay,
                        message = existing.planMessage
                    ) ?: false
                } else {
                    logger.warn("No se puede enviar email: email o nombre del cliente no proporcionado")
                    false
                }
            }
            "sms" -> {
                if (clientPhone != null && clientName != null) {
                    smsService?.sendMembershipPlan(
                        toPhone = clientPhone,
                        clientName = clientName,
                        planAmount = existing.planAmount.toDouble(),
                        planCurrency = existing.planCurrency ?: "USD",
                        billingDay = existing.planBillingDay,
                        message = existing.planMessage
                    ) ?: false
                } else {
                    logger.warn("No se puede enviar SMS: teléfono o nombre del cliente no proporcionado")
                    false
                }
            }
            else -> {
                logger.warn("Canal de notificación no reconocido: {}", notificationChannel)
                false
            }
        }
        
        if (notificationSent) {
            logger.info("Notificación enviada exitosamente via {}", notificationChannel)
        } else {
            logger.warn("No se pudo enviar la notificación via {} (servicio no configurado o datos faltantes)", notificationChannel)
        }
        
        // Si se especifica un canal, actualizarlo
        val updatedEntity = if (channel != null && channel != existing.planChannel) {
            existing.copy(
                planChannel = channel,
                lastSentAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )
        } else {
            existing.copy(
                lastSentAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )
        }
        
        val saved = repository.save(updatedEntity.markPersisted())
        
        logger.info("Plan de membresía enviado para cliente: {} (notificación: {})", clientId, if (notificationSent) "enviada" else "no enviada")
        return saved.toDto()
    }
    
    suspend fun cancelMembership(
        clientId: String,
        operatorId: String,
        reason: String?
    ): MembershipDto {
        logger.debug("Cancelando membresía para cliente: {} con razón: {}", clientId, reason)
        
        val existing = repository.findByClientIdAndOperatorId(clientId, operatorId)
            ?: throw IllegalArgumentException("Membership not found for client: $clientId")
        
        if (existing.status == MembershipStatus.CANCELLED) {
            logger.warn("Membership ya está cancelada para cliente: {}", clientId)
            return existing.toDto()
        }
        
        val updatedEntity = existing.copy(
            status = MembershipStatus.CANCELLED,
            nextChargeAt = null, // Limpiar próxima cobranza
            updatedAt = OffsetDateTime.now()
        )
        
        val saved = repository.save(updatedEntity.markPersisted())
        
        logger.info("Membresía cancelada para cliente: {}", clientId)
        return saved.toDto()
    }
    
    suspend fun reactivateMembership(
        clientId: String,
        operatorId: String
    ): MembershipDto {
        logger.debug("Reactivando membresía para cliente: {}", clientId)
        
        val existing = repository.findByClientIdAndOperatorId(clientId, operatorId)
            ?: throw IllegalArgumentException("Membership not found for client: $clientId")
        
        if (existing.status == MembershipStatus.ACTIVE) {
            logger.warn("Membership ya está activa para cliente: {}", clientId)
            return existing.toDto()
        }
        
        // Verificar que tenga plan y método de pago
        if (existing.planAmount == null || existing.planBillingDay == null) {
            throw IllegalStateException("Cannot reactivate membership without a plan. Please configure a plan first.")
        }
        
        if (existing.paymentMethodBrand == null || existing.paymentMethodLast4 == null) {
            throw IllegalStateException("Cannot reactivate membership without a payment method. Please attach a payment method first.")
        }
        
        // Calcular próxima fecha de cobro basada en billingDay
        val nextCharge = calculateNextChargeDate(
            billingDay = existing.planBillingDay,
            fromDate = OffsetDateTime.now()
        )
        
        val updatedEntity = existing.copy(
            status = MembershipStatus.ACTIVE,
            nextChargeAt = nextCharge,
            updatedAt = OffsetDateTime.now()
        )
        
        val saved = repository.save(updatedEntity.markPersisted())
        
        logger.info("Membresía reactivada para cliente: {} con próxima cobranza: {}", clientId, nextCharge)
        return saved.toDto()
    }
    
    suspend fun attachPaymentMethod(
        clientId: String,
        operatorId: String,
        brand: String,
        last4: String,
        expMonth: Int,
        expYear: Int,
        holderName: String?,
        stripePaymentMethodId: String? = null
    ): MembershipDto {
        logger.debug("Adjuntando método de pago para cliente: {}", clientId)
        
        val existing = repository.findByClientIdAndOperatorId(clientId, operatorId)
            ?: throw IllegalArgumentException("Membership not found for client: $clientId")
        
        val updatedEntity = existing.copy(
            paymentMethodBrand = brand,
            paymentMethodLast4 = last4,
            paymentMethodExpMonth = expMonth,
            paymentMethodExpYear = expYear,
            paymentMethodHolderName = holderName,
            paymentMethodStripePaymentMethodId = stripePaymentMethodId,
            // Si estaba en AWAITING_PAYMENT y ahora tiene método de pago y plan, activar
            status = if (existing.status == MembershipStatus.AWAITING_PAYMENT 
                && existing.planAmount != null && existing.planBillingDay != null) {
                MembershipStatus.ACTIVE
            } else {
                existing.status
            },
            // Si se activó, calcular próxima cobranza
            nextChargeAt = if (existing.status == MembershipStatus.AWAITING_PAYMENT 
                && existing.planAmount != null && existing.planBillingDay != null) {
                calculateNextChargeDate(
                    billingDay = existing.planBillingDay,
                    fromDate = OffsetDateTime.now()
                )
            } else {
                existing.nextChargeAt
            },
            updatedAt = OffsetDateTime.now()
        )
        
        val saved = repository.save(updatedEntity.markPersisted())
        
        logger.info("Método de pago adjuntado para cliente: {}", clientId)
        return saved.toDto()
    }
    
    private fun calculateNextChargeDate(billingDay: Int, fromDate: OffsetDateTime): OffsetDateTime {
        val now = fromDate
        val zoneOffset = now.offset
        
        // Convertir a LocalDate para manipular días del mes
        var localDate = now.toLocalDate()
        
        // Usar el día de facturación, pero limitarlo a 28 para evitar problemas con meses cortos
        val dayToUse = billingDay.coerceIn(1, 28)
        
        // Intentar establecer el día en este mes
        var nextChargeDate = try {
            localDate.withDayOfMonth(dayToUse)
        } catch (e: Exception) {
            // Si el día no existe en este mes (ej: día 31 en febrero), usar el último día del mes
            localDate.withDayOfMonth(localDate.lengthOfMonth())
        }
        
        // Si el día ya pasó este mes, usar el próximo mes
        if (nextChargeDate.isBefore(now.toLocalDate()) || nextChargeDate.isEqual(now.toLocalDate())) {
            nextChargeDate = nextChargeDate.plusMonths(1)
            
            // Verificar que el día existe en el próximo mes
            val maxDayInMonth = nextChargeDate.lengthOfMonth()
            if (dayToUse > maxDayInMonth) {
                nextChargeDate = nextChargeDate.withDayOfMonth(maxDayInMonth)
            } else {
                nextChargeDate = nextChargeDate.withDayOfMonth(dayToUse)
            }
        }
        
        // Convertir de vuelta a OffsetDateTime manteniendo la hora y el offset
        return nextChargeDate.atTime(now.toLocalTime()).atOffset(zoneOffset)
    }
    
    private fun MembershipEntity.toDto(): MembershipDto {
        val plan = if (planAmount != null && planBillingDay != null && planChannel != null) {
            MembershipPlanDto(
                amount = planAmount.toDouble(),
                currency = planCurrency ?: "USD",
                billingDay = planBillingDay,
                channel = planChannel,
                message = planMessage
            )
        } else {
            null
        }
        
        val paymentMethod = if (paymentMethodBrand != null && paymentMethodLast4 != null) {
            PaymentMethodDto(
                brand = paymentMethodBrand,
                last4 = paymentMethodLast4,
                expMonth = paymentMethodExpMonth ?: 0,
                expYear = paymentMethodExpYear ?: 0,
                holderName = paymentMethodHolderName ?: ""
            )
        } else {
            null
        }
        
        return MembershipDto(
            clientId = clientId,
            status = status.name.lowercase(),
            plan = plan,
            paymentMethod = paymentMethod,
            nextChargeAt = nextChargeAt?.toString(),
            lastSentAt = lastSentAt?.toString(),
            updatedAt = updatedAt.toString()
        )
    }
}

