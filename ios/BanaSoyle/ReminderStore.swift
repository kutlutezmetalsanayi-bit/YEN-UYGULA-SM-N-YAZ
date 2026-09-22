import Foundation
import UserNotifications

final class ReminderStore: ObservableObject {
    @Published private(set) var reminders: [Reminder] = []

    private let key = "bana_soyle_reminders_ios"

    init() {
        load()
    }

    func load() {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([Reminder].self, from: data) else {
            reminders = []
            return
        }
        reminders = decoded.filter { $0.reminderAt > Date() }.sorted { $0.reminderAt < $1.reminderAt }
    }

    func save(_ reminder: Reminder) {
        var list = reminders.filter { $0.reminderAt > Date() }
        list.removeAll { $0.title == reminder.title && $0.reminderAt == reminder.reminderAt }
        list.append(reminder)
        reminders = list.sorted { $0.reminderAt < $1.reminderAt }
        if let data = try? JSONEncoder().encode(reminders) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }
}
