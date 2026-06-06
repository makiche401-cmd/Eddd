package com.example.gateway

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.PrefsManager

class WatchdogReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WatchdogReceiver"
        private const val REQUEST_CODE = 4040

        fun scheduleWatchdog(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WatchdogReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Trigger every 15 minutes
            val interval = 15 * 60 * 1000L
            val triggerAt = SystemClock.elapsedRealtime() + interval

            try {
                alarmManager.cancel(pendingIntent)
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    interval,
                    pendingIntent
                )
                Log.d(TAG, "Watchdog scheduled to pulse repeating every 15 minutes")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling Watchdog repeating alarm", e)
            }
        }

        fun cancelWatchdog(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WatchdogReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarmManager.cancel(pendingIntent)
                Log.d(TAG, "Watchdog alarm canceled")
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling Watchdog alarm", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Watchdog alarm heartbeat triggered")
        val prefs = PrefsManager(context)

        if (prefs.isServiceActive && prefs.isPaired()) {
            if (!GatewayService.isRunning.value) {
                Log.w(TAG, "Watchdog detected GatewayService is not running but should be! Restarting...")
                val serviceIntent = Intent(context, GatewayService::class.java).apply {
                    action = GatewayService.ACTION_START
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Watchdog failed to restart GatewayService", e)
                }
            } else {
                Log.i(TAG, "Watchdog verified: GatewayService is active and running correctly")
            }
        } else {
            Log.d(TAG, "Watchdog ignored: Service is not expected to be active")
        }
    }
}
