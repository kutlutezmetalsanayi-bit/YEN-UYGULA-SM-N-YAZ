import SwiftUI

struct ContentView: View {
    @StateObject private var speech = SpeechRecognizer()
    @StateObject private var store = ReminderStore()
    @State private var pulse = false
    @State private var status = "Hazır"

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color.blue.opacity(0.18), Color.white, Color.blue.opacity(0.10)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 22) {
                    Text("Bana Söyle")
                        .font(.largeTitle.bold())
                        .padding(.top, 18)

                    Text("Konuş, ben hatırlatmanı oluşturayım.")
                        .foregroundStyle(.secondary)

                    if !speech.transcript.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Söylediğin")
                                .font(.headline)
                            Text(speech.transcript)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .padding()
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 18))
                    }

                    Button {
                        if speech.isListening {
                            speech.stop()
                            processTranscript()
                        } else {
                            speech.start()
                            status = "Dinliyorum…"
                        }
                    } label: {
                        ZStack {
                            Circle()
                                .fill(speech.isListening ? Color.yellow : Color.blue)
                                .frame(width: 150, height: 150)
                                .scaleEffect(speech.isListening && pulse ? 1.08 : 1.0)
                                .shadow(radius: 14)

                            Image(systemName: speech.isListening ? "stop.fill" : "mic.fill")
                                .font(.system(size: 48, weight: .bold))
                                .foregroundStyle(.white)
                        }
                    }
                    .buttonStyle(.plain)
                    .onAppear {
                        pulse = true
                    }
                    .onChange(of: speech.isListening) { _, listening in
                        if listening {
                            withAnimation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true)) {
                                pulse = true
                            }
                        } else {
                            pulse = false
                        }
                    }

                    Text(status)
                        .font(.headline)
                        .foregroundStyle(speech.isListening ? .orange : .secondary)

                    if let error = speech.errorMessage {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                            .multilineTextAlignment(.center)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Yaklaşan Hatırlatmalar")
                            .font(.title2.bold())

                        if store.reminders.isEmpty {
                            Text("Henüz hatırlatma yok.")
                                .foregroundStyle(.secondary)
                        } else {
                            ForEach(store.reminders.prefix(5)) { reminder in
                                VStack(alignment: .leading, spacing: 5) {
                                    Text(reminder.title)
                                        .font(.headline)
                                    Text(reminder.reminderAt.formatted(date: .abbreviated, time: .shortened))
                                        .foregroundStyle(.secondary)
                                }
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding()
            }
        }
        .task {
            _ = await NotificationScheduler.requestPermission()
        }
    }

    private func processTranscript() {
        guard !speech.transcript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            status = "Konuşma algılanamadı."
            return
        }

        guard let reminder = ReminderParser.parse(speech.transcript) else {
            status = "Tarih ve saat anlaşılmadı."
            return
        }

        store.save(reminder)
        status = "Hatırlatma kaydedildi."
        Task {
            await NotificationScheduler.schedule(reminder)
        }
    }
}
