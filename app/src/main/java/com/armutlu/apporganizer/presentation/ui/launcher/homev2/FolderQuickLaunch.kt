package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import com.armutlu.apporganizer.domain.models.AppInfo

/**
 * Klasör hızlı başlatma seçicisi (roadmap: klasörde swipe-up → en sık kullanılan uygulama).
 *
 * Saf fonksiyon — Android bağımlılığı yok, birim testleriyle kilitlenir.
 * Görünür (gizli olmayan) uygulamalar arasından en çok açılanı seçer;
 * eşitlikte son kullanım zamanı, sonra ad (determinizm) kırar.
 */
object FolderQuickLaunchResolver {

    fun resolve(apps: List<AppInfo>): AppInfo? =
        apps
            .filter { !it.isHidden }
            .maxWithOrNull(
                compareBy<AppInfo> { it.launchCount }
                    .thenBy { it.lastUsedTimestamp }
                    .thenBy { it.appName },
            )
}
