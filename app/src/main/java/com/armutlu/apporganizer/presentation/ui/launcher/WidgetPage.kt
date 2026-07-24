package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Döngü P0.6 — Widget sayfası (Sayfa 1, Dashboard'dan sonra).
 *
 * Widget'ları ayrı, adanmış sayfada gösterir. Serbest grid (WidgetFreeGrid)
 * veya klasik dikey Column (WidgetArea) — kullanıcı Ayarlar'dan seçer.
 *
 * Layout politikası: Büyük tablette içerik HomeAdaptiveLayoutPolicy.centeredContentMaxWidthDp()
 * ile ortalanır (arama çubuğu/dock ile tutarlı); telefon/küçük tablette fillMaxWidth.
 *
 * Klasör grid padding'i (16.dp) ile aynı padding uygulanır — görsel tutarlılık.
 */
@Composable
fun WidgetPage(
    widgetIds: List<Int>,
    widgetFreeGridEnabled: Boolean = false,
    onRemoveWidget: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val deviceClass = HomeAdaptiveLayoutPolicy.deviceClass(configuration.screenWidthDp)
    val contentMaxWidth = HomeAdaptiveLayoutPolicy.centeredContentMaxWidthDp(deviceClass)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Büyük tablette içeriği ortalayıp maksimum genişlik tut
        val contentModifier = if (contentMaxWidth != null) {
            Modifier
                .fillMaxWidth()
                .widthIn(max = contentMaxWidth.dp)
                .align(Alignment.TopCenter)
        } else {
            Modifier.fillMaxWidth()
        }

        Box(modifier = contentModifier) {
            if (widgetFreeGridEnabled) {
                // Serbest 2D grid yerleşimi
                WidgetFreeGrid(
                    widgetIds = widgetIds,
                    onRemoveWidget = onRemoveWidget,
                    editMode = false,
                    screenHeightDp = configuration.screenHeightDp,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // Klasik dikey Column düzeni
                WidgetArea(
                    widgetIds = widgetIds,
                    onRemoveWidget = onRemoveWidget,
                    editMode = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
