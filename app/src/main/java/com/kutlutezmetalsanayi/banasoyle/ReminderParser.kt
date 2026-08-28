package com.kutlutezmetalsanayi.banasoyle

import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.regex.Pattern

object ReminderParser {
    fun parse(spokenText: String, now: LocalDateTime = LocalDateTime.now()): Reminder? {
        val normalized = normalize(spokenText)
        val date = when {
            normalized.contains("yarin") -> now.toLocalDate().plusDays(1)
            normalized.contains("bugun") -> now.toLocalDate()
            normalized.contains("ertesi gun") -> now.toLocalDate().plusDays(2)
            else -> parseWeekday(normalized, now) ?: parseExplicitDate(normalized, now) ?: parseRelativeDate(normalized, now) ?: now.toLocalDate()
        }

        val time = parseTime(normalized) ?: return null
        val event = LocalDateTime.of(date, time)
        if (event.isBefore(now) && date == now.toLocalDate()) return null

        val reminderMinutes = parseReminderMinutes(normalized) ?: 0L
        val reminderAt = event.minusMinutes(reminderMinutes)

        val title = cleanTitle(spokenText, normalized)
        return Reminder(
            title = title,
            triggerAtMillis = event.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            reminderAtMillis = reminderAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            spokenText = spokenText
        )
    }

    private fun parseRelativeDate(text: String, now: LocalDateTime): LocalDate? {
        val m = Pattern.compile("\\b(\\d+)\\s*(gun|hafta)\\s*(sonra)\\b").matcher(text)
        if (!m.find()) return null
        val amount = m.group(1).toLong()
        return if (m.group(2) == "hafta") now.toLocalDate().plusWeeks(amount) else now.toLocalDate().plusDays(amount)
    }

    private fun parseTime(text: String): LocalTime? {
        val colon = Pattern.compile("\\b([01]?\\d|2[0-3])[:.]([0-5]\\d)\\b").matcher(text)
        if (colon.find()) return LocalTime.of(colon.group(1).toInt(), colon.group(2).toInt())

        val hour = Pattern.compile("\\b(saat\\s*)?([01]?\\d|2[0-3])\\s*(?:'?de|'?da|de|da|\\s|$)").matcher(text)
        if (hour.find()) {\n            var h = hour.group(2).toInt()\n            if (text.contains("aksam") || text.contains("gece")) { if (h in 1..11) h += 12 }\n            return LocalTime.of(h, 0)\n        }

        return null
    }

    private fun parseReminderMinutes(text: String): Long? {
        val before = Pattern.compile("(\\d+)\\s*(dakika|dk)\\s*once").matcher(text)
        if (before.find()) return before.group(1).toLong()
        val hourBefore = Pattern.compile("(\\d+)\\s*(saat)\\s*once").matcher(text)
        if (hourBefore.find()) return hourBefore.group(1).toLong() * 60
        if (text.contains("yarim saat once")) return 30L
        if (text.contains("1 saat once") || text.contains("bir saat once")) return 60L
        return null
    }

    private fun parseWeekday(text: String, now: LocalDateTime): LocalDate? {
        val days = mapOf(
            "pazartesi" to 1, "sali" to 2, "carsamba" to 3,
            "persembe" to 4, "cuma" to 5, "cumartesi" to 6, "pazar" to 7
        )
        val target = days.entries.firstOrNull { text.contains(it.key) }?.value ?: return null
        var date = now.toLocalDate()
        while (date.dayOfWeek.value != target) date = date.plusDays(1)
        if (date == now.toLocalDate() && now.toLocalTime().isAfter(LocalTime.NOON)) date = date.plusWeeks(1)
        return date
    }

    private fun parseExplicitDate(text: String, now: LocalDateTime): LocalDate? {
        val matcher = Pattern.compile("\\b(\\d{1,2})[./](\\d{1,2})(?:[./](\\d{2,4}))?\\b").matcher(text)
        if (!matcher.find()) return null
        val day = matcher.group(1).toInt()
        val month = matcher.group(2).toInt()
        val year = matcher.group(3)?.let { if (it.length == 2) 2000 + it.toInt() else it.toInt() } ?: now.year
        var date = LocalDate.of(year, month, day)
        if (matcher.group(3) == null && date.isBefore(now.toLocalDate())) date = date.plusYears(1)
        return date
    }

    private fun cleanTitle(original: String, normalized: String): String {
        val title = original
            .replace(Regex("(?i)yarin|bugun|ertesi gun|saat\\s*\\d{1,2}([:.]\\d{2})?('de|'da|de|da)?"), "")
            .replace(Regex("(?i)\\b\\d{1,2}[./]\\d{1,2}([./]\\d{2,4})?\\b"), "")
            .replace(Regex("(?i)\\b(\\d+)\\s*(dakika|dk|saat)\\s*once\\b"), "")
            .replace(Regex("(?i)bir saat once|yarim saat once"), "")\n            .replace(Regex("(?i)\\b\\d+\\s*(gun|hafta)\\s*sonra\\b"), "")\n            .replace(Regex("(?i)\\b(sabah|ogle|aksam|gece)\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '.', ',', '!', '?')
        return title.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr", "TR")) else it.toString() }
            .ifBlank { "Hatırlatma" }
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value.lowercase(Locale("tr", "TR")), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .replace("ı", "i")
    }
}
