package com.armutlu.apporganizer.presentation.ui.screens

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.theme.AppFont
import com.armutlu.apporganizer.presentation.ui.theme.AppTheme
import com.armutlu.apporganizer.presentation.ui.theme.ThemePreferences
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import kotlinx.coroutines.launch

// Yüklü tarayıcıları listele (ACTION_VIEW + http scheme destekleyenler)
private fun installedBrowsers(context: android.content.Context): List<android.content.pm.ResolveInfo> {
    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("http://example.com"))
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
}

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: AppListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // rememberSaveable — rotation/process death'te onboarding ilerlemesi kaybolmasın (D209 fix)
    var stepIndex by rememberSaveable { mutableStateOf(0) }
    val steps = listOf(
        OnboardingStep.WELCOME,
        OnboardingStep.SET_LAUNCHER,
        OnboardingStep.THEME_SELECT,
        OnboardingStep.QUICK_SETTINGS,
        OnboardingStep.BROWSER_SELECT,
        OnboardingStep.DONE,
    )
    val currentStep by rememberUpdatedState(steps[stepIndex])

    // ── State ────────────────────────────────────────────────────────────
    var launcherSet by remember { mutableStateOf(isDefaultLauncherApp(context)) }
    var selectedTheme by rememberSaveable { mutableStateOf(AppTheme.TEAL) }
    var selectedFont by rememberSaveable { mutableStateOf(AppFont.DEFAULT) }
    val scope = rememberCoroutineScope()
    val themePrefs = remember { ThemePreferences(context) }

    // Browser state
    val browsers = remember { installedBrowsers(context) }
    var selectedBrowserPkg by rememberSaveable { mutableStateOf<String?>(null) }
    var browserRoleSet by remember { mutableStateOf(false) }

    // SET_LAUNCHER'da ON_RESUME ve ActivityResult callback'i aynı anda stepIndex++ tetikleyebilir
    // (Activity result sırası garantili değil) — bu bayrak çift artışı engeller (D209 fix).
    var launcherStepAdvanced by rememberSaveable { mutableStateOf(false) }

    // ── Launchers ────────────────────────────────────────────────────────
    val roleRequestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        launcherSet = isDefaultLauncherApp(context)
        if (launcherSet && !launcherStepAdvanced) {
            launcherStepAdvanced = true
            stepIndex++
        }
    }

    val browserRoleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        browserRoleSet = true
        stepIndex++
    }

    // ── Lifecycle observer ───────────────────────────────────────────────
    @Suppress("DEPRECATION")
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                launcherSet = isDefaultLauncherApp(context)
                if (launcherSet && currentStep == OnboardingStep.SET_LAUNCHER && !launcherStepAdvanced) {
                    launcherStepAdvanced = true
                    stepIndex++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = stepIndex > 0) { stepIndex-- }

    // ── UI ───────────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize().background(OnboardingBackgroundGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            OnboardingStepIcon(steps, stepIndex)
            Spacer(Modifier.height(32.dp))
            OnboardingStepDots(steps, stepIndex)
            Spacer(Modifier.height(28.dp))
            OnboardingStepHeader(steps, stepIndex)
            Spacer(Modifier.height(24.dp))
            OnboardingWhyBox(currentStep)
            if (currentStep.whyRes != 0) Spacer(Modifier.height(16.dp))

            // SET_LAUNCHER durum göstergesi
            if (currentStep == OnboardingStep.SET_LAUNCHER && launcherSet) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00897B).copy(0.25f))
                        .padding(12.dp)
                ) {
                    Text(stringResource(R.string.onb_status_launcher_set), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(12.dp))
            }

            // THEME_SELECT
            if (currentStep == OnboardingStep.THEME_SELECT) {
                Spacer(Modifier.height(8.dp))
                OnboardingThemeSelector(
                    selectedTheme = selectedTheme, selectedFont = selectedFont,
                    onThemeChange = { selectedTheme = it }, onFontChange = { selectedFont = it }
                )
            }

            // QUICK_SETTINGS
            if (currentStep == OnboardingStep.QUICK_SETTINGS) {
                Spacer(Modifier.height(8.dp))
                val quickItems = listOf(
                    Triple("Widget Alanı", "Ana ekranda saat/hava durumu widget\'ı göster",
                        AppPrefs.isWidgetAreaEnabled(context)) to { v: Boolean -> AppPrefs.setWidgetAreaEnabled(context, v) },
                    Triple("Uygulama Önerileri", "En çok kullandıklarını ana ekranda göster",
                        AppPrefs.isSuggestionsEnabled(context)) to { v: Boolean -> AppPrefs.setSuggestionsEnabled(context, v) },
                    Triple("Ana Ekran Araması", "Klasörler arasında arama çubuğu",
                        AppPrefs.isHomeSearchEnabled(context)) to { v: Boolean -> AppPrefs.setHomeSearchEnabled(context, v) },
                    Triple("Klasör Blur Efekti", "Frosted glass efekti (performans gerektirir)",
                        AppPrefs.isFolderBlurEnabled(context)) to { v: Boolean -> AppPrefs.setFolderBlurEnabled(context, v) }
                )
                var quickStates by remember {
                    mutableStateOf(quickItems.map { (triple, _) -> triple.third })
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    quickItems.forEachIndexed { idx, (triple, setter) ->
                        val (title, subtitle, _) = triple
                        val enabled = quickStates[idx]
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (enabled) Color(0xFF00897B).copy(0.25f) else Color.White.copy(0.07f))
                                .border(1.dp,
                                    if (enabled) Color(0xFF00897B).copy(0.6f) else Color.White.copy(0.12f),
                                    RoundedCornerShape(14.dp))
                                .clickable {
                                    val next = !enabled
                                    setter(next)
                                    quickStates = quickStates.toMutableList().also { it[idx] = next }
                                }
                                .padding(14.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(subtitle, color = Color.White.copy(0.55f), fontSize = 12.sp)
                                }
                                Text(if (enabled) "Açık" else "Kapalı",
                                    color = if (enabled) Color(0xFF26C6DA) else Color.White.copy(0.35f),
                                    fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // BROWSER_SELECT
            if (currentStep == OnboardingStep.BROWSER_SELECT) {
                Spacer(Modifier.height(8.dp))
                if (browsers.isEmpty()) {
                    Text("Yüklü tarayıcı bulunamadı.", color = Color.White.copy(0.6f), fontSize = 14.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        browsers.forEach { info ->
                            val pkg = info.activityInfo.packageName
                            val label = info.loadLabel(context.packageManager).toString()
                            val isSelected = selectedBrowserPkg == pkg
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) OnboardingAccentPurple.copy(0.25f) else Color.White.copy(0.07f))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) OnboardingAccentPurple else Color.White.copy(0.15f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedBrowserPkg = pkg }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Language, null,
                                        tint = if (isSelected) OnboardingAccentPurpleLight else Color.White.copy(0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(label, color = Color.White, fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                    if (isSelected) {
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.CheckCircle, null,
                                            tint = OnboardingAccentPurpleLight, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Ana buton ────────────────────────────────────────────────
            val buttonGradient = if (currentStep == OnboardingStep.SET_LAUNCHER && !launcherSet)
                OnboardingTealGradient else OnboardingButtonGradient

            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .clip(RoundedCornerShape(16.dp)).background(buttonGradient)
                    .clickable {
                        when (currentStep) {
                            OnboardingStep.WELCOME -> stepIndex++

                            OnboardingStep.SET_LAUNCHER -> {
                                if (launcherSet) {
                                    stepIndex++
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val rm = context.getSystemService(RoleManager::class.java)
                                    if (rm?.isRoleAvailable(RoleManager.ROLE_HOME) == true)
                                        roleRequestLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                                    else stepIndex++
                                } else {
                                    context.startActivity(Intent(Intent.ACTION_MAIN)
                                        .addCategory(Intent.CATEGORY_HOME)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            }

                            OnboardingStep.THEME_SELECT -> {
                                scope.launch { themePrefs.setTheme(selectedTheme); themePrefs.setFont(selectedFont) }
                                stepIndex++
                            }

                            OnboardingStep.QUICK_SETTINGS -> stepIndex++

                            OnboardingStep.BROWSER_SELECT -> {
                                val pkg = selectedBrowserPkg
                                if (pkg != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val rm = context.getSystemService(RoleManager::class.java)
                                    if (rm?.isRoleAvailable(RoleManager.ROLE_BROWSER) == true) {
                                        val intent = rm.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                                        browserRoleLauncher.launch(intent)
                                    } else stepIndex++
                                } else stepIndex++
                            }

                            OnboardingStep.DONE -> {
                                context.getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                                    .edit().putBoolean(AppPrefs.KEY_ONBOARDING_DONE, true).apply()
                                onFinish()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        currentStep == OnboardingStep.SET_LAUNCHER && launcherSet -> stringResource(R.string.onb_continue)
                        currentStep == OnboardingStep.BROWSER_SELECT && browsers.isEmpty() -> stringResource(R.string.onb_continue)
                        else -> stringResource(currentStep.buttonLabelRes)
                    },
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
            }

            // SET_LAUNCHER için "Şimdi Değil"
            if (currentStep == OnboardingStep.SET_LAUNCHER && !launcherSet) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.clickable { stepIndex++ }.padding(vertical = 12.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) { Text(stringResource(R.string.onb_skip_now), fontSize = 14.sp, color = Color.White.copy(0.50f)) }
            }

            // İsteğe bağlı adımlar için "Atla" — BROWSER_SELECT'te tarayıcı yoksa ana buton zaten
            // skip işlevi görüyor (bkz. yukarı), ayrı "Atla" linki göstermek kafa karıştırır.
            val hideRedundantSkipLink = currentStep == OnboardingStep.BROWSER_SELECT && browsers.isEmpty()
            if (currentStep.isSkippable && !hideRedundantSkipLink) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier.clickable { stepIndex++ }.padding(vertical = 12.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) { Text(stringResource(R.string.onb_skip), fontSize = 14.sp, color = Color.White.copy(0.50f)) }
            } else {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
