package com.armutlu.apporganizer.presentation.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.armutlu.apporganizer.presentation.ui.theme.AppFont
import com.armutlu.apporganizer.presentation.ui.theme.AppTheme
import com.armutlu.apporganizer.presentation.ui.theme.ThemePreferences
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: AppListViewModel = hiltViewModel()
) {
    val context    = LocalContext.current
    var stepIndex  by remember { mutableStateOf(0) }
    val steps = listOf(
        OnboardingStep.WELCOME, OnboardingStep.RESTORE_BACKUP, OnboardingStep.QUERY_PACKAGES,
        OnboardingStep.NOTIFICATIONS, OnboardingStep.UNUSED_GREY, OnboardingStep.AUTO_BACKUP,
        OnboardingStep.NOTIF_TEXT, OnboardingStep.NOTIF_ACCESS, OnboardingStep.SWIPE_HINT,
        OnboardingStep.NEW_BADGE, OnboardingStep.FOLDER_COUNT, OnboardingStep.NAV_HIDE,
        OnboardingStep.THEME_SELECT, OnboardingStep.SET_LAUNCHER, OnboardingStep.CLASSIFY_MODE, OnboardingStep.DONE,
    )
    val currentStep by rememberUpdatedState(steps[stepIndex])

    // ── State ────────────────────────────────────────────────────────────
    var launcherSet       by remember { mutableStateOf(isDefaultLauncherApp(context)) }
    var notifGranted      by remember {
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PermissionChecker.PERMISSION_GRANTED
            else true)
    }
    var notifAccessGranted by remember {
        mutableStateOf(Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.contains(context.packageName) == true)
    }
    var unusedGreyDays    by remember { mutableStateOf(AppPrefs.getUnusedGreyDays(context)) }
    var autoBackupEnabled by remember { mutableStateOf(true) }
    var notifTextEnabled  by remember { mutableStateOf(true) }
    var swipeHintEnabled  by remember { mutableStateOf(true) }
    var newBadgeEnabled   by remember { mutableStateOf(true) }
    var folderCountEnabled by remember { mutableStateOf(true) }
    var navHideEnabled    by remember { mutableStateOf(false) }
    var selectedTheme     by remember { mutableStateOf(AppTheme.TEAL) }
    var selectedFont      by remember { mutableStateOf(AppFont.DEFAULT) }
    var restoreResult     by remember { mutableStateOf<String?>(null) }
    val scope         = rememberCoroutineScope()
    val themePrefs    = remember { ThemePreferences(context) }

    // ── Launchers ────────────────────────────────────────────────────────
    val restoreFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@launch
                val result = viewModel.importBackup(json)
                restoreResult = if (result.success) "${result.updatedCount} uygulama geri yuklendi"
                                else "Geri yukleme basarisiz: ${result.error}"
            }.onFailure { restoreResult = "Dosya okunamadi: ${it.message}" }
        }
    }
    val roleRequestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        launcherSet = isDefaultLauncherApp(context)
        if (launcherSet) stepIndex++
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifGranted = granted; stepIndex++
    }

    // ── Lifecycle observer ───────────────────────────────────────────────
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                launcherSet = isDefaultLauncherApp(context)
                notifAccessGranted = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                    ?.contains(context.packageName) == true
                if (launcherSet && currentStep == OnboardingStep.SET_LAUNCHER) stepIndex++
                if (notifAccessGranted && currentStep == OnboardingStep.NOTIF_ACCESS) stepIndex++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── UI ───────────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize().background(OnboardingBackgroundGradient)) {
        Column(
            modifier = Modifier.fillMaxSize()
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
            if (currentStep.why.isNotBlank()) Spacer(Modifier.height(16.dp))

            OnboardingStatusBadge(currentStep, launcherSet, notifGranted, notifAccessGranted, unusedGreyDays)
            if (currentStep in listOf(OnboardingStep.SET_LAUNCHER, OnboardingStep.QUERY_PACKAGES,
                    OnboardingStep.NOTIFICATIONS, OnboardingStep.NOTIF_ACCESS, OnboardingStep.UNUSED_GREY))
                Spacer(Modifier.height(12.dp))

            // RESTORE_BACKUP sonucu
            if (currentStep == OnboardingStep.RESTORE_BACKUP && restoreResult != null) {
                val result = restoreResult ?: ""
                val isSuccess = result.contains("geri yuklendi")
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (isSuccess) Color(0xFF00897B).copy(0.25f) else Color(0xFFB00020).copy(0.25f))
                        .padding(12.dp)
                ) { Text(result, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
                if (isSuccess) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                            .background(OnboardingTealGradient).clickable { stepIndex++ },
                        contentAlignment = Alignment.Center
                    ) { Text("Devam Et", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Toggle seçici (AUTO_BACKUP, NOTIF_TEXT, SWIPE_HINT vb.)
            val toggleState: Boolean? = when (currentStep) {
                OnboardingStep.AUTO_BACKUP    -> autoBackupEnabled
                OnboardingStep.NOTIF_TEXT     -> notifTextEnabled
                OnboardingStep.SWIPE_HINT     -> swipeHintEnabled
                OnboardingStep.NEW_BADGE      -> newBadgeEnabled
                OnboardingStep.FOLDER_COUNT   -> folderCountEnabled
                OnboardingStep.NAV_HIDE       -> navHideEnabled
                else -> null
            }
            if (toggleState != null) {
                Spacer(Modifier.height(8.dp))
                OnboardingToggleRow(toggleState) { value ->
                    when (currentStep) {
                        OnboardingStep.AUTO_BACKUP    -> autoBackupEnabled  = value
                        OnboardingStep.NOTIF_TEXT     -> notifTextEnabled   = value
                        OnboardingStep.SWIPE_HINT     -> swipeHintEnabled   = value
                        OnboardingStep.NEW_BADGE      -> newBadgeEnabled    = value
                        OnboardingStep.FOLDER_COUNT   -> folderCountEnabled = value
                        OnboardingStep.NAV_HIDE       -> navHideEnabled     = value
                        else -> {}
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Tema / yazı tipi seçici
            if (currentStep == OnboardingStep.THEME_SELECT) {
                Spacer(Modifier.height(8.dp))
                OnboardingThemeSelector(
                    selectedTheme = selectedTheme, selectedFont = selectedFont,
                    onThemeChange = { selectedTheme = it }, onFontChange = { selectedFont = it }
                )
            }

            if (currentStep == OnboardingStep.CLASSIFY_MODE) {
                Spacer(Modifier.height(8.dp))
                val classifyOptions = listOf(
                    "category"     to ("Kategoriye Göre" to "Sosyal Medya, Oyunlar, Finans..."),
                    "manufacturer" to ("Üreticiye Göre"  to "Google, Samsung, Microsoft...")
                )
                var selectedClassify by remember {
                    mutableStateOf(if (com.armutlu.apporganizer.utils.AppPrefs.isManufacturerClassifyEnabled(context)) "manufacturer" else "category")
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    classifyOptions.forEach { (key, pair) ->
                        val (title, subtitle) = pair
                        val isSelected = selectedClassify == key
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Color(0xFF00897B).copy(0.3f)
                                    else Color.White.copy(0.08f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00897B) else Color.White.copy(0.2f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    selectedClassify = key
                                    com.armutlu.apporganizer.utils.AppPrefs.setManufacturerClassifyEnabled(context, key == "manufacturer")
                                }
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
                                Spacer(Modifier.height(4.dp))
                                Text(subtitle, fontSize = 13.sp, color = Color.White.copy(0.7f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Ana buton ────────────────────────────────────────────────
            val buttonGradient = if (currentStep == OnboardingStep.SET_LAUNCHER && !launcherSet)
                OnboardingTealGradient else OnboardingButtonGradient
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .clip(RoundedCornerShape(16.dp)).background(buttonGradient)
                    .clickable { handleOnboardingStep(
                        step = currentStep, context = context, scope = scope,
                        themePrefs = themePrefs, viewModel = viewModel,
                        launcherSet = launcherSet, notifGranted = notifGranted, notifAccessGranted = notifAccessGranted,
                        unusedGreyDays = unusedGreyDays, autoBackupEnabled = autoBackupEnabled,
                        notifTextEnabled = notifTextEnabled, swipeHintEnabled = swipeHintEnabled,
                        newBadgeEnabled = newBadgeEnabled, folderCountEnabled = folderCountEnabled,
                        navHideEnabled = navHideEnabled, selectedTheme = selectedTheme, selectedFont = selectedFont,
                        restoreFilePicker = { restoreFilePicker.launch("application/json") },
                        onRequestRole = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val rm = context.getSystemService(android.app.role.RoleManager::class.java)
                                if (rm?.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) == true)
                                    roleRequestLauncher.launch(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME))
                                else stepIndex++
                            } else {
                                context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        },
                        onRequestNotif = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else stepIndex++
                        },
                        onNotifAccess = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                        onFinish = onFinish,
                        onNextStep = { stepIndex++ }
                    )},
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        currentStep == OnboardingStep.SET_LAUNCHER && launcherSet -> "Devam Et"
                        currentStep == OnboardingStep.NOTIFICATIONS && notifGranted -> "Devam Et"
                        currentStep == OnboardingStep.NOTIF_ACCESS && notifAccessGranted -> "Devam Et"
                        else -> currentStep.buttonLabel
                    },
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
            }

            // UNUSED_GREY gün chip'leri
            if (currentStep == OnboardingStep.UNUSED_GREY) {
                Spacer(Modifier.height(12.dp))
                OnboardingGreyDayChips(unusedGreyDays) { days ->
                    unusedGreyDays = days
                    AppPrefs.setUnusedGreyDays(context, days)
                }
            }

            // SET_LAUNCHER için "Şimdi Değil"
            if (currentStep == OnboardingStep.SET_LAUNCHER && !launcherSet) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.clickable { stepIndex++ }.padding(vertical = 12.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Simdi Degil", fontSize = 14.sp, color = Color.White.copy(0.50f)) }
            }

            // İsteğe bağlı adımlar için "Atla"
            if (currentStep.isSkippable && currentStep != OnboardingStep.SET_LAUNCHER && currentStep != OnboardingStep.UNUSED_GREY) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier.clickable { stepIndex++ }.padding(vertical = 12.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Atla", fontSize = 14.sp, color = Color.White.copy(0.50f)) }
            } else {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ── Buton aksiyonu yardımcısı ────────────────────────────────────────────────

private fun handleOnboardingStep(
    step: OnboardingStep,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    themePrefs: ThemePreferences,
    viewModel: AppListViewModel,
    launcherSet: Boolean,
    notifGranted: Boolean,
    notifAccessGranted: Boolean,
    unusedGreyDays: Int,
    autoBackupEnabled: Boolean,
    notifTextEnabled: Boolean,
    swipeHintEnabled: Boolean,
    newBadgeEnabled: Boolean,
    folderCountEnabled: Boolean,
    navHideEnabled: Boolean,
    selectedTheme: AppTheme,
    selectedFont: AppFont,
    restoreFilePicker: () -> Unit,
    onRequestRole: () -> Unit,
    onRequestNotif: () -> Unit,
    onNotifAccess: () -> Unit,
    onFinish: () -> Unit,
    onNextStep: () -> Unit
) {
    when (step) {
        OnboardingStep.WELCOME        -> onNextStep()
        OnboardingStep.RESTORE_BACKUP -> restoreFilePicker()
        OnboardingStep.QUERY_PACKAGES -> onNextStep()
        OnboardingStep.NOTIFICATIONS  -> if (notifGranted) onNextStep() else onRequestNotif()
        OnboardingStep.UNUSED_GREY    -> { AppPrefs.setUnusedGreyDays(context, unusedGreyDays); onNextStep() }
        OnboardingStep.AUTO_BACKUP    -> { AppPrefs.setAutoBackupEnabled(context, autoBackupEnabled); onNextStep() }
        OnboardingStep.NOTIF_TEXT     -> { AppPrefs.setNotificationTextEnabled(context, notifTextEnabled); onNextStep() }
        OnboardingStep.NOTIF_ACCESS   -> if (notifAccessGranted) onNextStep() else onNotifAccess()
        OnboardingStep.SWIPE_HINT     -> { AppPrefs.setSwipeHintEnabled(context, swipeHintEnabled); onNextStep() }
        OnboardingStep.NEW_BADGE      -> { AppPrefs.setNewBadgeEnabled(context, newBadgeEnabled); onNextStep() }
        OnboardingStep.FOLDER_COUNT   -> { AppPrefs.setFolderCountVisible(context, folderCountEnabled); onNextStep() }
        OnboardingStep.NAV_HIDE       -> { AppPrefs.setNavButtonsHidden(context, navHideEnabled); onNextStep() }
        OnboardingStep.THEME_SELECT   -> { scope.launch { themePrefs.setTheme(selectedTheme); themePrefs.setFont(selectedFont) }; onNextStep() }
        OnboardingStep.SET_LAUNCHER   -> if (launcherSet) onNextStep() else onRequestRole()
        OnboardingStep.CLASSIFY_MODE  -> onNextStep()
        OnboardingStep.DONE           -> {
            context.getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit().putBoolean(AppPrefs.KEY_ONBOARDING_DONE, true).apply()
            onFinish()
        }
    }
}
