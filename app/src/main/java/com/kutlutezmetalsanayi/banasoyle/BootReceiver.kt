package com.kutlutezmetalsanayi.banasoyle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        ReminderStore.load(context).forEach { reminder ->
            ReminderScheduler.schedule(context, reminder)
        }
    }
}
