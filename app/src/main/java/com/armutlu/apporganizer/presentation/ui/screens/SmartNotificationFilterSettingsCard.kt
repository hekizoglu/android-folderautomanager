package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.NotificationBadgeMode
import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.utils.SmartNotificationPrefs

/**
 * Smart Notification Engine için AppPrefs'ten bağımsız, reaktif ayar kartı.
 * Bu kart yalnız AppOrganizer içindeki rozet/özet görünümünü değiştirir;
 * Android sistem bildirimlerini iptal etmez veya kanal ayarlarını değiştirmez.
 */
@Composable
internal fun SmartNotificationFilterSettingsCard() {
    val context = LocalContext.current
    LaunchedEffect(context) {
        SmartNotificationPrefs.initialize(context)
    }
    val settings by SmartNotificationPrefs.settings.collectAsState()
    var badgeMenuExpanded by remember { mutableStateOf(false) }

    SettingsCard {
        SettingsSwitchRow(
            icon = Icons.Default.FilterAlt,
            title = "Akıllı Bildirim Motoru",
            subtitle = if (settings.engineEnabled) {
                "Önemli bildirimleri öne çıkarır, seçtiğin türleri rozetten düşürür"
            } else {
                "Klasik aktif bildirim rozetleri kullanılıyor"
            },
            checked = settings.engineEnabled,
            onCheckedChange = { enabled ->
                SmartNotificationPrefs.setEngineEnabled(context, enabled)
            },
        )

        if (settings.engineEnabled) {
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Default.LocalOffer,
                title = "Promosyonları Filtrele",
                subtitle = "Kampanya ve indirimleri AppOrganizer rozetine dahil etme",
                checked = settings.filterPromotions,
                onCheckedChange = { enabled ->
                    SmartNotificationPrefs.setFilterPromotions(context, enabled)
                },
            )

            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Default.PrivacyTip,
                title = "Hassas İçeriği Gizle",
                subtitle = "Finans ve güvenlik bildirimlerinde içerik yerine maskeli özet göster",
                checked = settings.hideSensitiveContent,
                onCheckedChange = { enabled ->
                    SmartNotificationPrefs.setHideSensitiveContent(context, enabled)
                },
            )

            SettingsDivider()
            Box(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { badgeMenuExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (settings.badgeMode == NotificationBadgeMode.CLASSIC_APP) {
                            Icons.Default.Apps
                        } else {
                            Icons.Default.Category
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Rozet Gösterimi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(
                            text = settings.badgeMode.displayName(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = badgeMenuExpanded,
                    onDismissRequest = { badgeMenuExpanded = false },
                ) {
                    NotificationBadgeMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(mode.displayName())
                                    Text(
                                        mode.description(),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                SmartNotificationPrefs.setBadgeMode(context, mode)
                                badgeMenuExpanded = false
                            },
                        )
                    }
                }
            }

            SettingsDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
            ) {
                Text(
                    text = "Rozette Gösterilecek Türler",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NotificationCategory.values().forEach { category ->
                    val checked = category in settings.visibleCategories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(category.displayName(), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(
                                category.description(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = { enabled ->
                                val updated = settings.visibleCategories.toMutableSet().apply {
                                    if (enabled) add(category) else remove(category)
                                }
                                SmartNotificationPrefs.setVisibleCategories(context, updated)
                            },
                        )
                    }
                }
            }

            HorizontalDivider(
                Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            Text(
                text = "Bu ayarlar yalnız AppOrganizer görünümünü etkiler. Android bildirimleri silinmez veya sessize alınmaz.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

internal fun NotificationBadgeMode.displayName(): String = when (this) {
    NotificationBadgeMode.CLASSIC_APP -> "Klasik Uygulama Rozeti"
    NotificationBadgeMode.CATEGORY -> "Kategori Rozeti"
}

internal fun NotificationBadgeMode.description(): String = when (this) {
    NotificationBadgeMode.CLASSIC_APP -> "Her uygulamanın okunmamış sayısını ayrı gösterir"
    NotificationBadgeMode.CATEGORY -> "Baskın bildirim türünü ve toplam sayıyı öne çıkarır"
}

internal fun NotificationCategory.displayName(): String = when (this) {
    NotificationCategory.MESSAGING -> "Mesajlar"
    NotificationCategory.DELIVERY -> "Teslimat"
    NotificationCategory.FINANCE -> "Finans ve Güvenlik"
    NotificationCategory.PROMOTION -> "Promosyonlar"
    NotificationCategory.REMINDER -> "Hatırlatıcılar"
    NotificationCategory.SOCIAL -> "Sosyal"
    NotificationCategory.SYSTEM -> "Sistem"
    NotificationCategory.OTHER -> "Diğer"
}

internal fun NotificationCategory.description(): String = when (this) {
    NotificationCategory.MESSAGING -> "WhatsApp, SMS ve doğrudan konuşmalar"
    NotificationCategory.DELIVERY -> "Sipariş, kargo ve teslimat hareketleri"
    NotificationCategory.FINANCE -> "Banka işlemleri, ödeme ve giriş güvenliği"
    NotificationCategory.PROMOTION -> "Kampanya, indirim, kupon ve teklifler"
    NotificationCategory.REMINDER -> "Toplantı, randevu ve zamanlı hatırlatmalar"
    NotificationCategory.SOCIAL -> "Sosyal ağ etkileşimleri"
    NotificationCategory.SYSTEM -> "Android ve cihaz servisleri"
    NotificationCategory.OTHER -> "Henüz güvenle sınıflandırılamayan bildirimler"
}
