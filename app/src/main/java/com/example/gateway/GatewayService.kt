package com.example.gateway

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PrefsManager
import com.example.data.api.PollMessage
import com.example.data.database.AppDatabase
import com.example.data.database.Message
import com.example.data.repository.GatewayRepository
import kotlin.coroutines.resume
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

class GatewayService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var repository: GatewayRepository
    private lateinit var prefsManager: PrefsManager
    private var wakeLock: PowerManager.WakeLock? = null

    private var pollJob: Job? = null
    private var heartbeatJob: Job? = null
    private var outboxFlushJob: Job? = null

    private val smsSentAction = "com.example.SMS_SENT_ACTION"
    private val smsDeliveredAction = "com.example.SMS_DELIVERED_ACTION"

    // Multi-SIM SMS managers
    private var activeSimSlots = mutableListOf<SimInfo>()

    companion object {
        private const val TAG = "GatewayService"
        private const val NOTIFICATION_ID = 2026
        private const val CHANNEL_ID = "simgate_service_channel"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        private val _lastHeartbeat = MutableStateFlow("Never")
        val lastHeartbeat: StateFlow<String> = _lastHeartbeat

        private val _onlineStatus = MutableStateFlow("Offline") // "Online", "Offline", "Reconnecting"
        val onlineStatus: StateFlow<String> = _onlineStatus

        private val _sentToday = MutableStateFlow(0)
        val sentToday: StateFlow<Int> = _sentToday

        private val _failedToday = MutableStateFlow(0)
        val failedToday: StateFlow<Int> = _failedToday

        private val _receivedToday = MutableStateFlow(0)
        val receivedToday: StateFlow<Int> = _receivedToday

        private val _batteryLevel = MutableStateFlow(100)
        val batteryLevel: StateFlow<Int> = _batteryLevel

        private val _signalStrength = MutableStateFlow(4) // 0-4
        val signalStrength: StateFlow<Int> = _signalStrength

        private val _currentNetwork = MutableStateFlow("WiFi")
        val currentNetwork: StateFlow<String> = _currentNetwork

        private val _currentSim = MutableStateFlow("Auto")
        val currentSim: StateFlow<String> = _currentSim

        const val ACTION_START = "com.example.START_GATEWAY"
        const val ACTION_STOP = "com.example.STOP_GATEWAY"
    }

    data class SimInfo(val slot: Int, val carrier: String, val number: String, val subId: Int)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "GatewayService Created")
        _isRunning.value = true

        val db = AppDatabase.getDatabase(this)
        prefsManager = PrefsManager(this)
        repository = GatewayRepository(this, db.messageDao(), db.outboxEventDao(), prefsManager)

        createNotificationChannel()
        acquireWakeLock()
        registerSmsOutcomeReceiver()
        updateTodayStats()
        queryTelephonyState()

        // Sync local queued reporting outbox sequentially
        startOutboxSyncJob()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "onStartCommand: action = $action")

        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Default: Start Gateway
        startForeground(NOTIFICATION_ID, buildNotification("Initializing..."))
        prefsManager.isServiceActive = true
        startGatewayPipelines()

        return START_STICKY
    }

    private fun startGatewayPipelines() {
        // Cancel existing loops to prevent overlap
        pollJob?.cancel()
        heartbeatJob?.cancel()

        _onlineStatus.value = if (prefsManager.pauseGateway) "Offline" else "Online"

        // Heartbeat Loop (dynamic based on preference)
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                if (!prefsManager.pauseGateway) {
                    sendHeartbeat()
                }
                val intervalSec = prefsManager.heartbeatInterval.coerceAtLeast(5)
                delay(intervalSec * 1000L)
            }
        }

        // Polling Loop
        pollJob = serviceScope.launch {
            var reconnectDelay = 5000L
            while (isActive) {
                if (prefsManager.pauseGateway) {
                    _onlineStatus.value = "Offline"
                    updateNotification("SimGate Paused")
                    delay(3000L)
                    continue
                }

                if (!isNetworkConnected()) {
                    _onlineStatus.value = "Reconnecting"
                    updateNotification("Offline - Waiting for network...")
                    Log.d(TAG, "No network connection. Polling paused. Waiting ${reconnectDelay / 1000}s...")
                    
                    if (prefsManager.autoReconnect) {
                        delay(reconnectDelay)
                        // Exponential backoff for reconnect checks
                        reconnectDelay = (reconnectDelay * 2).coerceAtMost(30000L)
                    } else {
                        // Wait longer without backoff if auto reconnect is off
                        delay(15000L)
                    }
                    continue
                }

                // Network is fine, reset delay
                reconnectDelay = 5000L

                val isBatteryOk = queryTelemetryStats()
                val basePollSec = prefsManager.pollInterval.coerceAtLeast(3)
                val pollInterval = if (isBatteryOk) basePollSec * 1000L else (basePollSec * 6L).coerceAtLeast(30L) * 1000L

                _onlineStatus.value = "Online"
                doPollAndProcess()

                delay(pollInterval)
            }
        }
    }

    private fun startOutboxSyncJob() {
        outboxFlushJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (isNetworkConnected()) {
                    repository.processPendingOutboxEvents()
                }
                delay(10000L) // Scan pending queue every 10 seconds
            }
        }
    }

    private suspend fun sendHeartbeat() {
        val service = repository.supabaseService ?: return
        val deviceId = prefsManager.deviceId ?: return
        val deviceToken = prefsManager.deviceToken ?: return

        val batteryPct = _batteryLevel.value
        val isCharging = getChargingStatus()
        val networkName = getNetworkType()
        val sigStrength = _signalStrength.value

        try {
            val response = service.heartbeatDevice(
                com.example.data.api.HeartbeatRequest(
                    device_id = deviceId,
                    device_token = deviceToken,
                    battery_level = batteryPct,
                    signal_strength = sigStrength,
                    charging = isCharging,
                    network_type = networkName
                )
            )
            if (response.isSuccessful) {
                val formatter = SimpleDateFormat("HH:mm:ss a", Locale.getDefault())
                _lastHeartbeat.value = formatter.format(Date())
                Log.i(TAG, "Heartbeat connected successfully")
                updateNotification("SimGate Active")
            } else {
                Log.e(TAG, "Heartbeat failed with HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Heartbeat network exception", e)
        }
    }

    private suspend fun doPollAndProcess() {
        val service = repository.supabaseService ?: return
        val deviceId = prefsManager.deviceId ?: return
        val deviceToken = prefsManager.deviceToken ?: return

        try {
            val response = service.pollDevice(com.example.data.api.PollRequest(deviceId, deviceToken))
            if (response.isSuccessful) {
                val pollResponse = response.body()
                val msgs = pollResponse?.messages
                if (!msgs.isNullOrEmpty()) {
                    Log.i(TAG, "Polled ${msgs.size} SMS requests from remote backend")
                    for (smsObj in msgs) {
                        processOutgoingSms(smsObj)
                    }
                }
            } else {
                Log.e(TAG, "Poll failed with HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Poll request exception", e)
        }
    }

    // Process single message with custom dual-SIM lookup and retry policies
    private suspend fun processOutgoingSms(pollMessage: PollMessage) {
        val selectedPref = prefsManager.simPreference // "AUTO", "SIM_1", "SIM_2", "DEFAULT"
        var targetSubId = -1
        var targetSlot = -1

        queryTelephonyState() // Refresh current SIM cards

        when (selectedPref) {
            "SIM_1" -> {
                val sim = activeSimSlots.find { it.slot == 0 }
                if (sim != null) {
                    targetSubId = sim.subId
                    targetSlot = 0
                }
            }
            "SIM_2" -> {
                val sim = activeSimSlots.find { it.slot == 1 }
                if (sim != null) {
                    targetSubId = sim.subId
                    targetSlot = 1
                }
            }
            "DEFAULT" -> {
                // keeps default subscription (-1 maps to default)
                targetSlot = 100 
            }
            else -> { // "AUTO" -> try first available slot
                val sim = activeSimSlots.firstOrNull()
                if (sim != null) {
                    targetSubId = sim.subId
                    targetSlot = sim.slot
                }
            }
        }

        // Store Message Locally as Queued
        val dbMsg = Message(
            recipient = pollMessage.recipient,
            body = pollMessage.body,
            status = "PENDING",
            simSlot = targetSlot,
            createdAt = System.currentTimeMillis()
        )
        val localMsgId = repository.insertMessage(dbMsg)

        // Trigger Send Operation (with retries)
        lifecycleSendSms(localMsgId, pollMessage.id, pollMessage.recipient, pollMessage.body, targetSubId, targetSlot)
    }

    private suspend fun lifecycleSendSms(
        localId: Long,
        backendId: String,
        recipient: String,
        body: String,
        subId: Int,
        slot: Int
    ) {
        val retries = listOf(5000L, 15000L, 45000L, 120000L) // delay timers
        var attemptCount = 0
        var sentOk = false
        var lastErr = "Timeout or Unknown"

        while (attemptCount <= 4 && !sentOk) {
            attemptCount++
            Log.d(TAG, "Sending message $backendId (attempt $attemptCount/4) to $recipient")
            
            // Call system SmsManager
            val outcome = attemptSmsTransmission(recipient, body, subId, backendId)
            if (outcome.success) {
                sentOk = true
                Log.d(TAG, "SmsManager transmission reported success for message $backendId")
            } else {
                lastErr = outcome.error ?: "SmsManager error code: ${outcome.errorCode}"
                Log.e(TAG, "SmsManager reporting failure (attempt $attemptCount/4): $lastErr")
                if (attemptCount < 4) {
                    val waitTime = retries.getOrElse(attemptCount - 1) { 5000L }
                    delay(waitTime)
                }
            }
        }

        val finalMsg = Message(
            id = localId,
            recipient = recipient,
            body = body,
            status = if (sentOk) "SENT" else "FAILED",
            attempts = attemptCount,
            lastError = if (sentOk) null else lastErr,
            simSlot = slot,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repository.updateMessage(finalMsg)
        updateTodayStats()

        if (sentOk) {
            repository.enqueueSmsSent(backendId, recipient, slot)
            if (prefsManager.smsSentNotifications) {
                postTransientNotification("SMS Sent Successfully", "Successfully sent SMS to $recipient")
            }
        } else {
            repository.enqueueSmsFailed(backendId, recipient, lastErr)
            if (prefsManager.smsFailedNotifications) {
                postTransientNotification("SMS Delivery Failed", "Failed to send SMS to $recipient: $lastErr")
            }
        }
    }

    data class SmsTransmissionResult(val success: Boolean, val errorCode: Int = 0, val error: String? = null)

    private suspend fun attemptSmsTransmission(
        recipient: String,
        body: String,
        subId: Int,
        backendId: String
    ): SmsTransmissionResult = suspendCancellableCoroutine { continuation ->

        var outcomeDelivered = false
        val customSentAction = "$smsSentAction.$backendId"

        val sentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (outcomeDelivered) return
                outcomeDelivered = true

                try {
                    context.unregisterReceiver(this)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unregistering receiver", e)
                }

                val code = resultCode
                if (code == Activity.RESULT_OK) {
                    continuation.resume(SmsTransmissionResult(true))
                } else {
                    val errorDesc = getSmsErrorMessage(code)
                    continuation.resume(SmsTransmissionResult(false, errorCode = code, error = errorDesc))
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(sentReceiver, IntentFilter(customSentAction), RECEIVER_EXPORTED)
        } else {
            registerReceiver(sentReceiver, IntentFilter(customSentAction))
        }

        try {
            val smsManager = if (subId != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val subManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    val subInfo = subManager.getActiveSubscriptionInfo(subId)
                    if (subInfo != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getSmsManagerForSubscriptionId(subId)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getDefault()
                    }
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            val sentIntent = PendingIntent.getBroadcast(
                this,
                backendId.hashCode(),
                Intent(customSentAction),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            // Dynamic splitting for exceptionally long text strings
            val parts = smsManager.divideMessage(body)
            if (parts.size > 1) {
                val sentIntents = ArrayList<PendingIntent>()
                for (i in 0 until parts.size) {
                    // Send callback only on the last chunk, or on all of them
                    sentIntents.add(sentIntent)
                }
                smsManager.sendMultipartTextMessage(recipient, null, parts, sentIntents, null)
            } else {
                smsManager.sendTextMessage(recipient, null, body, sentIntent, null)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fatal send text error", e)
            try {
                unregisterReceiver(sentReceiver)
            } catch (ex: Exception) {}
            if (continuation.isActive) {
                continuation.resume(SmsTransmissionResult(false, errorCode = -999, error = e.localizedMessage))
            }
        }
    }

    private fun getSmsErrorMessage(code: Int): String {
        return when (code) {
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Generic failure"
            SmsManager.RESULT_ERROR_NO_SERVICE -> "No service coverage"
            SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU"
            SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio turned off"
            else -> "System error code: $code"
        }
    }

    private fun updateTodayStats() {
        serviceScope.launch(Dispatchers.Main) {
            val sent = repository.getTodaySentCount()
            val failed = repository.getTodayFailedCount()
            val recv = repository.getTodayReceivedCount()

            _sentToday.value = sent
            _failedToday.value = failed
            _receivedToday.value = recv

            updateNotification("Sent: $sent | Failed: $failed")
        }
    }

    // Gathers system telemetry indicators
    private fun queryTelemetryStats(): Boolean {
        // Battery level
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 100
        _batteryLevel.value = pct

        // Return whether battery is satisfactory (> 15%)
        return pct > 15
    }

    private fun getChargingStatus(): Boolean {
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
    }

    private fun getNetworkType(): String {
        val conManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            conManager.getNetworkCapabilities(conManager.activeNetwork)
        } else {
            @Suppress("DEPRECATION")
            conManager.activeNetworkInfo?.run {
                _currentNetwork.value = if (type == ConnectivityManager.TYPE_WIFI) "WiFi" else "Cellular"
                return if (type == ConnectivityManager.TYPE_WIFI) "WiFi" else "Cellular"
            }
            null
        }

        val typeStr = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Unknown"
        }
        _currentNetwork.value = typeStr
        return typeStr
    }

    private fun queryTelephonyState() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            _currentSim.value = "Unknown (No Permission)"
            return
        }

        activeSimSlots.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            try {
                val infoList = subManager.activeSubscriptionInfoList
                if (!infoList.isNullOrEmpty()) {
                    for (info in infoList) {
                        val carrier = info.carrierName?.toString() ?: "Unknown"
                        val number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            @Suppress("DEPRECATION", "MissingPermission")
                            subManager.getPhoneNumber(info.subscriptionId) ?: info.number ?: ""
                        } else {
                            @Suppress("DEPRECATION", "MissingPermission")
                            info.number ?: ""
                        }

                        activeSimSlots.add(
                            SimInfo(
                                slot = info.simSlotIndex,
                                carrier = carrier,
                                number = number,
                                subId = info.subscriptionId
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Telephony error", e)
            }
        }

        // Setup textual indicators for display
        if (activeSimSlots.isEmpty()) {
            _currentSim.value = "No active SIM detected"
        } else {
            val stringBuilder = StringBuilder()
            activeSimSlots.forEachIndexed { i, sim ->
                stringBuilder.append("SIM ${sim.slot + 1}: ${sim.carrier}")
                if (i < activeSimSlots.size - 1) stringBuilder.append(" | ")
            }
            _currentSim.value = stringBuilder.toString()
        }
    }

    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            info != null && info.isConnected
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SimGate::GatewayLock").apply {
            acquire()
        }
        Log.i(TAG, "Partial WakeLock acquired")
    }

    private fun registerSmsOutcomeReceiver() {
        // Register standard actions or let custom dynamically registered continuations handle outcomes
        // Registered already per transaction in continuation to avoid routing conflicts
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SimGate Gateway Services",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the background SimGate SMS daemon connection persistent"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GatewayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val accentColor = ContextCompat.getColor(this, R.color.primary_green)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SimGate Gateway Running")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(accentColor)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_launcher_background, "Open App", openPendingIntent)
            .addAction(R.drawable.ic_launcher_background, "Stop Gateway", stopPendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun postTransientNotification(title: String, body: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val openIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val accentColor = ContextCompat.getColor(this, R.color.primary_green)
            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(accentColor)
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .build()
            manager.notify(System.currentTimeMillis().toInt(), notif)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post transient notification", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "GatewayService Destroyed")
        _isRunning.value = false
        _onlineStatus.value = "Offline"
        prefsManager.isServiceActive = false

        pollJob?.cancel()
        heartbeatJob?.cancel()
        outboxFlushJob?.cancel()
        serviceJob.cancel()

        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }

        super.onDestroy()
    }
}
