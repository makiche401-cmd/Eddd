package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.PrefsManager
import com.example.data.api.*
import com.example.data.database.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class GatewayRepository(
    private val context: Context,
    private val messageDao: MessageDao,
    private val outboxEventDao: OutboxEventDao,
    private val prefsManager: PrefsManager
) {
    private val TAG = "GatewayRepository"
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    val allMessagesFlow: Flow<List<Message>> = messageDao.getAllMessagesFlow()

    // API Base URL changes reactively based on pairing
    val supabaseService: SupabaseService?
        get() {
            val api = prefsManager.apiBase ?: return null
            return try {
                SupabaseClient.createService(api)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating Supabase client", e)
                null
            }
        }

    suspend fun insertMessage(message: Message): Long = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message)
    }

    suspend fun updateMessage(message: Message) = withContext(Dispatchers.IO) {
        messageDao.updateMessage(message)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        messageDao.clearAll()
    }

    private fun getStartOfToday(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    suspend fun getTodaySentCount(): Int = withContext(Dispatchers.IO) {
        messageDao.getTodaySentCount(getStartOfToday())
    }

    suspend fun getTodayFailedCount(): Int = withContext(Dispatchers.IO) {
        messageDao.getTodayFailedCount(getStartOfToday())
    }

    suspend fun getTodayReceivedCount(): Int = withContext(Dispatchers.IO) {
        messageDao.getTodayReceivedCount(getStartOfToday())
    }

    // Connect device directly to Supabase Edge Function
    suspend fun connectDevice(deviceId: String, deviceToken: String, apiBase: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.createService(apiBase)
            val response = service.connectDevice(ConnectRequest(deviceId, deviceToken))
            if (response.isSuccessful) {
                // Successful pairing, save credentials locally
                prefsManager.saveCredentials(apiBase, deviceId, deviceToken)
                true
            } else {
                Log.e(TAG, "Connect API failed with status code ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connect API exception", e)
            false
        }
    }

    // Queue / local outbox synchronization mechanism
    suspend fun enqueueSmsSent(smsId: String, recipient: String, simSlot: Int) {
        val deviceId = prefsManager.deviceId ?: return
        val deviceToken = prefsManager.deviceToken ?: return
        val req = SmsStatusRequest(
            device_id = deviceId,
            device_token = deviceToken,
            sms_id = smsId,
            recipient = recipient,
            status = "SENT",
            sim_slot = simSlot
        )
        val json = moshi.adapter(SmsStatusRequest::class.java).toJson(req)
        enqueueEvent("sms-sent", json)
    }

    suspend fun enqueueSmsFailed(smsId: String, recipient: String, errorReason: String) {
        val deviceId = prefsManager.deviceId ?: return
        val deviceToken = prefsManager.deviceToken ?: return
        val req = SmsStatusRequest(
            device_id = deviceId,
            device_token = deviceToken,
            sms_id = smsId,
            recipient = recipient,
            status = "FAILED",
            error = errorReason
        )
        val json = moshi.adapter(SmsStatusRequest::class.java).toJson(req)
        enqueueEvent("sms-failed", json)
    }

    suspend fun enqueueIncomingSms(sender: String, body: String) {
        val deviceId = prefsManager.deviceId ?: return
        val deviceToken = prefsManager.deviceToken ?: return
        val req = IncomingSmsRequest(
            device_id = deviceId,
            device_token = deviceToken,
            sender = sender,
            body = body
        )
        val json = moshi.adapter(IncomingSmsRequest::class.java).toJson(req)
        enqueueEvent("incoming-sms", json)
    }

    private suspend fun enqueueEvent(endpoint: String, payloadJson: String) = withContext(Dispatchers.IO) {
        val event = OutboxEvent(
            endpoint = endpoint,
            payloadJson = payloadJson,
            attempts = 0,
            nextRetryAt = System.currentTimeMillis()
        )
        outboxEventDao.insertEvent(event)
        
        // Try calling the flush process immediately in a fire-and-forget background style
        try {
            processPendingOutboxEvents()
        } catch (e: Exception) {
            Log.e(TAG, "Failed first-attempt flush", e)
        }
    }

    suspend fun processPendingOutboxEvents() = withContext(Dispatchers.IO) {
        val events = outboxEventDao.getPendingEvents()
        if (events.isEmpty()) return@withContext

        val service = supabaseService ?: return@withContext
        val now = System.currentTimeMillis()

        for (event in events) {
            if (event.nextRetryAt > now) continue

            var success = false
            try {
                when (event.endpoint) {
                    "sms-sent" -> {
                        val req = moshi.adapter(SmsStatusRequest::class.java).fromJson(event.payloadJson)
                        if (req != null) {
                            val resp = service.smsSent(req)
                            success = resp.isSuccessful
                        }
                    }
                    "sms-failed" -> {
                        val req = moshi.adapter(SmsStatusRequest::class.java).fromJson(event.payloadJson)
                        if (req != null) {
                            val resp = service.smsFailed(req)
                            success = resp.isSuccessful
                        }
                    }
                    "incoming-sms" -> {
                        val req = moshi.adapter(IncomingSmsRequest::class.java).fromJson(event.payloadJson)
                        if (req != null) {
                            val resp = service.incomingSms(req)
                            success = resp.isSuccessful
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed syncing outbox event ID: ${event.id}", e)
            }

            if (success) {
                outboxEventDao.deleteEvent(event)
                Log.i(TAG, "Successfully synced outbox event ID: ${event.id}")
            } else {
                val nextAttempts = event.attempts + 1
                val delayMs = when (nextAttempts) {
                    1 -> 1000L
                    2 -> 3000L
                    3 -> 9000L
                    else -> 30000L
                }
                val updatedEvent = event.copy(
                    attempts = nextAttempts,
                    nextRetryAt = System.currentTimeMillis() + delayMs
                )
                outboxEventDao.updateEvent(updatedEvent)
                Log.i(TAG, "Rescheduled failed outbox event ID: ${event.id} to retry in ${delayMs / 1000}s")
            }
        }
    }
}
