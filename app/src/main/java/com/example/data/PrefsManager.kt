package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class PrefsManager(private val context: Context) {
    private val TAG = "PrefsManager"

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "simgate_secure_prefs_v1",
                masterKeyAlias,
                context.applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, falling back to standard prefs", e)
            context.getSharedPreferences("simgate_secure_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_API_BASE = "api_base"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_SIM_PREFERENCE = "sim_preference" // "AUTO", "SIM_1", "SIM_2", "DEFAULT"
        private const val KEY_SERVICE_ACTIVE = "service_active"

        private const val KEY_POLL_INTERVAL = "setting_poll_interval"
        private const val KEY_HEARTBEAT_INTERVAL = "setting_heartbeat_interval"
        private const val KEY_AUTO_RECONNECT = "setting_auto_reconnect"
        private const val KEY_START_ON_BOOT = "setting_start_on_boot"
        private const val KEY_PAUSE_GATEWAY = "setting_pause_gateway"
        private const val KEY_NOTIF_PERSISTENT = "setting_notif_persistent"
        private const val KEY_NOTIF_SMS_SENT = "setting_notif_sms_sent"
        private const val KEY_NOTIF_SMS_FAILED = "setting_notif_sms_failed"
        private const val KEY_NOTIF_SMS_RECV = "setting_notif_sms_recv"
    }

    var pollInterval: Int
        get() = prefs.getInt(KEY_POLL_INTERVAL, 5)
        set(value) = prefs.edit().putInt(KEY_POLL_INTERVAL, value).apply()

    var heartbeatInterval: Int
        get() = prefs.getInt(KEY_HEARTBEAT_INTERVAL, 60)
        set(value) = prefs.edit().putInt(KEY_HEARTBEAT_INTERVAL, value).apply()

    var autoReconnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RECONNECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RECONNECT, value).apply()

    var startOnBoot: Boolean
        get() = prefs.getBoolean(KEY_START_ON_BOOT, true)
        set(value) = prefs.edit().putBoolean(KEY_START_ON_BOOT, value).apply()

    var pauseGateway: Boolean
        get() = prefs.getBoolean(KEY_PAUSE_GATEWAY, false)
        set(value) = prefs.edit().putBoolean(KEY_PAUSE_GATEWAY, value).apply()

    var persistentNotification: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_PERSISTENT, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_PERSISTENT, value).apply()

    var smsSentNotifications: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_SMS_SENT, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_SMS_SENT, value).apply()

    var smsFailedNotifications: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_SMS_FAILED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_SMS_FAILED, value).apply()

    var incomingSmsNotifications: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_SMS_RECV, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_SMS_RECV, value).apply()

    var apiBase: String?
        get() = prefs.getString(KEY_API_BASE, null)?.takeIf { it.isNotBlank() } ?: "https://abjwmllylfdbcmhfqwvk.supabase.co/functions/v1"
        set(value) = prefs.edit().putString(KEY_API_BASE, value).apply()

    var deviceId: String?
        get() = prefs.getString(KEY_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var deviceToken: String?
        get() = prefs.getString(KEY_DEVICE_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_TOKEN, value).apply()

    var simPreference: String
        get() = prefs.getString(KEY_SIM_PREFERENCE, "AUTO") ?: "AUTO"
        set(value) = prefs.edit().putString(KEY_SIM_PREFERENCE, value).apply()

    var isServiceActive: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ACTIVE, value).apply()

    fun isPaired(): Boolean {
        return !deviceId.isNullOrBlank() && !deviceToken.isNullOrBlank()
    }

    fun saveCredentials(api: String, id: String, token: String) {
        prefs.edit()
            .putString(KEY_API_BASE, api)
            .putString(KEY_DEVICE_ID, id)
            .putString(KEY_DEVICE_TOKEN, token)
            .apply()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_API_BASE)
            .remove(KEY_DEVICE_ID)
            .remove(KEY_DEVICE_TOKEN)
            .putBoolean(KEY_SERVICE_ACTIVE, false)
            .apply()
    }
}
