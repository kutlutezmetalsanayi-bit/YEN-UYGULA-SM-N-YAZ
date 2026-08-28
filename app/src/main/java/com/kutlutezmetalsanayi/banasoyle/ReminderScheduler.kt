package com.kutlutezmetalsanayi.banasoyle

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object ReminderScheduler {
    private const val REQUEST_CODE = "BanaSoyleReminder".hashCode()

    fun schedule(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
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

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.reminderAtMillis,
                pending
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminder.reminderAtMillis,
                pending
            )
        }
    }
}
