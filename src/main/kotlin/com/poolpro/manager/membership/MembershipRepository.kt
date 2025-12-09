package com.poolpro.manager.membership

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface MembershipRepository : CoroutineCrudRepository<MembershipEntity, UUID> {
    
    @Query("SELECT * FROM memberships WHERE operator_id = $1")
    fun findByOperatorIdReactive(operatorId: String): Flux<MembershipEntity>
    
    suspend fun findByOperatorId(operatorId: String): List<MembershipEntity> {
        return findByOperatorIdReactive(operatorId).collectList().awaitSingle()
    }
    
    @Query("SELECT * FROM memberships WHERE client_id = $1 AND operator_id = $2 LIMIT 1")
    fun findByClientIdAndOperatorIdReactive(clientId: String, operatorId: String): Mono<MembershipEntity>
    
    suspend fun findByClientIdAndOperatorId(clientId: String, operatorId: String): MembershipEntity? {
        return findByClientIdAndOperatorIdReactive(clientId, operatorId).awaitSingleOrNull()
    }
    
    @Query("SELECT EXISTS(SELECT 1 FROM memberships WHERE client_id = $1 AND operator_id = $2)")
    fun existsByClientIdAndOperatorIdReactive(clientId: String, operatorId: String): Mono<Boolean>
    
    suspend fun existsByClientIdAndOperatorId(clientId: String, operatorId: String): Boolean {
        return existsByClientIdAndOperatorIdReactive(clientId, operatorId).awaitSingle()
    }
    
    @Query("""
        INSERT INTO memberships (
            id, client_id, operator_id, status, 
            plan_amount, plan_currency, plan_billing_day, plan_channel, plan_message,
            created_at, updated_at
        ) VALUES (
            gen_random_uuid(), $1, $2, $3,
            $4, $5, $6, $7, $8,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
    """)
    fun insertNewMembershipReactive(
        clientId: String,
        operatorId: String,
        status: String,
        planAmount: java.math.BigDecimal?,
        planCurrency: String?,
        planBillingDay: Int?,
        planChannel: String?,
        planMessage: String?
    ): Mono<Void>
    
    suspend fun insertNewMembership(
        clientId: String,
        operatorId: String,
        status: MembershipStatus,
        planAmount: java.math.BigDecimal?,
        planCurrency: String?,
        planBillingDay: Int?,
        planChannel: String?,
        planMessage: String?
    ): MembershipEntity {
        // Insertar sin RETURNING para evitar problemas con R2DBC
        insertNewMembershipReactive(
            clientId, operatorId, status.name.lowercase(),
            planAmount, planCurrency, planBillingDay, planChannel, planMessage
        ).awaitSingle()
        
        // Esperar un poco para asegurar que la transacción se complete
        kotlinx.coroutines.delay(100)
        
        // Luego obtener la entidad recién insertada
        // Intentar varias veces en caso de race condition
        var retries = 3
        while (retries > 0) {
            val found = findByClientIdAndOperatorId(clientId, operatorId)
            if (found != null) {
                return found
            }
            retries--
            if (retries > 0) {
                kotlinx.coroutines.delay(200)
            }
        }
        
        throw IllegalStateException("No se pudo recuperar la membresía después de insertarla para clientId=$clientId, operatorId=$operatorId")
    }
}

