import Foundation
import UserNotifications

enum NotificationScheduler {
    static func requestPermission() async -> Bool {
        (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    static func schedule(_ reminder: Reminder) async {
        let content = UNMutableNotificationContent()
        content.title = "Bana Söyle"
        content.body = reminder.title
        content.sound = .default

        let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: reminder.reminderAt)
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        let request = UNNotificationRequest(identifier: reminder.id.uuidString, content: content, trigger: trigger)

        try? await UNUserNotificationCenter.current().add(request)
    }
}
