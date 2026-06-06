package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PrefsManager
import com.example.data.database.AppDatabase
import com.example.data.database.Message
import com.example.data.repository.GatewayRepository
import com.example.gateway.GatewayService
import com.example.gateway.WatchdogReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class GatewayViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "GatewayViewModel"
    private val context = application.applicationContext

    private val db = AppDatabase.getDatabase(context)
    val prefsManager = PrefsManager(context)
    val repository = GatewayRepository(context, db.messageDao(), db.outboxEventDao(), prefsManager)

    // Binding Live data from Repository
    val messagesFlow: Flow<List<Message>> = repository.allMessagesFlow

    // Service States (Delegated directly from GatewayService metrics)
    val isServiceRunning: StateFlow<Boolean> = GatewayService.isRunning
    val lastHeartbeat: StateFlow<String> = GatewayService.lastHeartbeat
    val onlineStatus: StateFlow<String> = GatewayService.onlineStatus
    val sentToday: StateFlow<Int> = GatewayService.sentToday
    val failedToday: StateFlow<Int> = GatewayService.failedToday
    val receivedToday: StateFlow<Int> = GatewayService.receivedToday
    val batteryLevel: StateFlow<Int> = GatewayService.batteryLevel
    val signalStrength: StateFlow<Int> = GatewayService.signalStrength
    val currentNetwork: StateFlow<String> = GatewayService.currentNetwork
    val currentSimInfo: StateFlow<String> = GatewayService.currentSim

    // Pair States
    private val _isPaired = MutableStateFlow(prefsManager.isPaired())
    val isPaired: StateFlow<Boolean> = _isPaired

    // SIM Selection Preference
    private val _simPreference = MutableStateFlow(prefsManager.simPreference)
    val simPreference: StateFlow<String> = _simPreference

    // UI Loading states
    val isConnecting = mutableStateOf(false)
    val connectError = mutableStateOf<String?>(null)
    val isPairSuccess = mutableStateOf(false)

    init {
        // Schedule alarm watchdog if running
        if (prefsManager.isServiceActive && prefsManager.isPaired()) {
            WatchdogReceiver.scheduleWatchdog(context)
        }
    }

    fun startService() {
        if (!prefsManager.isPaired()) {
            Toast.makeText(context, "Pair device first", Toast.LENGTH_SHORT).show()
            return
        }
        Log.i(TAG, "Starting GatewayService...")
        val intent = Intent(context, GatewayService::class.java).apply {
            action = GatewayService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, intent)
            prefsManager.isServiceActive = true
            WatchdogReceiver.scheduleWatchdog(context)
            Toast.makeText(context, "SimGate Service Activated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed launching service", e)
            Toast.makeText(context, "Error starting service: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopService() {
        Log.i(TAG, "Stopping GatewayService...")
        val intent = Intent(context, GatewayService::class.java).apply {
            action = GatewayService.ACTION_STOP
        }
        context.stopService(intent)
        prefsManager.isServiceActive = false
        WatchdogReceiver.cancelWatchdog(context)
        Toast.makeText(context, "SimGate Service Stopped", Toast.LENGTH_SHORT).show()
    }

    fun setSimPreference(preference: String) {
        prefsManager.simPreference = preference
        _simPreference.value = preference
        Toast.makeText(context, "SIM policy updated to: $preference", Toast.LENGTH_SHORT).show()
    }

    fun pairDevice(deviceId: String, deviceToken: String, apiBase: String, onSuccess: () -> Unit) {
        if (deviceId.isBlank() || deviceToken.isBlank() || apiBase.isBlank()) {
            connectError.value = "All fields are required"
            return
        }

        viewModelScope.launch {
            isConnecting.value = true
            connectError.value = null
            
            // Check for real internet connection
            if (!com.example.gateway.NetworkHelper.hasRealInternetConnection(context)) {
                connectError.value = "No internet connection. Please connect to the internet."
                isConnecting.value = false
                return@launch
            }
            
            // Call connective verify on post /device-connect inside Repository
            val success = repository.connectDevice(deviceId, deviceToken, apiBase)
            isConnecting.value = false
            
            if (success) {
                isPairSuccess.value = true
                onSuccess()
                // Auto-start background GatewayService immediately on success
                startService()
                
                // Allow user to see "Connected" success screen before loading the dashboard automatically
                kotlinx.coroutines.delay(1800)
                _isPaired.value = true
                isPairSuccess.value = false
            } else {
                connectError.value = "Failed to connect to backend Edge Function. Verify credentials."
            }
        }
    }

    fun pairWithQr(qrJsonString: String, onSuccess: () -> Unit) {
        try {
            val jsonObject = JSONObject(qrJsonString)
            val api = jsonObject.getString("api")
            val id = jsonObject.getString("device_id")
            val token = jsonObject.getString("device_token")
            
            pairDevice(id, token, api, onSuccess)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse QR json config", e)
            connectError.value = "Invalid QR Format or missing keys"
        }
    }

    fun unpairDevice() {
        stopService()
        prefsManager.clearCredentials()
        _isPaired.value = false
        Toast.makeText(context, "Device unbound from gateway", Toast.LENGTH_SHORT).show()
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            Toast.makeText(context, "Local message logs cleared", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendTestSms(recipient: String, messageText: String) {
        if (recipient.isBlank() || messageText.isBlank()) {
            Toast.makeText(context, "Recipient details cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val customSmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            try {
                customSmsManager.sendTextMessage(recipient, null, messageText, null, null)
                
                // Track locally as standard Sent
                val testMsg = Message(
                    recipient = recipient,
                    body = "$messageText (TEST)",
                    status = "SENT",
                    simSlot = 99, // slot notation for Test
                    createdAt = System.currentTimeMillis()
                )
                repository.insertMessage(testMsg)
                Toast.makeText(context, "Test SMS payload triggered", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed triggering test SMS", e)
                val testMsg = Message(
                    recipient = recipient,
                    body = "$messageText (TEST)",
                    status = "FAILED",
                    simSlot = 99,
                    lastError = e.localizedMessage,
                    createdAt = System.currentTimeMillis()
                )
                repository.insertMessage(testMsg)
                Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun forceSync() {
        if (!isServiceRunning.value) {
            Toast.makeText(context, "Start Gateway Service first to sync", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(context, GatewayService::class.java).apply {
            action = GatewayService.ACTION_FORCE_SYNC
        }
        try {
            ContextCompat.startForegroundService(context, intent)
            Toast.makeText(context, "Force syncing SMS outbox...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force sync", e)
        }
    }

    fun hasPermissions(ctx: Context): Boolean {
        val permissions = mutableListOf(
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }

        // Check Battery Optimizations
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
                return false
            }
        }

        return true
    }
}
