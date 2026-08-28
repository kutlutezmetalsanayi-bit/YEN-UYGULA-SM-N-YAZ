package com.kutlutezmetalsanayi.banasoyle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Purple = Color(0xFF6C3BFF)
private val LightPurple = Color(0xFFF2EEFF)
private val TextDark = Color(0xFF1E1B24)

private data class Reminder(
    val title: String,
    val date: String,
    val time: String,
    val repeat: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF8F7FC)) {
                    BanaSoyleApp()
                }
            }
        }
    }
}

@Composable
private fun BanaSoyleApp() {
    var isListening by remember { mutableStateOf(false) }
    val reminders = remember {
        listOf(
            Reminder("Dişçi randevusu", "29 Ağustos 2026", "11:00", "30 dk önce"),
            Reminder("Ahmet'e teklif gönder", "31 Ağustos 2026", "09:00", "30 dk önce"),
            Reminder("Arabayı servise götür", "1 Eylül 2026", "14:00", "30 dk önce"),
            Reminder("Bitkileri sula", "Her Pazar", "10:00", "Tekrarlayan")
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8F7FC),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem("Ana Sayfa", Icons.Default.TaskAlt, selected = true)
                NavItem("Hatırlatmalar", Icons.Default.Alarm, selected = false)
                FloatingActionButton(
                    onClick = { isListening = !isListening },
                    containerColor = Purple,
                    contentColor = Color.White,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Konuş", modifier = Modifier.size(30.dp))
                }
                NavItem("Takvim", Icons.Default.CalendarToday, selected = false)
                NavItem("Daha Fazla", Icons.Default.Add, selected = false)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item {
                VoiceHero(
                    isListening = isListening,
                    onClick = { isListening = !isListening }
                )
            }
            item {
                if (isListening) {
                    UnderstandingCard()
                } else {
                    ExamplesCard()
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Yaklaşan Hatırlatmalar", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Tümünü gör", color = Purple, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            items(reminders) { reminder -> ReminderCard(reminder) }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun VoiceHero(isListening: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(142.dp)
                    .background(LightPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(Purple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Mikrofon", tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                if (isListening) "Dinliyorum..." else "Konuşmak için dokun",
                color = Purple,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (isListening) "Söylediklerini anlayıp hatırlatmayı hazırlıyorum."
                else "“Yarın saat 11'de dişçim var” demen yeterli.",
                color = Color(0xFF726D79),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ExamplesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LightPurple)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Örnekler", color = Purple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("“Yarın saat 11'de dişçim var.”", color = TextDark, fontSize = 14.sp)
            Text("“Pazartesi Ahmet'e teklif göndereceğim.”", color = TextDark, fontSize = 14.sp)
            Text("“Her Pazar saat 10'da bitkileri sula.”", color = TextDark, fontSize = 14.sp)
        }
    }
}

@Composable
private fun UnderstandingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Anlıyorum", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Text("Konuşmandan tarih, saat ve yapılacak işi otomatik çıkaracağız.", color = Color(0xFF726D79), fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text("• Görev / randevu", color = Purple, fontWeight = FontWeight.SemiBold)
            Text("• Tarih ve saat", color = Purple, fontWeight = FontWeight.SemiBold)
            Text("• Hatırlatma zamanı", color = Purple, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReminderCard(reminder: Reminder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(LightPurple, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = Purple)
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.title, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${reminder.date} • ${reminder.time}", color = Color(0xFF726D79), fontSize = 13.sp)
            }
            Text(reminder.repeat, color = Purple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
