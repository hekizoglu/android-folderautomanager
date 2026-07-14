package com.armutlu.apporganizer.presentation.ui.screens

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import java.io.BufferedReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: AppListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // rememberSaveable — rotation/process death'te onboarding ilerlemesi kaybolmasın (D209 fix).
    // D240: varsayilan launcher secimi sistemin gorevi YENIDEN baslatmasina yol acabiliyor;
    // yeni activity kaydinda saveable state korunmaz — adim SharedPrefs'ten geri yuklenir.
    var stepIndex by rememberSaveable { mutableStateOf(AppPrefs.getOnboardingStep(context).coerceIn(0, 6)) }
    LaunchedEffect(stepIndex) { AppPrefs.setOnboardingStep(context, stepIndex) }
    // Varsayilan launcher sorusu EN SONDA sorulur (kullanici talebi, D233) —
    // tum ayarlar bitmeden kullaniciya kalici karar dayatilmaz.
    val steps = listOf(
        OnboardingStep.WELCOME,
        OnboardingStep.THEME_SELECT,
        OnboardingStep.QUICK_SETTINGS,
        OnboardingStep.ORGANIZATION_PREVIEW,
        OnboardingStep.SET_LAUNCHER,
        OnboardingStep.RESTORE_BACKUP,
        OnboardingStep.DONE,
    )
    val currentStep by rememberUpdatedState(steps[stepIndex])

    // ── State ────────────────────────────────────────────────────────────
    var launcherSet by remember { mutableStateOf(isDefaultLauncherApp(context)) }
    var selectedTheme by rememberSaveable { mutableStateOf(AppTheme.TEAL) }
    var selectedFont by rememberSaveable { mutableStateOf(AppFont.DEFAULT) }
    var restoreLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val themePrefs = remember { ThemePreferences(context) }

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
    val backupFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            restoreLoading = true
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use(BufferedReader::readText)
                } ?: error(context.getString(R.string.onb_restore_empty_file))
                val result = viewModel.importBackup(context, json)
                if (result.success) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.onb_restore_success, result.updatedCount),
                        Toast.LENGTH_SHORT
                    ).show()
                    stepIndex++
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.onb_restore_fail, result.error ?: "unknown"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    context.getString(R.string.onb_restore_read_fail, error.message ?: "unknown"),
                    Toast.LENGTH_LONG
                ).show()
            }
            restoreLoading = false
        }
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
            if (currentStep == OnboardingStep.WELCOME) {
                OnboardingStrengthsCard()
                Spacer(Modifier.height(16.dp))
            }
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
                    Triple(R.string.onb_quick_widget_title, R.string.onb_quick_widget_desc,
                        AppPrefs.isWidgetAreaEnabled(context)) to { v: Boolean -> AppPrefs.setWidgetAreaEnabled(context, v) },
                    Triple(R.string.onb_quick_suggestions_title, R.string.onb_quick_suggestions_desc,
                        AppPrefs.isSuggestionsEnabled(context)) to { v: Boolean -> AppPrefs.setSuggestionsEnabled(context, v) },
                    Triple(R.string.onb_quick_home_search_title, R.string.onb_quick_home_search_desc,
                        AppPrefs.isHomeSearchEnabled(context)) to { v: Boolean -> AppPrefs.setHomeSearchEnabled(context, v) },
                    Triple(R.string.onb_quick_folder_blur_title, R.string.onb_quick_folder_blur_desc,
                        AppPrefs.isFolderBlurEnabled(context)) to { v: Boolean -> AppPrefs.setFolderBlurEnabled(context, v) }
                )
                var quickStates by remember {
                    mutableStateOf(quickItems.map { (triple, _) -> triple.third })
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    quickItems.forEachIndexed { idx, (triple, setter) ->
                        val (titleRes, subtitleRes, _) = triple
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
                                    Text(stringResource(titleRes), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(subtitleRes), color = Color.White.copy(0.55f), fontSize = 12.sp)
                                }
                                Text(if (enabled) stringResource(R.string.onb_on) else stringResource(R.string.onb_off),
                                    color = if (enabled) Color(0xFF26C6DA) else Color.White.copy(0.35f),
                                    fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            if (currentStep == OnboardingStep.ORGANIZATION_PREVIEW) {
                Spacer(Modifier.height(8.dp))
                OnboardingOrganizationPreview(viewModel)
            }
            if (currentStep == OnboardingStep.RESTORE_BACKUP) {
                Spacer(Modifier.height(8.dp))
                OnboardingRestoreBackupCard(restoreLoading = restoreLoading)
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
                            OnboardingStep.ORGANIZATION_PREVIEW -> stepIndex++
                            OnboardingStep.RESTORE_BACKUP -> {
                                if (!restoreLoading) {
                                    backupFileLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
                                }
                            }

                            OnboardingStep.DONE -> {
                                AppPrefs.markOnboardingDone(context)
                                AppPrefs.setOnboardingStep(context, 0) // kalici adim sifirlanir (D240)
                                onFinish()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        currentStep == OnboardingStep.SET_LAUNCHER && launcherSet -> stringResource(R.string.onb_continue)
                        currentStep == OnboardingStep.RESTORE_BACKUP && restoreLoading -> stringResource(R.string.onb_restore_loading)
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

            // İsteğe bağlı adımlar için "Atla"
            if (currentStep.isSkippable) {
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

@Composable
private fun OnboardingStrengthsCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "İşte güçlü taraflarımız",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            listOf(
                Triple(Icons.Default.AutoAwesome, "Otomatik düzen", "Uygulamaları kendi kendine sınıflandırır ve klasörlere dizer."),
                Triple(Icons.Default.Folder, "Akıllı klasörler", "Kalabalığı toparlar, düzeni tek bakışta görünür yapar."),
                Triple(Icons.Default.Search, "Hızlı arama", "Uygulama, kişi, klasör ve dosya adlarına tek yerden ulaşır."),
                Triple(Icons.Default.SmartDisplay, "Ana ekran gücü", "Dock, widget ve ana ekran davranışını birlikte kontrol eder."),
                Triple(Icons.Default.Shield, "Gizlilik odaklı", "Çok şey yapar ama veriyi mümkün olduğunca cihazda tutar."),
            ).forEach { (icon, title, desc) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF26C6DA), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = desc, color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingOrganizationPreview(viewModel: AppListViewModel) {
    val state by viewModel.screenState.collectAsState()
    val pending by viewModel.pendingClassificationApps.collectAsState()
    val categorized = state.apps.count { it.categoryId != com.armutlu.apporganizer.domain.models.Category.CAT_UNCATEGORIZED }
    val activeFolders = state.categories.count { category ->
        state.apps.any { it.categoryId == category.categoryId }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OnboardingPreviewMetric("Uygulama", state.apps.size.toString(), Modifier.weight(1f))
            OnboardingPreviewMetric("Klasor", activeFolders.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OnboardingPreviewMetric("Kategorili", categorized.toString(), Modifier.weight(1f))
            OnboardingPreviewMetric("Kontrol", pending.size.toString(), Modifier.weight(1f))
        }
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(0.08f))
                .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(
                "Daha sonra Ayarlar > Uygulamalar > Kontrol Bekleyenler ekranindan dusuk guvenli kararları onaylayabilirsin.",
                color = Color.White.copy(0.72f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun OnboardingRestoreBackupCard(restoreLoading: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(0.08f))
            .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.onb_restore_card_title),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.onb_restore_card_desc),
                color = Color.White.copy(0.72f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            if (restoreLoading) {
                Text(
                    text = stringResource(R.string.onb_restore_loading),
                    color = Color(0xFF26C6DA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun OnboardingPreviewMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.10f))
            .border(1.dp, Color.White.copy(0.16f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = Color.White.copy(0.62f), fontSize = 12.sp)
    }
}
