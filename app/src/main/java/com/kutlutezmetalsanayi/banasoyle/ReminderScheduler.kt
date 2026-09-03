package com.kutlutezmetalsanayi.banasoyle

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object ReminderScheduler {
    fun schedule(context: Context, reminder: Reminder): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false\n        if (reminder.reminderAtMillis <= System.currentTimeMillis()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:" + context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return false
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", reminder.title)
            putExtra("text", reminder.spokenText)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            (reminder.reminderAtMillis xor reminder.triggerAtMillis).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.reminderAtMillis, pending)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.reminderAtMillis, pending)
        }
        return true
    }
}
