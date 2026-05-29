package com.armutlu.apporganizer.presentation.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionLabel: String? = null
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Default.Apps,
        title = "App Organizer'a Hoş Geldiniz",
        description = "Telefonunuzdaki tüm uygulamaları otomatik olarak kategorilere ayırın. Sosyal medya, oyunlar, finans ve daha fazlası.",
    ),
    OnboardingPage(
        icon = Icons.Default.Security,
        title = "Uygulama İzni",
        description = "Telefonunuzdaki kurulu uygulamaları görebilmek için izin gerekiyor. Bu izin sadece liste okuma içindir, hiçbir veri dışarıya gönderilmez.",
        actionLabel = "İzin Ver"
    ),
    OnboardingPage(
        icon = Icons.Default.AutoFixHigh,
        title = "Otomatik Sınıflandırma",
        description = "✨ FAB butonuna basarak tüm uygulamalarınızı otomatik kategorilere ayırın.\n\n🔍 Arama çubuğu akıllı fuzzy arama yapar.\n\n👆 Herhangi bir uygulamaya tıklayarak kategorisini değiştirin.",
    ),
    OnboardingPage(
        icon = Icons.Default.CheckCircle,
        title = "Hazırsınız!",
        description = "Uygulamalarınız yükleniyor. Listeyi görmek için bitir butonuna basın.",
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val page = pages[currentPage]
    val isLast = currentPage == pages.lastIndex

    // İzin launcher (ikinci sayfada)
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> if (currentPage < pages.lastIndex) currentPage++ }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(32.dp))

            // İkon
            AnimatedContent(targetState = currentPage, transitionSpec = {
                fadeIn() + slideInHorizontally { it / 3 } togetherWith
                fadeOut() + slideOutHorizontally { -it / 3 }
            }, label = "page") { idx ->
                Box(
                    modifier = Modifier.size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        pages[idx].icon, null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Metin
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f).padding(vertical = 32.dp)
            ) {
                AnimatedContent(targetState = currentPage, label = "text") { idx ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = pages[idx].title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = pages[idx].description,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sayfa noktaları
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == currentPage) 24.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (i == currentPage) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Buton
                Button(
                    onClick = {
                        when {
                            page.actionLabel != null -> {
                                // İzin sayfası
                                val perms = mutableListOf(Manifest.permission.QUERY_ALL_PACKAGES)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                permLauncher.launch(perms.toTypedArray())
                            }
                            isLast -> onFinish()
                            else  -> currentPage++
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            page.actionLabel != null -> page.actionLabel
                            isLast -> "Başla"
                            else   -> "Devam"
                        },
                        fontSize = 16.sp
                    )
                }

                // Atla (son sayfada gizli)
                if (!isLast && page.actionLabel == null) {
                    TextButton(onClick = onFinish) {
                        Text("Atla", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}
