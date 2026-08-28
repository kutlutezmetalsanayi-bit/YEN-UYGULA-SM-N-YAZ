package com.kutlutezmetalsanayi.banasoyle

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Purple = Color(0xFF6C3BFF)
private val LightPurple = Color(0xFFF2EEFF)
private val TextDark = Color(0xFF1E1B24)

class MainActivity : ComponentActivity() {
    private var recognizer: SpeechRecognizer? = null
    private var speechResultListener: ((String) -> Unit)? = null

    fun setSpeechResultListener(listener: ((String) -> Unit)?) {
        speechResultListener = listener
    }

    private val audioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color(0xFFF8F7FC)) {
                    BanaSoyleApp(
                        onMicClick = { beginVoiceFlow() },
                        onDestroyRecognizer = { recognizer?.destroy() }
                    )
                }
            }
        }
    }

    private fun beginVoiceFlow() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speechResultListener?.invoke("")
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onError(error: Int) { speechResultListener?.invoke("") }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                speechResultListener?.invoke(text)
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel("reminders", "Hatırlatmalar", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Bana Söyle hatırlatmaları" }
        )
    }

    override fun onDestroy() {
        recognizer?.destroy()
        super.onDestroy()
    }
}

@Composable
private fun BanaSoyleApp(
    onMicClick: () -> Unit,
    onDestroyRecognizer: () -> Unit
) {
    var isListening by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var parsedReminder by remember { mutableStateOf<Reminder?>(null) }
    var errorText by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val mainActivity = context as? MainActivity

    DisposableEffect(mainActivity) {
        mainActivity?.setSpeechResultListener { spoken ->
            isListening = false
            transcript = spoken
            if (spoken.isBlank()) {
                errorText = "Sesini anlayamadım. Tekrar deneyelim."
                parsedReminder = null
            } else {
                val reminder = ReminderParser.parse(spoken)
                if (reminder == null) {
                    errorText = "Tarih ve saat bilgisini net anlayamadım. Örneğin: “Yarın saat 11'de dişçim var.”"
                    parsedReminder = null
                } else {
                    errorText = ""
                    parsedReminder = reminder
                    ReminderScheduler.schedule(mainActivity, reminder)
                }
            }
        }
        onDispose {
            mainActivity?.setSpeechResultListener(null)
            onDestroyRecognizer()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F7FC),
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Bana Söyle", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Aklına geldiği anda söyle, unutma.", color = Color(0xFF726D79), fontSize = 14.sp)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, contentDescription = "Ayarlar", tint = Color(0xFF57515D))
                }
            }
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem("Ana Sayfa", Icons.Default.TaskAlt, true)
                NavItem("Hatırlatmalar", Icons.Default.Alarm, false)
                FloatingActionButton(
                    onClick = {
                        isListening = true
                        errorText = ""
                        parsedReminder = null
                        onMicClick()
                    },
                    containerColor = Purple,
                    contentColor = Color.White,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Konuş", modifier = Modifier.size(30.dp))
                }
                NavItem("Takvim", Icons.Default.CalendarToday, false)
                NavItem("Ayarlar", Icons.Default.Settings, false)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item { VoiceHero(isListening, onMicClick) }

            if (transcript.isNotBlank()) item { TranscriptCard(transcript) }
            parsedReminder?.let { item { ReminderCreatedCard(it) } }

            if (errorText.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F1)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(errorText, Modifier.padding(16.dp), color = Color(0xFF9B3333), fontSize = 14.sp)
                    }
                }
            }

            if (parsedReminder == null && transcript.isBlank()) item { ExamplesCard() }

            item { Text("Nasıl çalışır?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark) }
            item { InfoCard("1", "Konuş", "Mikrofona dokun ve aklındaki şeyi doğal şekilde söyle.") }
            item { InfoCard("2", "Anlar", "Uygulama Türkçe konuşmandan tarih, saat ve görevi çıkarır.") }
            item { InfoCard("3", "Hatırlatır", "Hatırlatma zamanı geldiğinde telefonuna bildirim gönderir.") }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun VoiceHero(isListening: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(28.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(142.dp).background(LightPurple, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(104.dp).background(Purple, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, contentDescription = "Mikrofon", tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(if (isListening) "Dinliyorum..." else "Konuşmak için dokun", color = Purple, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (isListening) "Seni dinliyorum, bitirdiğinde otomatik anlayacağım."
                else "“Yarın saat 11'de dişçim var” demen yeterli.",
                color = Color(0xFF726D79), fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TranscriptCard(text: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("Söylediğin", color = Purple, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text("“" + text + "”", color = TextDark, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ReminderCreatedCard(reminder: Reminder) {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy • HH:mm", Locale("tr", "TR"))
    val event = java.time.Instant.ofEpochMilli(reminder.triggerAtMillis)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    val reminderAt = java.time.Instant.ofEpochMilli(reminder.reminderAtMillis)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp)) {
            Text("✓ Hatırlatma oluşturuldu", color = Color(0xFF159447), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Text(reminder.title, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(event.format(formatter), color = Color(0xFF726D79), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("Bildirim: " + reminderAt.format(formatter), color = Purple, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ExamplesCard() {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = LightPurple)) {
        Column(Modifier.padding(18.dp)) {
            Text("Örnekler", color = Purple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("“Yarın saat 11'de dişçim var.”", color = TextDark, fontSize = 14.sp)
            Text("“Pazartesi 9'da Ahmet'e teklif göndereceğim.”", color = TextDark, fontSize = 14.sp)
            Text("“3 Eylül saat 14'te arabayı servise götüreceğim.”", color = TextDark, fontSize = 14.sp)
        }
    }
}

@Composable
private fun InfoCard(number: String, title: String, body: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(LightPurple, CircleShape), contentAlignment = Alignment.Center) {
                Text(number, color = Purple, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = TextDark, fontWeight = FontWeight.Bold)
                Text(body, color = Color(0xFF726D79), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Icon(icon, contentDescription = label, tint = if (selected) Purple else Color(0xFF8A8490), modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (selected) Purple else Color(0xFF8A8490), fontSize = 10.sp)
    }
}
