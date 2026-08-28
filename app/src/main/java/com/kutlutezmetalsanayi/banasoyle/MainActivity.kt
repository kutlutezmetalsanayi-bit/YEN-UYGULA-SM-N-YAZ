package com.kutlutezmetalsanayi.banasoyle

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import org.json.JSONArray
import org.json.JSONObject
import java.time.format.DateTimeFormatter
import java.util.Locale

private object ReminderStore {
    private const val PREFS = "bana_soyle_reminders"
    private const val KEY = "items"

    fun load(context: Context): List<Reminder> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { o ->
                Reminder(
                    o.optString("title", "Hatırlatma"),
                    o.optString("spokenText", ""),
                    o.optLong("triggerAtMillis", 0L),
                    o.optLong("reminderAtMillis", 0L)
                )
            }
        }.filter { it.triggerAtMillis > System.currentTimeMillis() }.sortedBy { it.triggerAtMillis }
    }

    fun remove(context: Context, reminder: Reminder) {
        val list = load(context).filterNot { it.triggerAtMillis == reminder.triggerAtMillis && it.title == reminder.title }
        save(context, list)
    }

    private fun save(context: Context, list: List<Reminder>) {
        val arr = JSONArray()
        list.sortedBy { it.triggerAtMillis }.forEach {
            arr.put(JSONObject().apply {
                put("title", it.title); put("spokenText", it.spokenText)
                put("triggerAtMillis", it.triggerAtMillis); put("reminderAtMillis", it.reminderAtMillis)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    fun add(context: Context, reminder: Reminder) {
        val list = load(context).toMutableList()
        list.removeAll { it.triggerAtMillis == reminder.triggerAtMillis && it.title == reminder.title }
        list.add(reminder)
        val arr = JSONArray()
        list.sortedBy { it.triggerAtMillis }.forEach {
            arr.put(JSONObject().apply {
                put("title", it.title)
                put("spokenText", it.spokenText)
                put("triggerAtMillis", it.triggerAtMillis)
                put("reminderAtMillis", it.reminderAtMillis)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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

    fun setSpeechResultListener(listener: ((String) -> Unit)?) {
        speechResultListener = listener
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
        speechResultListener?.invoke("__LISTENING__")
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
            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Mikrofon ses akışında sorun oluştu."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni verilmedi."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Konuşma tanıma servisine ulaşılamadı. İnternet bağlantısını kontrol edin."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Söylediğinizi anlayamadım. Biraz daha net tekrar deneyin."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Ses tanıma meşgul. Tekrar deneyin."
                    SpeechRecognizer.ERROR_SERVER -> "Ses tanıma servisi cevap vermedi."
                    else -> "Ses tanıma başlatılamadı (kod: $error)."
                }
                speechResultListener?.invoke("")
                runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show() }
            }
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
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
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
    var reminders by remember { mutableStateOf(ReminderStore.load(context)) }
    var errorText by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val mainActivity = context as? MainActivity

    DisposableEffect(mainActivity) {
        mainActivity?.setSpeechResultListener { spoken ->
            if (spoken == "__LISTENING__") {
                isListening = true
                errorText = ""
                return@setSpeechResultListener
            }
            isListening = false
            transcript = spoken
            if (spoken.isBlank()) {
                errorText = "Seni anlayamadım. Tekrar deneyelim."
                parsedReminder = null
            } else {
                val reminder = ReminderParser.parse(spoken)
                if (reminder == null) {
                    errorText = "Tarih ve saat bilgisini anlayamadım."
                    parsedReminder = null
                } else {
                    errorText = ""
                    parsedReminder = reminder
                    if (ReminderScheduler.schedule(mainActivity, reminder)) {
                        ReminderStore.add(mainActivity, reminder)
                        reminders = ReminderStore.load(mainActivity ?: return@setSpeechResultListener)
                    }
                }
            }
        }
        onDispose {
            mainActivity?.setSpeechResultListener(null)
            onDestroyRecognizer()
        }
    }

    val bg = androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(Color(0xFF17133F), Color(0xFF29205C), Color(0xFF17133F))
    )

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = "Menü", tint = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bana Söyle", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text("Aklına geldiğinde söyle, unutma.", color = Color(0xFFBDB7D5), fontSize = 13.sp)
                }
                IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Ayarlar", tint = Color.White)
                }
            }

            Spacer(Modifier.weight(0.12f))

            VoiceHero(isListening, onMicClick)

            Spacer(Modifier.weight(0.18f))

            if (transcript.isNotBlank()) {
                TranscriptCard(transcript)
                Spacer(Modifier.height(10.dp))
            }

            parsedReminder?.let {
                ReminderCreatedCard(it)
                Spacer(Modifier.height(10.dp))
            }

            if (errorText.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FF6B6B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(errorText, Modifier.padding(14.dp), color = Color(0xFFFFD0D0), fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
            }

            if (transcript.isBlank() && parsedReminder == null && errorText.isBlank()) {
                Text(
                    "Örnek: “Yarın saat 11'de dişçim var.”",
                    color = Color(0xFFBDB7D5),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(18.dp))

            Card(
                Modifier.fillMaxWidth().clickable { },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(46.dp).background(Color(0x22FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Yaklaşan Hatırlatmalar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (reminders.isEmpty()) "Henüz hatırlatma yok." else "${reminders.size} yaklaşan hatırlatma", color = Color(0xFFBDB7D5), fontSize = 13.sp)
                    }
                    Text("›", color = Color.White, fontSize = 30.sp)
                }
            }

            if (reminders.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reminders) { reminder ->
                        ReminderListItem(reminder, onDelete = {
                            ReminderStore.remove(mainActivity ?: return@items, reminder)
                            reminders = ReminderStore.load(mainActivity)
                        })
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ReminderListItem(reminder: Reminder, onDelete: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("d MMMM • HH:mm", Locale("tr", "TR"))
    val event = java.time.Instant.ofEpochMilli(reminder.triggerAtMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x18FFFFFF))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFD8CFFF), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(reminder.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(event.format(formatter), color = Color(0xFFBDB7D5), fontSize = 12.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFFFB7B7))
            }
        }
    }
}

@Composable
private fun VoiceHero(isListening: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(310.dp)
                .background(Color(0x332D1D73), CircleShape)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF6C3BFF), CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Konuş",
                        tint = Color.White,
                        modifier = Modifier.size(58.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (isListening) "Dinliyorum..." else "Bas, Konuş",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (isListening) "Bitirdiğinde otomatik kaydedilir" else "Bırak, kaydedilsin",
                        color = Color(0xFFE2DCFF),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptCard(text: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))) {
        Column(Modifier.padding(16.dp)) {
            Text("Söylediğin", color = Color(0xFFD8CFFF), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("“$text”", color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ReminderCreatedCard(reminder: Reminder) {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy • HH:mm", Locale("tr", "TR"))
    val event = java.time.Instant.ofEpochMilli(reminder.triggerAtMillis)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))) {
        Column(Modifier.padding(16.dp)) {
            Text("✓ Hatırlatma oluşturuldu", color = Color(0xFF9FF0BC), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(reminder.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(event.format(formatter), color = Color(0xFFBDB7D5), fontSize = 13.sp)
        }
    }
}
