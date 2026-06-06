package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipient: String,
    val body: String,
    val status: String, // "SENT", "FAILED", "INCOMING", "QUEUED"
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val simSlot: Int = -1 // Sim slot used (0 for SIM 1, 1 for SIM 2, etc.)
)

@Entity(tableName = "outbox_events")
data class OutboxEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val endpoint: String, // "/incoming-sms", "/sms-sent", "/sms-failed"
    val payloadJson: String,
    val attempts: Int = 0,
    val nextRetryAt: Long = System.currentTimeMillis()
)
