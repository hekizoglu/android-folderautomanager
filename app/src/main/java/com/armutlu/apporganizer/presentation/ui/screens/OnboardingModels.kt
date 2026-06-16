package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ── Renkler ve gradyanlar ────────────────────────────────────────────────────

internal val OnboardingBackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
)
internal val OnboardingAccentPurple      = Color(0xFF6C63FF)
internal val OnboardingAccentPurpleLight = Color(0xFF9C8FFF)
internal val OnboardingButtonGradient = Brush.horizontalGradient(
    colors = listOf(OnboardingAccentPurple, OnboardingAccentPurpleLight)
)
internal val OnboardingTealGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF00897B), Color(0xFF26C6DA))
)

// ── Yardımcı fonksiyon ───────────────────────────────────────────────────────

internal fun isDefaultLauncherApp(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return info?.activityInfo?.packageName == context.packageName
}

// ── Onboarding adım modeli ────────────────────────────────────────────────────

internal enum class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val buttonLabel: String,
    val why: String = "",
    val isSkippable: Boolean = false
) {
    WELCOME(
        title = "AppOrganizer'a Hoş Geldiniz",
        description = "Uygulamalarınızı otomatik olarak düzenleyen akıllı launcher.",
        icon = Icons.Default.Apps,
        buttonLabel = "Başla"
    ),
    RESTORE_BACKUP(
        title = "Önceki Yedeğiniz Var Mı?",
        description = "Daha önce AppOrganizer kullandıysanız, kategori ayarlarınızı geri yükleyebilirsiniz.",
        icon = Icons.Default.Restore,
        buttonLabel = "Yedekten Geri Yükle",
        isSkippable = true
    ),
    QUERY_PACKAGES(
        title = "Uygulama Listesi İzni",
        description = "Uygulamalarınızı görmek ve düzenlemek için bu izin gereklidir.",
        icon = Icons.AutoMirrored.Filled.ManageSearch,
        buttonLabel = "İzin Ver",
        why = "Bu izin olmadan launcher çalışamaz."
    ),
    NOTIFICATIONS(
        title = "Bildirim İzni",
        description = "Uygulama ikonlarında bildirim sayısını göstermek için izin gerekli.",
        icon = Icons.Default.Notifications,
        buttonLabel = "İzin Ver",
        why = "Badge sayıları bu izinle çalışır.",
        isSkippable = true
    ),
    UNUSED_GREY(
        title = "Kullanılmayan Uygulamalar",
        description = "Hiç açmadığınız uygulamalar soluk renkte gösterilsin mi?",
        icon = Icons.Default.Visibility,
        buttonLabel = "Devam Et",
        isSkippable = true
    ),
    AUTO_BACKUP(
        title = "Otomatik Yedekleme",
        description = "Her açılışta kategori atamalarınızın JSON yedeğini alsın mı?",
        icon = Icons.Default.Badge,
        buttonLabel = "Devam Et",
        isSkippable = true
    ),
    NOTIF_TEXT(
        title = "Bildirim Metni",
        description = "Klasör ve uygulamaların altında son bildirimi göstersin mi?",
        icon = Icons.Default.Notifications,
        buttonLabel = "Devam Et",
        isSkippable = true
    ),
    NOTIF_ACCESS(
        title = "Bildirim Erişimi",
        description = "Uygulama ikonlarında gerçek bildirim sayısını göstermek için tam erişim gerekli.",
        icon = Icons.Default.Notifications,
        buttonLabel = "Erişime İzin Ver",
        why = "Android kısıtlamaları nedeniyle ayrı izin gerekiyor.",
        isSkippable = true
    ),
    SWIPE_HINT(
        title = "Swipe-up İpucu",
        description = "Ana ekranda yukarı kaydırma animasyonu gösterilsin mi?",
        icon = Icons.Default.SwipeUp,
        buttonLabel = "Devam Et",
        isSkippable = true
    ),
    NEW_BADGE(
        title = "YENİ Badge",
        description = "7 gün içinde kurulan uygulamalara 'YENİ' rozeti gösterilsin mi?",
        icon = Icons.Default.Badge,
        buttonLabel = "Devam Et",
        isSkippable = true
    ),
    FOLDER_COUNT(
        title = "Klasör Uygulama Sayısı",
        description = "Klasör simgesinin altında uygulama adedi gösterilsin mi?",
        icon = Icons.Default.Folder,
        buttonLabel = "Devam Et",
        isSkippable = true
    ),
    NAV_HIDE(
        title = "Sistem Navigasyonunu Gizle",
        description = "Geri/Home/Recents butonlarını gizle, tam ekran launcher.",
        icon = Icons.Default.Navigation,
        buttonLabel = "Devam Et",
        isSkippable = true
    ),
    THEME_SELECT(
        title = "Renk Teması Seçin",
        description = "Beğendiğiniz renk temasını ve yazı tipini seçin.",
        icon = Icons.Default.CheckCircle,
        buttonLabel = "Uygula"
    ),
    SET_LAUNCHER(
        title = "Ana Ekran Uygulaması",
        description = "AppOrganizer'ı varsayılan launcher olarak ayarlayın.",
        icon = Icons.Default.Home,
        buttonLabel = "Varsayılan Yap",
        why = "Launcher olarak ayarlanmadan ana ekran gösterilmez."
    ),
    CLASSIFY_MODE(
        title = "Nasıl Sınıflandıralım?",
        description = "Uygulamalarını kategoriye veya üreticiye göre gruplayabiliriz.",
        icon = Icons.Default.Category,
        buttonLabel = "Devam Et",
        isSkippable = true
    ),
    DONE(
        title = "Hazırsınız!",
        description = "AppOrganizer kurulumu tamamlandı. İyi kullanımlar!",
        icon = Icons.Default.CheckCircle,
        buttonLabel = "Başla"
    )
}
