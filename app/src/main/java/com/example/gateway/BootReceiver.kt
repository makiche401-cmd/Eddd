package com.example.gateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.PrefsManager

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Boot broadcast received: action = $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            val prefs = PrefsManager(context)
            if (prefs.isServiceActive && prefs.isPaired() && prefs.startOnBoot) {
                Log.i(TAG, "Gateway was previously active, auto-restarting GatewayService...")
                val serviceIntent = Intent(context, GatewayService::class.java).apply {
                    this.action = GatewayService.ACTION_START
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                    
                    // Setup the periodic watchdog as a safety mechanism
                    setupWatchdogAlarm(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore GatewayService automatically on boot", e)
                }
            } else {
                Log.i(TAG, "Gateway was not active prior to restart. Skipping auto-activation.")
            }
        }
    }

    private fun setupWatchdogAlarm(context: Context) {
        // Enforce AlarmManager watchdog intervals (redundant helper mapped in WatchdogReceiver)
        WatchdogReceiver.scheduleWatchdog(context)
    }
}
