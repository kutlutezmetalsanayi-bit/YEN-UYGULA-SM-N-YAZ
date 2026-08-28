package com.kutlutezmetalsanayi.banasoyle

data class Reminder(
    val title: String,
    val triggerAtMillis: Long,
    val reminderAtMillis: Long,
    val spokenText: String
)
