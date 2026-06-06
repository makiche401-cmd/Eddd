package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    fun getAllMessagesFlow(): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Query("SELECT COUNT(*) FROM messages WHERE status = 'SENT' AND createdAt >= :startOfDay")
    suspend fun getTodaySentCount(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM messages WHERE status = 'FAILED' AND createdAt >= :startOfDay")
    suspend fun getTodayFailedCount(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM messages WHERE status = 'INCOMING' AND createdAt >= :startOfDay")
    suspend fun getTodayReceivedCount(startOfDay: Long): Int

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}

@Dao
interface OutboxEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: OutboxEvent): Long

    @Query("SELECT * FROM outbox_events ORDER BY nextRetryAt ASC")
    suspend fun getPendingEvents(): List<OutboxEvent>

    @Delete
    suspend fun deleteEvent(event: OutboxEvent)

    @Update
    suspend fun updateEvent(event: OutboxEvent)
}
