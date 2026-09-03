package com.kutlutezmetalsanayi.banasoyle

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReminderStore {
    private const val PREFS = "bana_soyle_reminders"
    private const val KEY = "items"

    fun save(context: Context, reminder: Reminder) {
        val list = load(context).filter { it.reminderAtMillis > System.currentTimeMillis() }.toMutableList()
        list.removeAll { it.reminderAtMillis == reminder.reminderAtMillis && it.title == reminder.title }
        list.add(reminder)
        val array = JSONArray()
        list.sortedBy { it.reminderAtMillis }.forEach {
            array.put(JSONObject().apply {
                put("title", it.title)
                put("triggerAtMillis", it.triggerAtMillis)
                put("reminderAtMillis", it.reminderAtMillis)
                put("spokenText", it.spokenText)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    fun load(context: Context): List<Reminder> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val reminder = Reminder(
                        o.getString("title"),
                        o.getLong("triggerAtMillis"),
                        o.getLong("reminderAtMillis"),
                        o.getString("spokenText")
                    )
                    if (reminder.reminderAtMillis > System.currentTimeMillis()) add(reminder)
                }
            }.sortedBy { it.reminderAtMillis }
        }.getOrDefault(emptyList())
    }
}
