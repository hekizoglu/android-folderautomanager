package com.armutlu.apporganizer.presentation.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.armutlu.apporganizer.presentation.ui.theme.AppFont
import com.armutlu.apporganizer.presentation.ui.theme.AppTheme
import com.armutlu.apporganizer.presentation.ui.theme.ThemePreferences
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

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
    RESTORE_BACKUP(
        icon = Icons.Default.Restore,
        title = "Onceki Yedeginiz Var Mi?",
        description = "Daha once App Organizer kullandiyseniz, uygulama duzenlemenizi JSON yedek dosyasindan geri yukleyebilirsiniz.",
        why = "Yedek bulunmazsa bu adimi atlayabilirsiniz. Geri yukleme kategori ve klasor duzenlemenizi korur.",
        buttonLabel = "Yedegi Geri Yukle",
        isRequired = false,
        isSkippable = true
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
    UNUSED_GREY(
        icon = Icons.Default.Visibility,
        title = "Kullanilmayan Uygulamalar",
        description = "Hic acilmamis uygulamalar ana ekranda soluk/gri gorunsun mu? Kalabalik azalir, odak artar.",
        why = "Hic kullanmadiginiz uygulamalar soluk gozukur — silmeden once fark edersiniz.",
        buttonLabel = "Ayarla",
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
    AUTO_BACKUP(
        icon = Icons.Default.Info,
        title = "Otomatik Yedekleme",
        description = "Her gun uygulama duzenlemeniz otomatik olarak yedeklensin mi? Telefon degistirdiginizde tek tikla geri yukleyin.",
        why = "Yedekleme sadece cihazinizda JSON dosyasi olarak saklanir, hicbir yere gonderilmez.",
        buttonLabel = "Etkinlestir",
        isRequired = false,
        isSkippable = true
    ),
    NOTIF_TEXT(
        icon = Icons.Default.Notifications,
        title = "Bildirim Metni",
        description = "Klasorlerin altinda ve tum uygulamalar ekraninda son gelen bildirimin metnini goruntulemek ister misiniz?",
        why = "WhatsApp veya mesajlasma uygulamalarinda son mesaji ana ekrandan gorebilirsiniz.",
        buttonLabel = "Devam Et",
        isRequired = false,
        isSkippable = false
    ),
    NOTIF_ACCESS(
        icon = Icons.Default.Notifications,
        title = "Bildirim Erisimi",
        description = "Bildirim metnini gosterebilmek icin sistem bildirim erisimi gereklidir.\n\nAcilan ekranda 'App Organizer'i bulun ve etkinlestirin.",
        why = "Bu izin olmadan bildirim metni hic gosterilmez. Veriler cihazinizda kalir, disari gonderilmez.",
        buttonLabel = "Izin Ver",
        isRequired = false,
        isSkippable = true
    ),
    SWIPE_HINT(
        icon = Icons.Default.SwipeUp,
        title = "Swipe-Up Ipucu",
        description = "Klasorun altinda en cok kullanilan uygulamanin adi gorunsun mu? Yukari kaydirarak o uygulamaya hizlica ulasabilirsiniz.",
        why = "Klasorden tek hareketle favori uygulamanizi acacaksiniz — aciklama olmadan kesfetmek zor.",
        buttonLabel = "Devam Et",
        isRequired = false,
        isSkippable = false
    ),
    NEW_BADGE(
        icon = Icons.Default.Badge,
        title = "Yeni Uygulama Rozeti",
        description = "Son 7 gunde yuklenen uygulamalara 'YENI' rozeti gorunsun mu?",
        why = "Yeni yuklediginiz uygulamalari kolay fark edersiniz.",
        buttonLabel = "Devam Et",
        isRequired = false,
        isSkippable = false
    ),
    FOLDER_COUNT(
        icon = Icons.Default.Folder,
        title = "Klasor Uygulama Sayisi",
        description = "Klasorun altinda kac uygulama oldugu gorunsun mu?",
        why = "Klasorun icine bakmadan hizlica dolu mu bos mu anlayabilirsiniz.",
        buttonLabel = "Devam Et",
        isRequired = false,
        isSkippable = false
    ),
    NAV_HIDE(
        icon = Icons.Default.Navigation,
        title = "Sistem Navigasyonu",
        description = "Ana ekranda geri/home/recent tuslarini gizleyin, tam ekran launcher deneyimi yasayin.",
        why = "Geri tulari gizlemek ekrani buyutür. Sistem navigate'i yine de swipe gesture ile kullanilabilir.",
        buttonLabel = "Devam Et",
        isRequired = false,
        isSkippable = false
    ),
    THEME_SELECT(
        icon = Icons.Default.Info,
        title = "Tema Secin",
        description = "Uygulamanin gorunumunu kisisellestirebilirsiniz. Daha sonra Ayarlar ekranindan degistirebilirsiniz.",
        why = "",
        buttonLabel = "Devam Et",
        isRequired = false,
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
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: AppListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var stepIndex by remember { mutableStateOf(0) }
    // SET_LAUNCHER sona alındı — tüm ayarlar bittikten sonra sorulsun
    val steps = listOf(
        OnboardingStep.WELCOME,
        OnboardingStep.RESTORE_BACKUP,
        OnboardingStep.QUERY_PACKAGES,
        OnboardingStep.NOTIFICATIONS,
        OnboardingStep.UNUSED_GREY,
        OnboardingStep.AUTO_BACKUP,
        OnboardingStep.NOTIF_TEXT,
        OnboardingStep.NOTIF_ACCESS,
        OnboardingStep.SWIPE_HINT,
        OnboardingStep.NEW_BADGE,
        OnboardingStep.FOLDER_COUNT,
        OnboardingStep.NAV_HIDE,
        OnboardingStep.THEME_SELECT,
        OnboardingStep.SET_LAUNCHER,
        OnboardingStep.DONE,
    )
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
    var unusedGreyDays by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getUnusedGreyDays(context)) }
    var autoBackupEnabled by remember { mutableStateOf(true) }
    var notifTextEnabled by remember { mutableStateOf(true) }
    var notifAccessGranted by remember {
        mutableStateOf(
            android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            )?.contains(context.packageName) == true
        )
    }
    var restoreResult by remember { mutableStateOf<String?>(null) }
    var swipeHintEnabled by remember { mutableStateOf(true) }
    var newBadgeEnabled by remember { mutableStateOf(true) }
    var folderCountEnabled by remember { mutableStateOf(true) }
    var navHideEnabled by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(AppTheme.TEAL) }
    var selectedFont  by remember { mutableStateOf(AppFont.DEFAULT) }
    val scope = rememberCoroutineScope()
    val themePrefs = remember { ThemePreferences(context) }

    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.readText() ?: return@launch
                    val result = viewModel.importBackup(json)
                    restoreResult = if (result.success)
                        "${result.updatedCount} uygulama geri yuklendi"
                    else
                        "Geri yukleme basarisiz: ${result.error}"
                }.onFailure {
                    restoreResult = "Dosya okunamadi: ${it.message}"
                }
            }
        }
    }

    val currentStep by rememberUpdatedState(step)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                launcherSet = isDefaultLauncher(context)
                notifAccessGranted = android.provider.Settings.Secure.getString(
                    context.contentResolver, "enabled_notification_listeners"
                )?.contains(context.packageName) == true
                if (launcherSet && currentStep == OnboardingStep.SET_LAUNCHER) stepIndex++
                if (notifAccessGranted && currentStep == OnboardingStep.NOTIF_ACCESS) stepIndex++
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
                OnboardingStep.NOTIF_ACCESS -> if (notifAccessGranted) "Bildirim erisimi verildi" else null
                OnboardingStep.UNUSED_GREY -> if (unusedGreyDays > 0) "$unusedGreyDays gun ayarlandi" else null
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

            // Geri yükleme sonucu mesajı
            if (currentStep == OnboardingStep.RESTORE_BACKUP && restoreResult != null) {
                val result = restoreResult ?: ""
                val isSuccess = result.contains("geri yuklendi")
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSuccess) Color(0xFF00897B).copy(alpha = 0.25f)
                            else Color(0xFFB00020).copy(alpha = 0.25f)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        result,
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (isSuccess) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TealGradient)
                            .clickable { stepIndex++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Devam Et", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Toggle seçici — AUTO_BACKUP, NOTIF_TEXT, SWIPE_HINT, NEW_BADGE, FOLDER_COUNT, NAV_HIDE
            val toggleState: Boolean? = when (currentStep) {
                OnboardingStep.AUTO_BACKUP -> autoBackupEnabled
                OnboardingStep.NOTIF_TEXT -> notifTextEnabled
                OnboardingStep.SWIPE_HINT -> swipeHintEnabled
                OnboardingStep.NEW_BADGE -> newBadgeEnabled
                OnboardingStep.FOLDER_COUNT -> folderCountEnabled
                OnboardingStep.NAV_HIDE -> navHideEnabled
                else -> null
            }
            if (toggleState != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    listOf(true to "Acik", false to "Kapali").forEach { (value, label) ->
                        val selected = toggleState == value
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) Color(0xFF00897B)
                                    else Color.White.copy(alpha = 0.15f)
                                )
                                .clickable {
                                    when (currentStep) {
                                        OnboardingStep.AUTO_BACKUP -> autoBackupEnabled = value
                                        OnboardingStep.NOTIF_TEXT -> notifTextEnabled = value
                                        OnboardingStep.SWIPE_HINT -> swipeHintEnabled = value
                                        OnboardingStep.NEW_BADGE -> newBadgeEnabled = value
                                        OnboardingStep.FOLDER_COUNT -> folderCountEnabled = value
                                        OnboardingStep.NAV_HIDE -> navHideEnabled = value
                                        else -> {}
                                    }
                                }
                                .padding(horizontal = 32.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Tema seçim UI — sadece THEME_SELECT adımında göster
            if (currentStep == OnboardingStep.THEME_SELECT) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Renk Teması",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(AppTheme.entries) { theme ->
                        val isSelected = selectedTheme == theme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTheme = theme }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(theme.primary)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                theme.label,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Yazı Tipi",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFont.entries.forEach { font ->
                        val isSelected = selectedFont == font
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) AccentPurple else Color.White.copy(alpha = 0.12f)
                                )
                                .clickable { selectedFont = font }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                font.label,
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
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

                            OnboardingStep.RESTORE_BACKUP -> {
                                restoreFilePicker.launch("application/json")
                            }

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

                            OnboardingStep.UNUSED_GREY -> {
                                // Seçim yapıldıysa kaydet, kaydedilmediyse kapalı bırak
                                com.armutlu.apporganizer.utils.AppPrefs.setUnusedGreyDays(context, unusedGreyDays)
                                stepIndex++
                            }

                            OnboardingStep.AUTO_BACKUP -> {
                                com.armutlu.apporganizer.utils.AppPrefs.setAutoBackupEnabled(context, autoBackupEnabled)
                                stepIndex++
                            }

                            OnboardingStep.NOTIF_TEXT -> {
                                com.armutlu.apporganizer.utils.AppPrefs.setNotificationTextEnabled(context, notifTextEnabled)
                                stepIndex++
                            }

                            OnboardingStep.NOTIF_ACCESS -> {
                                if (notifAccessGranted) { stepIndex++; return@clickable }
                                context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }

                            OnboardingStep.SWIPE_HINT -> {
                                com.armutlu.apporganizer.utils.AppPrefs.setSwipeHintEnabled(context, swipeHintEnabled)
                                stepIndex++
                            }

                            OnboardingStep.NEW_BADGE -> {
                                com.armutlu.apporganizer.utils.AppPrefs.setNewBadgeEnabled(context, newBadgeEnabled)
                                stepIndex++
                            }

                            OnboardingStep.FOLDER_COUNT -> {
                                com.armutlu.apporganizer.utils.AppPrefs.setFolderCountVisible(context, folderCountEnabled)
                                stepIndex++
                            }

                            OnboardingStep.NAV_HIDE -> {
                                com.armutlu.apporganizer.utils.AppPrefs.setNavButtonsHidden(context, navHideEnabled)
                                stepIndex++
                            }

                            OnboardingStep.THEME_SELECT -> {
                                scope.launch {
                                    themePrefs.setTheme(selectedTheme)
                                    themePrefs.setFont(selectedFont)
                                }
                                stepIndex++
                            }

                            OnboardingStep.DONE -> {
                                context.getSharedPreferences(
                                    com.armutlu.apporganizer.utils.AppPrefs.PREFS_NAME,
                                    android.content.Context.MODE_PRIVATE
                                ).edit().putBoolean(
                                    com.armutlu.apporganizer.utils.AppPrefs.KEY_ONBOARDING_DONE, true
                                ).apply()
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
                        currentStep == OnboardingStep.NOTIF_ACCESS && notifAccessGranted -> "Devam Et"
                        else -> currentStep.buttonLabel
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // UNUSED_GREY için gün seçici chip'leri
            if (currentStep == OnboardingStep.UNUSED_GREY) {
                Spacer(Modifier.height(12.dp))
                val options = listOf(0 to "Kapalı", 7 to "7 gün", 14 to "14 gün", 30 to "30 gün")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    options.forEach { (days, label) ->
                        val selected = unusedGreyDays == days
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) Color(0xFF00897B) else Color.White.copy(alpha = 0.15f))
                                .clickable {
                                    unusedGreyDays = days
                                    com.armutlu.apporganizer.utils.AppPrefs.setUnusedGreyDays(context, days)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
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
                currentStep != OnboardingStep.UNUSED_GREY
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
