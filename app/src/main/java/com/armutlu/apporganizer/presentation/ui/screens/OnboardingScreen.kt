package com.armutlu.apporganizer.presentation.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

// ── İzin tanımları ──────────────────────────────────────────────────────────

private enum class PermissionStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val why: String,
    val buttonLabel: String,
    val isRequired: Boolean = true,
    val isSystemSettings: Boolean = false   // normal dialog mı, Settings mi açar
) {
    WELCOME(
        icon = Icons.Default.Apps,
        title = "App Organizer'a Hoş Geldiniz",
        description = "Telefonunuzdaki uygulamaları kategorilere ayırın ve launcher'da gruplandırın.\n\nBaşlamak için birkaç izin vermeniz gerekiyor.",
        why = "",
        buttonLabel = "Başlayalım",
        isRequired = false
    ),
    QUERY_PACKAGES(
        icon = Icons.Default.ManageSearch,
        title = "Uygulama Listesi İzni",
        description = "Telefonunuzdaki kurulu uygulamaları görmek için bu izin gereklidir.",
        why = "Bu izin olmadan hiçbir uygulama listelenemiyor. Veriler sadece cihazınızda kalır, dışarı gönderilmez.",
        buttonLabel = "İzin Ver",
        isRequired = true
    ),
    NOTIFICATIONS(
        icon = Icons.Default.Notifications,
        title = "Bildirim İzni",
        description = "Organize işlemi tamamlandığında size bildirim göndermek için bu izin kullanılır.",
        why = "Yalnızca organize işlemi bittikten sonra tek bir bildirim gönderilir. Reklam veya spam yoktur.",
        buttonLabel = "İzin Ver",
        isRequired = false
    ),
    ACCESSIBILITY(
        icon = Icons.Default.Accessibility,
        title = "Erişilebilirlik Servisi",
        description = "Launcher'da uygulamaları klasörlere fiziksel olarak taşımak için bu servis gereklidir.",
        why = "Bu servis ekrandaki uygulama ikonlarını bulup 'drag & drop' hareketi yaparak onları klasörlere taşır. Şifre, mesaj veya kişisel veri okumaz.",
        buttonLabel = "Ayarları Aç",
        isRequired = false,
        isSystemSettings = true
    ),
    DONE(
        icon = Icons.Default.CheckCircle,
        title = "Her Şey Hazır!",
        description = "İzinler verildi. Uygulamalarınız şimdi taranıyor.",
        why = "",
        buttonLabel = "Uygulamayı Aç",
        isRequired = false
    )
}

// ── Ana ekran ────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var stepIndex by remember { mutableStateOf(0) }
    val steps = PermissionStep.entries.toList()
    val step = steps[stepIndex]

    // İzin durumlarını izle
    var queryGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.QUERY_ALL_PACKAGES)
                == PermissionChecker.PERMISSION_GRANTED
        )
    }
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PermissionChecker.PERMISSION_GRANTED
            else true
        )
    }
    var a11yGranted by remember {
        mutableStateOf(com.armutlu.apporganizer.service.LauncherAccessibilityService.isRunning)
    }

    // Runtime izin launcher'ları
    val queryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        queryGranted = granted
        stepIndex++
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
        stepIndex++
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // Animasyonlu ikon
            AnimatedContent(
                targetState = stepIndex,
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it / 3 } togetherWith
                    fadeOut() + slideOutHorizontally { -it / 3 }
                },
                label = "icon"
            ) { idx ->
                val s = steps[idx]
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            if (s == PermissionStep.DONE)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        s.icon, null,
                        modifier = Modifier.size(52.dp),
                        tint = if (s == PermissionStep.DONE) Color.White
                               else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Sayfa noktaları
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                steps.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == stepIndex) 20.dp else 7.dp, 7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (i < stepIndex) MaterialTheme.colorScheme.primary
                                else if (i == stepIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Başlık & açıklama
            AnimatedContent(targetState = stepIndex, label = "text") { idx ->
                val s = steps[idx]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        s.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        s.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // "Neden bu izin?" kartı
            if (step.why.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Text(
                            step.why,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Mevcut izin durumu rozeti
            val statusText = when (step) {
                PermissionStep.QUERY_PACKAGES ->
                    if (queryGranted) "✅ İzin verildi" else null
                PermissionStep.NOTIFICATIONS ->
                    if (notifGranted) "✅ İzin verildi" else null
                PermissionStep.ACCESSIBILITY -> {
                    // Accessibility durumunu yeniden kontrol et
                    a11yGranted = com.armutlu.apporganizer.service.LauncherAccessibilityService.isRunning
                    if (a11yGranted) "✅ Servis aktif" else null
                }
                else -> null
            }
            if (statusText != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Ana buton
            Button(
                onClick = {
                    when (step) {
                        PermissionStep.WELCOME -> stepIndex++

                        PermissionStep.QUERY_PACKAGES -> {
                            if (queryGranted) { stepIndex++; return@Button }
                            queryLauncher.launch(Manifest.permission.QUERY_ALL_PACKAGES)
                        }

                        PermissionStep.NOTIFICATIONS -> {
                            if (notifGranted) { stepIndex++; return@Button }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                stepIndex++
                            }
                        }

                        PermissionStep.ACCESSIBILITY -> {
                            // Erişilebilirlik ayarlarını aç
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            // Kullanıcı geri döndüğünde "Devam Et" göster
                        }

                        PermissionStep.DONE -> onFinish()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        step == PermissionStep.QUERY_PACKAGES && queryGranted -> "Devam Et"
                        step == PermissionStep.NOTIFICATIONS && notifGranted -> "Devam Et"
                        step == PermissionStep.ACCESSIBILITY && a11yGranted -> "Devam Et"
                        else -> step.buttonLabel
                    },
                    fontSize = 16.sp
                )
            }

            // Accessibility adımında "Geri Dön" butonu göster
            if (step == PermissionStep.ACCESSIBILITY && !a11yGranted) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { stepIndex++ },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Şimdi Değil, Atla")
                }
            }

            // Zorunlu olmayan adımlarda "Atla" seçeneği
            if (!step.isRequired &&
                step != PermissionStep.WELCOME &&
                step != PermissionStep.DONE &&
                !(step == PermissionStep.ACCESSIBILITY && !a11yGranted)
            ) {
                TextButton(onClick = { stepIndex++ }) {
                    Text("Atla", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
