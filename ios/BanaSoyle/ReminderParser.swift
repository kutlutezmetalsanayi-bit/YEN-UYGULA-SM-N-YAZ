import Foundation

enum ReminderParser {
    static func parse(_ spokenText: String, now: Date = Date()) -> Reminder? {
        let normalized = normalize(spokenText)
        let calendar = Calendar.current

        let date: Date
        if normalized.contains("yarin") {
            date = calendar.date(byAdding: .day, value: 1, to: now)!
        } else if normalized.contains("bugun") {
            date = now
        } else if normalized.contains("ertesi gun") {
            date = calendar.date(byAdding: .day, value: 2, to: now)!
        } else if let weekday = parseWeekday(normalized, now: now) {
            date = weekday
        } else if let explicit = parseExplicitDate(normalized, now: now) {
            date = explicit
        } else if let relative = parseRelativeDate(normalized, now: now) {
            date = relative
        } else {
            date = now
        }

        guard let time = parseTime(normalized, now: now) else { return nil }
        var components = calendar.dateComponents([.year, .month, .day], from: date)
        components.hour = time.hour
        components.minute = time.minute
        components.second = 0

        guard let event = calendar.date(from: components) else { return nil }
        if event <= now && calendar.isDate(event, inSameDayAs: now) { return nil }

        let reminderMinutes = parseReminderMinutes(normalized)
        let reminderAt = calendar.date(byAdding: .minute, value: -reminderMinutes, to: event) ?? event
        guard reminderAt > now else { return nil }

        return Reminder(
            title: cleanTitle(spokenText),
            triggerAt: event,
            reminderAt: reminderAt,
            spokenText: spokenText
        )
    }

    private static func parseRelativeDate(_ text: String, now: Date) -> Date? {
        let pattern = #"\b(\d+)\s*(gun|hafta)\s*sonra\b"#
        guard let match = firstMatch(pattern, in: text),
              let amount = Int(match[1]) else { return nil }
        return Calendar.current.date(byAdding: match[2] == "hafta" ? .weekOfYear : .day,
                                     value: match[2] == "hafta" ? amount : amount,
                                     to: now)
    }

    private static func parseTime(_ text: String, now: Date) -> (hour: Int, minute: Int)? {
        if let match = firstMatch(#"\b([01]?\d|2[0-3])[:.]([0-5]\d)\b"#, in: text),
           let h = Int(match[1]), let m = Int(match[2]) {
            return (h, m)
        }

        if let match = firstMatch(#"\b(?:saat\s*)?([01]?\d|2[0-3])\s*(?:'?de|'?da|de|da|\s|$)"#, in: text),
           let rawHour = Int(match[1]) {
            var hour = rawHour
            if (text.contains("aksam") || text.contains("gece")) && hour >= 1 && hour <= 11 {
                hour += 12
            }
            return (hour, 0)
        }

        return nil
    }

    private static func parseReminderMinutes(_ text: String) -> Int {
        if let match = firstMatch(#"(\d+)\s*(dakika|dk)\s*once"#, in: text), let value = Int(match[1]) {
            return value
        }
        if let match = firstMatch(#"(\d+)\s*saat\s*once"#, in: text), let value = Int(match[1]) {
            return value * 60
        }
        if text.contains("yarim saat once") { return 30 }
        if text.contains("1 saat once") || text.contains("bir saat once") { return 60 }
        return 0
    }

    private static func parseWeekday(_ text: String, now: Date) -> Date? {
        let names: [String: Int] = [
            "pazartesi": 2, "sali": 3, "carsamba": 4,
            "persembe": 5, "cuma": 6, "cumartesi": 7, "pazar": 1
        ]
        guard let target = names.first(where: { text.contains($0.key) })?.value else { return nil }

        let calendar = Calendar.current
        var date = now
        for _ in 0..<7 {
            if calendar.component(.weekday, from: date) == target {
                if calendar.isDate(date, inSameDayAs: now) && calendar.component(.hour, from: now) >= 12 {
                    return calendar.date(byAdding: .day, value: 7, to: date)
                }
                return date
            }
            date = calendar.date(byAdding: .day, value: 1, to: date)!
        }
        return nil
    }

    private static func parseExplicitDate(_ text: String, now: Date) -> Date? {
        guard let match = firstMatch(#"\b(\d{1,2})[./](\d{1,2})(?:[./](\d{2,4}))?\b"#, in: text),
              let day = Int(match[1]), let month = Int(match[2]) else { return nil }

        let calendar = Calendar.current
        var components = calendar.dateComponents([.year], from: now)
        components.day = day
        components.month = month
        guard var date = calendar.date(from: components) else { return nil }

        if match[3] != nil, let y = Int(match[3]) {
            let year = match[3].count == 2 ? 2000 + y : y
            components.year = year
            date = calendar.date(from: components)!
        } else if date < calendar.startOfDay(for: now) {
            date = calendar.date(byAdding: .year, value: 1, to: date)!
        }
        return date
    }

    private static func cleanTitle(_ original: String) -> String {
        var title = original
        let patterns = [
            #"(?i)yarin|bugun|ertesi gun|saat\s*\d{1,2}([:.]\d{2})?('?de|'?da|de|da)?"#,
            #"(?i)\b\d{1,2}[./]\d{1,2}([./]\d{2,4})?\b"#,
            #"(?i)\b(\d+)\s*(dakika|dk|saat)\s*once\b"#,
            #"(?i)bir saat once|yarim saat once"#,
            #"(?i)\b\d+\s*(gun|hafta)\s*sonra\b"#,
            #"(?i)\b(sabah|ogle|aksam|gece)\b"#
        ]
        for pattern in patterns {
            title = title.replacingOccurrences(of: pattern, with: "", options: .regularExpression)
        }
        title = title.replacingOccurrences(of: #"\s+"#, with: " ", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: " .,!?"))
        return title.isEmpty ? "Hatırlatma" : title.prefix(1).uppercased() + String(title.dropFirst())
    }

    private static func normalize(_ value: String) -> String {
        value.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "tr_TR"))
            .replacingOccurrences(of: "ı", with: "i")
    }

    private static func firstMatch(_ pattern: String, in text: String) -> [String]? {
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return nil }
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        guard let match = regex.firstMatch(in: text, range: range) else { return nil }
        return (0..<match.numberOfRanges).map { i in
            let r = match.range(at: i)
            guard r.location != NSNotFound, let swiftRange = Range(r, in: text) else { return "" }
            return String(text[swiftRange])
        }
    }
}
