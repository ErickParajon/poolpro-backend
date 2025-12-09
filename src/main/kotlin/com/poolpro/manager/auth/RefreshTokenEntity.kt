package com.poolpro.manager.auth

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("refresh_tokens")
data class RefreshTokenEntity(
    @Id
    @Column("token")
    val token: String,
    @Column("owner_id")
    val ownerId: String,
    @Column("issued_at")
    val issuedAt: Instant,
    @Column("expires_at")
    val expiresAt: Instant?,
    @Transient
    private val newRecord: Boolean = false
) : Persistable<String> {

    override fun getId(): String = token

    override fun isNew(): Boolean = newRecord

    fun markPersisted(): RefreshTokenEntity = copy(newRecord = false)
}


