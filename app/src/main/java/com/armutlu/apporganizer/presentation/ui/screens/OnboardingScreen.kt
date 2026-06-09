package com.armutlu.apporganizer.presentation.ui.screens

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Home
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
private val TealGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF00897B), Color(0xFF26C6DA))
)

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}

private fun isDefaultLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return info?.activityInfo?.packageName == context.packageName
}

private enum class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val why: String,
    val buttonLabel: String,
    val isRequired: Boolean = true,
    val isSkippable: Boolean = false
) {
    WELCOME(
        icon = Icons.Default.Apps,
        title = "App Organizer'a Hos Geldiniz",
        description = "Uygulamalarinizi otomatik olarak kategorilere ayiran ve ana ekraninizi duzenleyen akilli bir launcher.",
        why = "",
        buttonLabel = "Baslayin",
        isRequired = false
    ),
    QUERY_PACKAGES(
        icon = Icons.Default.ManageSearch,
        title = "Uygulama Listesi Izni",
        description = "Telefonunuzdaki kurulu uygulamalari gorebilmek icin bu izin gereklidir.",
        why = "Bu izin olmadan hicbir uygulama listelenemez. Veriler sadece cihazinizda kalir, disari gonderilmez.",
        buttonLabel = "Izin Ver",
        isRequired = true
    ),
    NOTIFICATIONS(
        icon = Icons.Default.Notifications,
        title = "Bildirim Izni",
        description = "Organize islemi tamamlandiginda size bildirim gondermek icin bu izin kullanilir.",
        why = "Yalnizca organize islemi bittikten sonra tek bir bildirim gonderilir. Reklam veya spam yoktur.",
        buttonLabel = "Izin Ver",
        isRequired = false,
        isSkippable = true
    ),
    ACCESSIBILITY(
        icon = Icons.Default.Accessibility,
        title = "Erisebilirlik Servisi",
        description = "Erisebilirlik servisi arka planda calisabilmek icin gereklidir.",
        why = "Bu servis sifre, mesaj veya kisisel veri okumaz. Yalnizca arka plan islemleri icin kullanilir.",
        buttonLabel = "Ayarlari Ac",
        isRequired = false,
        isSkippable = true
    ),
    SET_LAUNCHER(
        icon = Icons.Default.Home,
        title = "Ana Ekran Uygulamasi Olarak Ayarla",
        description = "Harika, neredeyse hazirsiniz! App Organizer'i ana ekran (launcher) olarak ayarlayin ve tam gucu deneyimleyin.\n\nAyarla butonuna tiklayin, acilan ekranda 'App Organizer'i secin.",
        why = "Bu adim olmadan uygulama sadece yonetim ekrani olarak calisiyor. Launcher olarak ayarlandiginda tum gucunu gosterir.",
        buttonLabel = "Ana Ekran Olarak Ayarla",
        isRequired = true,
        isSkippable = true
    ),
    DONE(
        icon = Icons.Default.CheckCircle,
        title = "Her Sey Hazir!",
        description = "Harika! Uygulamalariniz simdi taranarak kategorilere ayrilacak.",
        why = "",
        buttonLabel = "Basla",
        isRequired = false
    )
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var stepIndex by remember { mutableStateOf(0) }
    val steps = OnboardingStep.entries.toList()
    val step = steps[stepIndex]

    var launcherSet by remember { mutableStateOf(isDefaultLauncher(context)) }
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
                launcherSet = isDefaultLauncher(context)
                // Launcher ayarlandıysa otomatik ilerle
                if (launcherSet && currentStep == OnboardingStep.SET_LAUNCHER) {
                    stepIndex++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Android 10+ RoleManager launcher seçim ekranı
    val roleRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        launcherSet = isDefaultLauncher(context)
        if (launcherSet) stepIndex++
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

            // İkon
            AnimatedContent(
                targetState = stepIndex,
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it / 3 } togetherWith
                    fadeOut() + slideOutHorizontally { -it / 3 }
                },
                label = "icon"
            ) { idx ->
                val s = steps[idx]
                val iconBg = if (s == OnboardingStep.SET_LAUNCHER) TealGradient else null
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = if (s == OnboardingStep.SET_LAUNCHER)
                                    Color(0xFF00897B).copy(alpha = 0.6f)
                                else AccentPurple.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .then(
                                if (iconBg != null)
                                    Modifier.background(iconBg)
                                else
                                    Modifier.background(AccentPurple.copy(alpha = 0.25f))
                            ),
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

            // Adım göstergesi
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

            // Başlık + açıklama
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

            // Neden gerekli kutusu
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
                                .background(
                                    if (currentStep == OnboardingStep.SET_LAUNCHER)
                                        Color(0xFF00897B)
                                    else AccentPurple
                                )
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info, null,
                                tint = if (currentStep == OnboardingStep.SET_LAUNCHER)
                                    Color(0xFF00897B) else AccentPurple,
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

            // Durum göstergesi
            val statusText = when (currentStep) {
                OnboardingStep.SET_LAUNCHER -> if (launcherSet) "Varsayilan launcher olarak ayarlandi" else null
                OnboardingStep.QUERY_PACKAGES -> "Izin verildi"
                OnboardingStep.NOTIFICATIONS -> if (notifGranted) "Izin verildi" else null
                OnboardingStep.ACCESSIBILITY -> if (a11yGranted) "Servis aktif" else null
                else -> null
            }
            if (statusText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (currentStep == OnboardingStep.SET_LAUNCHER)
                                Color(0xFF00897B).copy(alpha = 0.25f)
                            else AccentPurple.copy(alpha = 0.20f)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        "Tamam: $statusText",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Ana buton
            val buttonGradient = if (currentStep == OnboardingStep.SET_LAUNCHER && !launcherSet)
                TealGradient else ButtonGradient

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(buttonGradient)
                    .clickable {
                        when (currentStep) {
                            OnboardingStep.WELCOME -> stepIndex++

                            OnboardingStep.SET_LAUNCHER -> {
                                if (launcherSet) {
                                    stepIndex++
                                    return@clickable
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(RoleManager::class.java)
                                    if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                                        roleRequestLauncher.launch(
                                            roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                                        )
                                    } else {
                                        stepIndex++
                                    }
                                } else {
                                    val intent = Intent(Intent.ACTION_MAIN)
                                        .addCategory(Intent.CATEGORY_HOME)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            }

                            OnboardingStep.QUERY_PACKAGES -> stepIndex++

                            OnboardingStep.NOTIFICATIONS -> {
                                if (notifGranted) { stepIndex++; return@clickable }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    stepIndex++
                                }
                            }

                            OnboardingStep.ACCESSIBILITY -> {
                                if (a11yGranted) {
                                    stepIndex++
                                } else {
                                    context.startActivity(
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                    )
                                }
                            }

                            OnboardingStep.DONE -> {
                                context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                    .edit().putBoolean("onboarding_complete", true).apply()
                                onFinish()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        currentStep == OnboardingStep.SET_LAUNCHER && launcherSet -> "Devam Et"
                        currentStep == OnboardingStep.NOTIFICATIONS && notifGranted -> "Devam Et"
                        currentStep == OnboardingStep.ACCESSIBILITY && a11yGranted -> "Devam Et"
                        else -> currentStep.buttonLabel
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Accessibility için özel "Atla" butonu
            if (currentStep == OnboardingStep.ACCESSIBILITY && !a11yGranted) {
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
                        "Simdi Degil, Atla",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                }
            }

            // Launcher adımı için atla butonu
            if (currentStep == OnboardingStep.SET_LAUNCHER && !launcherSet) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clickable { stepIndex++ }
                        .padding(vertical = 12.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Simdi Degil",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.50f)
                    )
                }
            }

            // Genel atla butonu (isteğe bağlı adımlar)
            if (currentStep.isSkippable &&
                currentStep != OnboardingStep.SET_LAUNCHER &&
                currentStep != OnboardingStep.ACCESSIBILITY
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
