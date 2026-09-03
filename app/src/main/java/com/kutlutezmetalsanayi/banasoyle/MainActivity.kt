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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Purple = Color(0xFF6C3BFF)
private val LightPurple = Color(0xFFF2EEFF)
private val TextDark = Color(0xFF1E1B24)

class MainActivity : ComponentActivity() {
    private var recognizer: SpeechRecognizer? = null
    private var speechResultListener: ((String) -> Unit)? = null
    private var audioLevelListener: ((Float) -> Unit)? = null

    fun setSpeechResultListener(listener: ((String) -> Unit)?) {
        speechResultListener = listener
    }

    fun setAudioLevelListener(listener: ((Float) -> Unit)?) {
        audioLevelListener = listener
    }

    private var disclosurePending = false

    private val audioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        disclosurePending = false
        if (granted) startListening()
        else Toast.makeText(this, "Mikrofon izni gerekli.", Toast.LENGTH_SHORT).show()
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
            if (disclosurePending) return
            disclosurePending = true
            android.app.AlertDialog.Builder(this)
                .setTitle("Mikrofon izni")
                .setMessage("Bana Söyle, söylediğiniz hatırlatmayı anlayabilmek için yalnızca siz mikrofon düğmesine bastığınızda sesinizi kullanır. Ses kaydı arka planda yapılmaz.")
                .setPositiveButton("Mikrofonu kullan") { _, _ ->
                    audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
                .setNegativeButton("Vazgeç") { _, _ ->
                    disclosurePending = false
                }
                .setOnCancelListener { disclosurePending = false }
                .show()
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
            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                audioLevelListener?.invoke(normalized)
            }
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

    override fun onResume() {
        super.onResume()
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
    var audioLevel by remember { mutableStateOf(0f) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var reminders by remember { mutableStateOf(ReminderStore.load(context)) }
    val mainActivity = context as? MainActivity

    DisposableEffect(mainActivity) {
        mainActivity?.setSpeechResultListener { spoken ->
            isListening = false
            audioLevel = 0f
            transcript = spoken
            if (spoken.isBlank()) {
                errorText = "Sesini anlayamadım. Tekrar deneyelim."
                parsedReminder = null
            } else {
                val reminder = ReminderParser.parse(spoken)
                if (reminder == null) {
                    errorText = "Tarih ve saat bilgisini net anlayamadım."
                    parsedReminder = null
                } else if (ReminderScheduler.schedule(mainActivity, reminder)) {
                    errorText = ""
                    ReminderStore.save(mainActivity, reminder)
                    reminders = ReminderStore.load(mainActivity)
                    parsedReminder = reminder
                } else {
                    errorText = "Hatırlatma izni gerekli."
                    parsedReminder = null
                }
            }
        }
        mainActivity?.setAudioLevelListener { level -> audioLevel = level }
        onDispose {
            mainActivity?.setSpeechResultListener(null)
            mainActivity?.setAudioLevelListener(null)
            onDestroyRecognizer()
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF17133F))) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Bana Söyle", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Aklına geldiğinde söyle, unutma.", color = Color(0xFFBDB7D5), fontSize = 13.sp)

            if (reminders.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                UpcomingRemindersCard(reminders)
            }

            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                VoiceHero(
                    isListening = isListening,
                    audioLevel = audioLevel,
                    onClick = {
                        if (!isListening) {
                            isListening = true
                            transcript = ""
                            parsedReminder = null
                            errorText = ""
                            onMicClick()
                        }
                    }
                )
            }

            if (transcript.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                TranscriptCard(transcript)
            }
            parsedReminder?.let {
                Spacer(Modifier.height(8.dp))
                ReminderCreatedCard(it)
            }
            if (errorText.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FF6B6B))
                ) {
                    Text(errorText, Modifier.padding(12.dp), color = Color(0xFFFFD0D0), fontSize = 13.sp)
                }
            }
            if (transcript.isBlank() && parsedReminder == null && errorText.isBlank() && reminders.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Örnek: “Yarın saat 11'de dişçim var.”", color = Color(0xFFBDB7D5), fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun UpcomingRemindersCard(reminders: List<Reminder>) {
    val formatter = DateTimeFormatter.ofPattern("d MMM • HH:mm", Locale("tr", "TR"))
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Yaklaşan hatırlatmalar", color = Purple, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            reminders.take(3).forEach { reminder ->
                val time = java.time.Instant.ofEpochMilli(reminder.reminderAtMillis)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, contentDescription = null, tint = Purple, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(reminder.title, Modifier.weight(1f), color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(time.format(formatter), color = Color(0xFF726D79), fontSize = 12.sp)
                }
            }
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
}@Composable
private fun VoiceHero(isListening: Boolean, audioLevel: Float, onClick: () -> Unit) {
    val animatedLevel by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isListening) audioLevel else 0f,
        animationSpec = androidx.compose.animation.core.tween(90),
        label = "voiceLevel"
    )
    val pulse = if (isListening) 1f + animatedLevel * 0.22f else 1f

    Box(
        Modifier
            .size(270.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(238.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .background(
                    if (isListening) Color(0xFFFFE082) else LightPurple,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(210.dp)
                    .graphicsLayer {
                        scaleX = 1f + animatedLevel * 0.10f
                        scaleY = 1f + animatedLevel * 0.10f
                    }
                    .background(
                        if (isListening) Color(0xFFFFB300) else Purple,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 18.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Mikrofon",
                        tint = Color.White,
                        modifier = Modifier.size(62.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isListening) "Konuşun..." else "Konuşmak için dokun",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingRemindersCard(reminders: List<Reminder>) {
    val formatter = DateTimeFormatter.ofPattern("d MMM • HH:mm", Locale("tr", "TR"))
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Yaklaşan hatırlatmalar", color = Purple, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            reminders.take(3).forEach { reminder ->
                val time = java.time.Instant.ofEpochMilli(reminder.reminderAtMillis)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, contentDescription = null, tint = Purple, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(reminder.title, Modifier.weight(1f), color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(time.format(formatter), color = Color(0xFF726D79), fontSize = 12.sp)
                }
            }
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
