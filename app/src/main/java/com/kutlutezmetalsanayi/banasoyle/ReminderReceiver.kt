package com.kutlutezmetalsanayi.banasoyle

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "reminders"
        val manager = context.getSystemService(NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Hatırlatmalar", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Bana Söyle hatırlatmaları"
                }
            )
        }

        val title = intent.getStringExtra("title") ?: "Hatırlatma"
        val text = intent.getStringExtra("text") ?: "Hatırlatma zamanı geldi."
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Bana Söyle")
            .setContentText("$title — $text")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}
