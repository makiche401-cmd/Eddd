package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Network Request & Response Models
data class ConnectRequest(
    val device_id: String,
    val device_token: String
)

data class HeartbeatRequest(
    val device_id: String,
    val device_token: String,
    val battery_level: Int,
    val signal_strength: Int,
    val charging: Boolean,
    val network_type: String
)

data class PollRequest(
    val device_id: String,
    val device_token: String
)

data class PollMessage(
    val id: String,
    val recipient: String,
    val body: String
)

data class PollResponse(
    val messages: List<PollMessage>? = null
)

data class SmsStatusRequest(
    val device_id: String,
    val device_token: String,
    val sms_id: String,
    val recipient: String,
    val status: String, // "SENT", "FAILED"
    val error: String? = null,
    val sim_slot: Int = -1
)

data class IncomingSmsRequest(
    val device_id: String,
    val device_token: String,
    val sender: String,
    val body: String,
    val received_at: Long = System.currentTimeMillis()
)

interface SupabaseService {
    @POST("device-connect")
    suspend fun connectDevice(@Body request: ConnectRequest): retrofit2.Response<Unit>

    @POST("device-heartbeat")
    suspend fun heartbeatDevice(@Body request: HeartbeatRequest): retrofit2.Response<Unit>

    @POST("device-poll")
    suspend fun pollDevice(@Body request: PollRequest): retrofit2.Response<PollResponse>

    @POST("sms-sent")
    suspend fun smsSent(@Body request: SmsStatusRequest): retrofit2.Response<Unit>

    @POST("sms-failed")
    suspend fun smsFailed(@Body request: SmsStatusRequest): retrofit2.Response<Unit>

    @POST("incoming-sms")
    suspend fun incomingSms(@Body request: IncomingSmsRequest): retrofit2.Response<Unit>
}

object SupabaseClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun createService(baseUrl: String): SupabaseService {
        // Ensure base URL ends with trailing slash
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseService::class.java)
    }
}
