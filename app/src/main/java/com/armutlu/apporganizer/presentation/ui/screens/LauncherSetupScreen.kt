package com.armutlu.apporganizer.presentation.ui.screens

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LauncherSetupScreen(
    onFinish: () -> Unit   // hem "ayarla" hem "daha sonra" bu callback'i çağırır
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        visible = true
    }

    // RoleManager ile launcher seçici (API 29+)
    val roleLauncher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Kullanıcı seçim yaptı ya da iptal etti; her durumda devam et
            onFinish()
        }
    } else null

    fun requestLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)
                && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                roleLauncher?.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        // Fallback: Sistem başlatıcı ayarları sayfasını aç
        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        onFinish()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(56.dp))

                // Ana ikon
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = Color.White
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Launcher Hazır! 🚀",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Uygulamalarınız kategorilere ayrıldı.\nArtık AppOrganizer'ı ana ekranınız yapabilirsiniz.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )

                Spacer(Modifier.height(28.dp))

                // Launcher önizleme kartı
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1A1A2E),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "📱 Launcher Önizleme",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        listOf(
                            Triple("👥", "Sosyal Medya", "Instagram, WhatsApp..."),
                            Triple("🎮", "Oyunlar", "PUBG, Clash of Clans..."),
                            Triple("💰", "Finans", "Bankacılık, kripto..."),
                            Triple("📝", "Üretkenlik", "ChatGPT, Office...")
                        ).forEach { (emoji, name, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(emoji, fontSize = 24.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Nasıl etkinleştirilir bilgisi
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Nasıl çalışır?",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "• Aşağıdaki butona basınca sistem başlatıcı seçici açılır\n" +
                            "• Listeden \"AppOrganizer\" seçin\n" +
                            "• Bundan sonra Ana Ekran tuşu AppOrganizer'ı açar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Ana buton
                Button(
                    onClick = { requestLauncher() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Başlatıcı Olarak Ayarla", fontSize = 16.sp)
                }

                Spacer(Modifier.height(12.dp))

                // Daha sonra butonu
                OutlinedButton(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Daha Sonra Ayarlayacağım")
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Ayarlar > Uygulamalar > Varsayılan Uygulamalar > Başlatıcı\nyolundan istediğiniz zaman değiştirebilirsiniz.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
