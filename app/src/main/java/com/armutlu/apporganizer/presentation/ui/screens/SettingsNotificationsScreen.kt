package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.armutlu.apporganizer.utils.AppPrefs

/**
 * U1: Bildirim alt ekranı — bildirim erişimi, akıllı badge rengi,
 * kullanım bilgisi ve akıllı bildirimler.
 * İçerik eski SettingsScreen'den birebir taşındı, fonksiyonellik değişmedi.
 * Reaktif AppPrefs pattern'i (DisposableEffect + listener) korunuyor (LEARNINGS P9).
 */
@Composable
fun SettingsNotificationsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    SettingsSubScreenScaffold(title = "Bildirimler", onNavigateBack = onNavigateBack) {

        // ── Bildirim İzni ─────────────────────────────────────────────────
        item { SettingsSectionTitle("Bildirim Erişimi") }
        item {
            var notifListenerOk by remember { mutableStateOf(isNotificationListenerGranted(context)) }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        notifListenerOk = isNotificationListenerGranted(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notifications, null,
                        tint = if (notifListenerOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Bildirim Erişimi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(
                            if (notifListenerOk) "Aktif — badge sayıları çalışıyor" else "Kapalı — badge'ler görünmez",
                            fontSize = 12.sp,
                            color = if (notifListenerOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!notifListenerOk) {
                        Button(
                            onClick = {
                                val i = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                runCatching { context.startActivity(i) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("Bildirim Erişimini Aç", fontSize = 13.sp) }
                    }
                }
                if (!notifListenerOk) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text(
                            "Tüm bildirim verileri yalnızca cihazınızda kalır — hiçbir veri dışarı çıkmaz.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Badge Intelligence toggle
        item {
            var badgeIntelligence by remember { mutableStateOf(AppPrefs.isBadgeIntelligenceEnabled(context)) }
            DisposableEffect(context) {
                val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == AppPrefs.KEY_BADGE_INTELLIGENCE) {
                        badgeIntelligence = AppPrefs.isBadgeIntelligenceEnabled(context)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Akıllı Badge Rengi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Mesaj=yeşil · Alarm=kırmızı · Güncelleme=sarı", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = badgeIntelligence,
                        onCheckedChange = {
                            badgeIntelligence = it
                            AppPrefs.setBadgeIntelligenceEnabled(context, it)
                        }
                    )
                }
            }
        }

        // Kullanım Bilgisi toggle — klasör altında "X gündür açılmadı" alt yazısı
        item {
            var unusedInfoEnabled by remember { mutableStateOf(AppPrefs.isUnusedInfoEnabled(context)) }
            DisposableEffect(context) {
                val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == AppPrefs.KEY_UNUSED_INFO_ENABLED) {
                        unusedInfoEnabled = AppPrefs.isUnusedInfoEnabled(context)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Kullanım Bilgisi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Klasör altında \"X gündür açılmadı\" göster — bildirim metni önceliklidir", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = unusedInfoEnabled,
                        onCheckedChange = {
                            unusedInfoEnabled = it
                            AppPrefs.setUnusedInfoEnabled(context, it)
                        }
                    )
                }
            }
        }

        // ── Akıllı Bildirimler ────────────────────────────────────────────
        item { SettingsSectionTitle("Akıllı Bildirimler") }
        item {
            var masterEnabled  by remember { mutableStateOf(AppPrefs.isSmartNotifEnabled(context)) }
            var expanded       by remember { mutableStateOf(masterEnabled) }
            var dailyUsage     by remember { mutableStateOf(AppPrefs.isSmartNotifDailyUsage(context)) }
            var unusedApps     by remember { mutableStateOf(AppPrefs.isSmartNotifUnusedApps(context)) }
            var catStats       by remember { mutableStateOf(AppPrefs.isSmartNotifCatStats(context)) }
            var notifHour      by remember { mutableStateOf(AppPrefs.getSmartNotifHour(context)) }
            var hourMenuExpanded by remember { mutableStateOf(false) }
            val workerCtx = context
            DisposableEffect(context) {
                val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        AppPrefs.KEY_SMART_NOTIF_ENABLED -> masterEnabled = AppPrefs.isSmartNotifEnabled(context)
                        AppPrefs.KEY_SMART_NOTIF_DAILY_USAGE -> dailyUsage = AppPrefs.isSmartNotifDailyUsage(context)
                        AppPrefs.KEY_SMART_NOTIF_UNUSED_APPS -> unusedApps = AppPrefs.isSmartNotifUnusedApps(context)
                        AppPrefs.KEY_SMART_NOTIF_CAT_STATS -> catStats = AppPrefs.isSmartNotifCatStats(context)
                        AppPrefs.KEY_SMART_NOTIF_HOUR -> notifHour = AppPrefs.getSmartNotifHour(context)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Akıllı Bildirimler", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(
                            if (masterEnabled) "Günlük içgörüler ve temizlik önerileri" else "Kapalı",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp).padding(end = 4.dp)
                    )
                    Switch(
                        checked = masterEnabled,
                        onCheckedChange = { v ->
                            masterEnabled = v
                            AppPrefs.setSmartNotifEnabled(workerCtx, v)
                            com.armutlu.apporganizer.workers.SmartInsightWorker.schedule(workerCtx)
                            if (v) expanded = true
                        }
                    )
                }
                if (expanded && masterEnabled) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    SettingsSwitchRow(
                        icon = Icons.Default.BarChart,
                        title = "Günlük Kullanım",
                        subtitle = "En çok kullandığın uygulamayı ve oturum sayısını bildir",
                        checked = dailyUsage,
                        onCheckedChange = { dailyUsage = it; AppPrefs.setSmartNotifDailyUsage(workerCtx, it) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    SettingsSwitchRow(
                        icon = Icons.Default.DeleteSweep,
                        title = "Kullanılmayan Uygulamalar",
                        subtitle = "3+ haftadır açılmayan uygulamalar için temizlik önerisi",
                        checked = unusedApps,
                        onCheckedChange = { unusedApps = it; AppPrefs.setSmartNotifUnusedApps(workerCtx, it) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    SettingsSwitchRow(
                        icon = Icons.Default.FolderOpen,
                        title = "Klasör İstatistikleri",
                        subtitle = "Kalabalık klasörler için organizasyon önerileri",
                        checked = catStats,
                        onCheckedChange = { catStats = it; AppPrefs.setSmartNotifCatStats(workerCtx, it) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    Box(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hourMenuExpanded = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Bildirim Saati", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text(
                                    "Saat %02d:00".format(notifHour),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = hourMenuExpanded, onDismissRequest = { hourMenuExpanded = false }) {
                            listOf(8, 12, 18, 20, 22).forEach { hour ->
                                DropdownMenuItem(
                                    text = { Text("Saat %02d:00".format(hour)) },
                                    onClick = {
                                        notifHour = hour
                                        AppPrefs.setSmartNotifHour(workerCtx, hour)
                                        com.armutlu.apporganizer.workers.SmartInsightWorker.schedule(workerCtx)
                                        hourMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
