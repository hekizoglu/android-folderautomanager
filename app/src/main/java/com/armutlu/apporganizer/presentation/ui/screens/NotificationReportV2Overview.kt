package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.utils.NotificationAnalyzer

internal data class NotificationReportV2Metrics(
    val totalReceived: Int,
    val actionableCount: Int,
    val suppressedCount: Int,
    val highPriorityCount: Int,
    val nightCount: Int,
    val actionablePercent: Int,
    val suppressedPercent: Int,
) {
    companion object {
        fun from(report: NotificationAnalyzer.Report): NotificationReportV2Metrics {
            val total = report.totalReceived.coerceAtLeast(0)
            fun percent(value: Int): Int = if (total == 0) 0 else {
                ((value.coerceAtLeast(0).toDouble() / total) * 100).toInt().coerceIn(0, 100)
            }
            return NotificationReportV2Metrics(
                totalReceived = total,
                actionableCount = report.actionableCount.coerceAtLeast(0),
                suppressedCount = report.suppressedCount.coerceAtLeast(0),
                highPriorityCount = report.highPriorityCount.coerceAtLeast(0),
                nightCount = report.nightCount.coerceAtLeast(0),
                actionablePercent = percent(report.actionableCount),
                suppressedPercent = percent(report.suppressedCount),
            )
        }
    }
}

@Composable
internal fun NotificationReportV2Overview(report: NotificationAnalyzer.Report) {
    val metrics = NotificationReportV2Metrics.from(report)
    val categoryRows = report.categoryDistribution.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Akıllı Bildirim Özeti",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Text(
                text = "Son 7 gün · yalnız içeriksiz cihaz içi metadata",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth()) {
                ReportMetric(
                    modifier = Modifier.weight(1f),
                    value = "${metrics.actionableCount}",
                    label = "Eyleme değer · %${metrics.actionablePercent}",
                )
                ReportMetric(
                    modifier = Modifier.weight(1f),
                    value = "${metrics.suppressedCount}",
                    label = "Filtrelenen · %${metrics.suppressedPercent}",
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                ReportMetric(
                    modifier = Modifier.weight(1f),
                    value = "${metrics.highPriorityCount}",
                    label = "Yüksek öncelik",
                )
                ReportMetric(
                    modifier = Modifier.weight(1f),
                    value = "${metrics.nightCount}",
                    label = "Gece bildirimi",
                )
            }

            if (categoryRows.isNotEmpty()) {
                ReportDivider()
                Text(
                    text = "Kategori Dağılımı",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                categoryRows.forEach { (category, count) ->
                    val fraction = if (metrics.totalReceived == 0) 0f else {
                        (count.toFloat() / metrics.totalReceived).coerceIn(0f, 1f)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            text = category.reportDisplayName(),
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "$count",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (report.topPromotionSources.isNotEmpty()) {
                ReportDivider()
                Text(
                    text = "En Çok Promosyon Gönderenler",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text(
                    text = "Uygulama satırına dokunarak Android bildirim ayarlarını açabilirsin.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                report.topPromotionSources.take(5).forEach { stat ->
                    PromotionSourceRow(stat)
                }
            }
        }
    }
}

@Composable
private fun ReportMetric(
    modifier: Modifier,
    value: String,
    label: String,
) {
    Column(modifier.padding(end = 8.dp)) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PromotionSourceRow(stat: NotificationAnalyzer.AppNotifStats) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, stat.packageName)
                            data = Uri.parse("package:${stat.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            }
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stat.appName,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "${stat.promotionCount}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ReportDivider() {
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Spacer(Modifier.height(12.dp))
}

internal fun NotificationCategory.reportDisplayName(): String = when (this) {
    NotificationCategory.MESSAGING -> "Mesajlar"
    NotificationCategory.DELIVERY -> "Teslimat"
    NotificationCategory.FINANCE -> "Finans ve Güvenlik"
    NotificationCategory.PROMOTION -> "Promosyonlar"
    NotificationCategory.REMINDER -> "Hatırlatıcılar"
    NotificationCategory.SOCIAL -> "Sosyal"
    NotificationCategory.SYSTEM -> "Sistem"
    NotificationCategory.OTHER -> "Diğer"
}
