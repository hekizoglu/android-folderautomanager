package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.armutlu.apporganizer.domain.models.SearchDocument

data class SystemSettingEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val action: String,
    val keywords: String,
)

object SystemSettingsCatalog {
    private const val SOURCE_SETTING = "setting"

    private val entries = listOf(
        SystemSettingEntry("wifi", "Wi-Fi Ayarlari", "Kablosuz ag, internet ve baglanti", Settings.ACTION_WIFI_SETTINGS, "wifi wi fi kablosuz internet ag"),
        SystemSettingEntry("bluetooth", "Bluetooth Ayarlari", "Kulaklik, saat ve yakin cihazlar", Settings.ACTION_BLUETOOTH_SETTINGS, "bluetooth kulaklik saat cihaz baglan"),
        SystemSettingEntry("notifications", "Bildirim Erisimi", "Bildirim okuma izni ve servisleri", Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, "bildirim notification izin erisim servis"),
        SystemSettingEntry("usage", "Kullanim Erisimi", "Uygulama kullanim verisi izni", Settings.ACTION_USAGE_ACCESS_SETTINGS, "kullanim usage sure ekran izin rapor"),
        SystemSettingEntry("apps", "Uygulama Ayarlari", "Yuklu uygulamalar ve varsayilanlar", Settings.ACTION_APPLICATION_SETTINGS, "uygulama app yuklu varsayilan kaldir"),
        SystemSettingEntry("accessibility", "Erisilebilirlik", "Servisler, otomasyon ve yardimci ozellikler", Settings.ACTION_ACCESSIBILITY_SETTINGS, "erisilebilirlik accessibility servis otomasyon yardim"),
        SystemSettingEntry("display", "Ekran Ayarlari", "Parlaklik, tema ve ekran suresi", Settings.ACTION_DISPLAY_SETTINGS, "ekran parlaklik tema gorunum display"),
        SystemSettingEntry("sound", "Ses Ayarlari", "Zil sesi, medya ve titresim", Settings.ACTION_SOUND_SETTINGS, "ses zil medya titresim volume"),
        SystemSettingEntry("security", "Guvenlik Ayarlari", "Kilit, biyometri ve gizlilik", Settings.ACTION_SECURITY_SETTINGS, "guvenlik kilit sifre biyometri parmak gizlilik"),
        SystemSettingEntry("location", "Konum Ayarlari", "GPS ve konum izinleri", Settings.ACTION_LOCATION_SOURCE_SETTINGS, "konum gps lokasyon location izin"),
        SystemSettingEntry("battery", "Pil Ayarlari", "Pil tasarrufu ve batarya durumu", Settings.ACTION_BATTERY_SAVER_SETTINGS, "pil batarya battery tasarruf"),
        SystemSettingEntry("date", "Tarih ve Saat", "Saat, tarih ve bolge ayarlari", Settings.ACTION_DATE_SETTINGS, "tarih saat zaman time date"),
        SystemSettingEntry("language", "Dil Ayarlari", "Dil, klavye ve giris", Settings.ACTION_LOCALE_SETTINGS, "dil klavye locale language input"),
        SystemSettingEntry("storage", "Depolama Ayarlari", "Alan, dosyalar ve cihaz depolamasi", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "depolama storage hafiza dosya alan"),
        SystemSettingEntry("settings", "Android Ayarlari", "Genel sistem ayarlari", Settings.ACTION_SETTINGS, "ayar settings sistem telefon"),
    )

    fun documents(now: Long = System.currentTimeMillis()): List<SearchDocument> =
        entries.map { entry ->
            SearchDocument(
                sourceType = SOURCE_SETTING,
                sourceId = entry.id,
                title = entry.title,
                subtitle = "${entry.subtitle} | ${entry.keywords}",
                iconKey = entry.action,
                sourceGroup = SOURCE_SETTING,
                lastModified = now,
            )
        }

    fun open(context: Context, document: SearchDocument): Boolean {
        if (document.sourceType != SOURCE_SETTING) return false
        val entry = entries.firstOrNull { it.id == document.sourceId }
        val primary = Intent(entry?.action ?: document.iconKey).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fallback = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(primary)
            true
        }.getOrElse {
            runCatching {
                context.startActivity(fallback)
                true
            }.getOrDefault(false)
        }
    }
}
