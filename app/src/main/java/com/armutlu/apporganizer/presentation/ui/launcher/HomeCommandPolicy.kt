package com.armutlu.apporganizer.presentation.ui.launcher

/**
 * Döngü P12 — Home tuşu komut çözümleyici (saf karar çekirdeği).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P12 (satır 1020-1073).
 *
 * AMAÇ: `LauncherActivity`'nin pager'a doğrudan erişmeden tek Home basışını Dashboard'a (veya
 * kullanıcının başlangıç sayfası ayarına) yönlendirmesini sağlayan SAF fonksiyon. Compose/Android
 * bağımlılığı YOKTUR — girdi/çıktı düz Kotlin veri sınıfları/enum'lardır, JVM unit testlerinden
 * doğrudan çağrılabilir.
 *
 * P00'da çıkarılan [homePressDecision] (çift-basış penceresi, bkz. LauncherActivity.kt) BU
 * dosyaya taşınmaz — [resolveHomeCommand] onu SARAR (bütünleştirir), böylece tek çağrı noktası
 * hem çift-basış hem de yeni "kapat/başlangıç sayfasına dön" kurallarını üretir.
 *
 * Kural sırası (roadmap kural tablosu, satır 1029-1035 birebir):
 * 1. All Apps açık → sadece kapat (kapanışın kendisi ViewModel.allAppsOpen ile senkron —
 *    LauncherActivity.onNewIntent bunu bu fonksiyona hiç sormadan önce ele alır, bkz. not aşağıda).
 * 2. Search/modal açık → kapatma komutu.
 * 3. İlk Home (basış penceresi dışında) → GoToStartPage komutu, basış zamanı kaydedilir.
 * 4. 500ms içinde ikinci Home → All Apps açma komutu, basış zamanı sıfırlanır.
 *
 * NOT (madde 1 neden burada yok): `allAppsOpen` durumu [LauncherViewModel] StateFlow'unda yaşar
 * ve LauncherActivity.onNewIntent bunu ilk iş kendisi kontrol edip erken döner (P00'dan beri,
 * bkz. LauncherActivity.kt satır 202-205) — bu davranış REGRESYONSUZ korunur, [resolveHomeCommand]
 * yalnızca "All Apps kapalıyken" sorulur. Bu fonksiyon çağrılırken `allAppsOpen=false` varsayılır;
 * çağıran taraf zaten bu garantiyi sağlar.
 */

/** [resolveHomeCommand] çağrısının girdisi — Home basıldığı andaki UI durumu. */
data class HomeCommandContext(
    /** Tam ekran arama veya klasör arama sorgusu aktif mi (HomeGestureContext.searchActive ile aynı anlam). */
    val searchActive: Boolean = false,
    /** Dock edit, context menu, kategori seçici, klasör context menu gibi bir modal açık mı. */
    val modalOpen: Boolean = false,
)

/** [resolveHomeCommand] çıktısı — Activity/ViewModel/HomeScreen'in uygulayacağı komut. */
sealed interface HomeCommand {
    /** Tam ekran aramayı/klasör arama sorgusunu kapat — pager'a dokunma. */
    data object CloseSearch : HomeCommand
    /** Açık modal'ı (dock edit, context menu, kategori seçici) kapat — pager'a dokunma. */
    data object CloseModal : HomeCommand
    /** Başlangıç sayfasına (Dashboard veya kullanıcı ayarına göre ilk sayfa) anında dön. */
    data object GoToStartPage : HomeCommand
    /** İkinci Home (≤500ms) — All Apps çekmecesini aç. */
    data object OpenAllApps : HomeCommand
    /** Yapacak bir şey yok (savunma amaçlı — bugün üretilmez, exhaustive when için). */
    data object None : HomeCommand
}

/** [resolveHomeCommand] çağrısının tam sonucu — komut + güncellenecek basış zaman damgası. */
data class HomeCommandResult(
    val command: HomeCommand,
    val nextLastHomePressMs: Long,
)

internal const val HOME_COMMAND_DOUBLE_PRESS_WINDOW_MS = HOME_DOUBLE_PRESS_WINDOW_MS

/**
 * Tek karar noktası. `allAppsOpen==true` iken ÇAĞRILMAZ (yukarıdaki not) — çağıran taraf bu
 * durumu kendisi ele alır.
 *
 * @param context Search/modal açık mı (drawer state ViewModel'de, search/modal state HomeScreen'de
 *   yaşadığı için bu iki bayrak çağıran tarafça toplanıp buraya taşınır).
 * @param lastHomePressMs Son kaydedilen Home basış zaman damgası (çift-basış penceresi için).
 * @param nowMs Şimdiki zaman (test edilebilirlik için enjekte edilir, System.currentTimeMillis() değil).
 */
fun resolveHomeCommand(
    context: HomeCommandContext,
    lastHomePressMs: Long,
    nowMs: Long,
): HomeCommandResult {
    // 2) Search açıkken kapatma önceliklidir (arama üstte overlay olarak durur, önce o kapanmalı).
    if (context.searchActive) {
        return HomeCommandResult(HomeCommand.CloseSearch, nextLastHomePressMs = lastHomePressMs)
    }
    if (context.modalOpen) {
        return HomeCommandResult(HomeCommand.CloseModal, nextLastHomePressMs = lastHomePressMs)
    }

    // 3-4) Search/modal kapalıyken mevcut çift-basış politikası aynen uygulanır (P00 ile bütünleşik).
    return when (val decision = homePressDecision(lastHomePressMs, nowMs)) {
        is HomePressDecision.OpenAllApps ->
            HomeCommandResult(HomeCommand.OpenAllApps, nextLastHomePressMs = decision.nextLastHomePressMs)
        is HomePressDecision.RecordPress ->
            HomeCommandResult(HomeCommand.GoToStartPage, nextLastHomePressMs = decision.nextLastHomePressMs)
    }
}
