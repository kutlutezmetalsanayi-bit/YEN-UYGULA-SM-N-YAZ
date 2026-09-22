import Foundation

struct Reminder: Identifiable, Codable, Equatable {
    let id: UUID
    let title: String
    let triggerAt: Date
    let reminderAt: Date
    let spokenText: String

    init(id: UUID = UUID(), title: String, triggerAt: Date, reminderAt: Date, spokenText: String) {
        self.id = id
        self.title = title
        self.triggerAt = triggerAt
        self.reminderAt = reminderAt
        self.spokenText = spokenText
    }
}
