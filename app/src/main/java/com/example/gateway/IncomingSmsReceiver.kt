package com.example.gateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.R
import com.example.data.PrefsManager
import com.example.data.database.AppDatabase
import com.example.data.database.Message
import com.example.data.repository.GatewayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IncomingSmsReceiver : BroadcastReceiver() {
    private val TAG = "IncomingSmsReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        Log.i(TAG, "Incoming SMS broadcast received")
        val msgs = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming SMS payload", e)
            return
        }

        if (msgs.isNullOrEmpty()) return

        // Group multipart messages by sender
        val senderMap = HashMap<String, StringBuilder>()
        for (msg in msgs) {
            val sender = msg.originatingAddress ?: "Unknown"
            val body = msg.messageBody ?: ""
            if (!senderMap.containsKey(sender)) {
                senderMap[sender] = StringBuilder()
            }
            senderMap[sender]?.append(body)
        }

        val db = AppDatabase.getDatabase(context)
        val prefs = PrefsManager(context)
        val repo = GatewayRepository(context, db.messageDao(), db.outboxEventDao(), prefs)

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            for ((sender, bodyBuilder) in senderMap) {
                val fullBody = bodyBuilder.toString()
                Log.i(TAG, "Incoming SMS processed from $sender: $fullBody")

                // Save to Local SQLite History DB
                val message = Message(
                    recipient = sender, // For simplicity on incoming items, recipient is the sender
                    body = fullBody,
                    status = "INCOMING",
                    createdAt = System.currentTimeMillis()
                )
                repo.insertMessage(message)

                // Trigger transient notification if enabled
                if (prefs.incomingSmsNotifications) {
                    try {
                        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        val openIntent = Intent(context, com.example.MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val openPendingIntent = android.app.PendingIntent.getActivity(
                            context,
                            System.currentTimeMillis().toInt(),
                            openIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        val accentColor = androidx.core.content.ContextCompat.getColor(context, R.color.primary_green)
                        val channelId = "simgate_service_channel"
                        val notif = androidx.core.app.NotificationCompat.Builder(context, channelId)
                            .setContentTitle("Incoming SMS Received")
                            .setContentText("From $sender: ${fullBody.take(45)}")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setColor(accentColor)
                            .setContentIntent(openPendingIntent)
                            .setAutoCancel(true)
                            .build()
                        manager.notify(System.currentTimeMillis().toInt(), notif)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to post incoming SMS notification", e)
                    }
                }

                // Enqueue backend dispatch
                if (prefs.isPaired()) {
                    repo.enqueueIncomingSms(sender, fullBody)
                }
            }
        }
    }
}
