package com.armutlu.apporganizer.presentation.ui.screens

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F0C29),
        Color(0xFF302B63),
        Color(0xFF24243E)
    )
)

private val AccentPurple = Color(0xFF6C63FF)
private val AccentPurpleLight = Color(0xFF9C8FFF)
private val ButtonGradient = Brush.horizontalGradient(
    colors = listOf(AccentPurple, AccentPurpleLight)
)

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}

private enum class PermissionStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val why: String,
    val buttonLabel: String,
    val isRequired: Boolean = true,
    val isSystemSettings: Boolean = false
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
        description = "Erişilebilirlik servisi arka planda çalışmak için gereklidir.",
        why = "Bu servis şifre, mesaj veya kişisel veri okumaz. Yalnızca arka plan işlemleri için kullanılır.",
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

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var stepIndex by remember { mutableStateOf(0) }
    val steps = PermissionStep.entries.toList()
    val step = steps[stepIndex]

    var queryGranted by remember { mutableStateOf(true) }
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PermissionChecker.PERMISSION_GRANTED
            else true
        )
    }
    var a11yGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    val currentStep by rememberUpdatedState(step)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                a11yGranted = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
        stepIndex++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            AnimatedContent(
                targetState = stepIndex,
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it / 3 } togetherWith
                    fadeOut() + slideOutHorizontally { -it / 3 }
                },
                label = "icon"
            ) { idx ->
                val s = steps[idx]
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = AccentPurple.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .background(AccentPurple.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            s.icon, null,
                            modifier = Modifier.size(52.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                steps.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == stepIndex) 24.dp else 7.dp, 7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (i == stepIndex) AccentPurple
                                else Color.White.copy(alpha = 0.20f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            AnimatedContent(targetState = stepIndex, label = "text") { idx ->
                val s = steps[idx]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        s.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        s.description,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (currentStep.why.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(AccentPurple)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info, null,
                                tint = AccentPurple,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                currentStep.why,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            val statusText = when (currentStep) {
                PermissionStep.QUERY_PACKAGES -> if (queryGranted) "✅ İzin verildi" else null
                PermissionStep.NOTIFICATIONS -> if (notifGranted) "✅ İzin verildi" else null
                PermissionStep.ACCESSIBILITY -> if (a11yGranted) "✅ Servis aktif" else null
                else -> null
            }
            if (statusText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentPurple.copy(alpha = 0.20f))
                        .padding(12.dp)
                ) {
                    Text(
                        statusText,
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ButtonGradient)
                    .clickable {
                        when (currentStep) {
                            PermissionStep.WELCOME -> stepIndex++

                            PermissionStep.QUERY_PACKAGES -> stepIndex++

                            PermissionStep.NOTIFICATIONS -> {
                                if (notifGranted) { stepIndex++; return@clickable }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    stepIndex++
                                }
                            }

                            PermissionStep.ACCESSIBILITY -> {
                                if (a11yGranted) {
                                    stepIndex++
                                } else {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                }
                            }

                            PermissionStep.DONE -> onFinish()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        currentStep == PermissionStep.QUERY_PACKAGES && queryGranted -> "Devam Et"
                        currentStep == PermissionStep.NOTIFICATIONS && notifGranted -> "Devam Et"
                        currentStep == PermissionStep.ACCESSIBILITY && a11yGranted -> "Devam Et"
                        else -> currentStep.buttonLabel
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (currentStep == PermissionStep.ACCESSIBILITY && !a11yGranted) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { stepIndex++ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Şimdi Değil, Atla",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                }
            }

            if (!currentStep.isRequired &&
                currentStep != PermissionStep.WELCOME &&
                currentStep != PermissionStep.DONE &&
                !(currentStep == PermissionStep.ACCESSIBILITY && !a11yGranted)
            ) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clickable { stepIndex++ }
                        .padding(vertical = 12.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Atla",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.50f)
                    )
                }
            } else {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
