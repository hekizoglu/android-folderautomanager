package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.ui.launcher.AppIconView

/**
 * Home V2 dock — buzlu-cam pill. İçerik LauncherViewModel.dockPackages'tan gelir
 * (mevcut bağlamsal dock motoru: sabitlenmiş + saat-dilimi önerileri); burada yalnız
 * paket adları uygulamaya çözümlenir ve render edilir. Çözümlenemeyen girişler
 * (örn. klasör pseudo-item'ları) atlanır.
 */
@Composable
internal fun DockBarV2(
    dockPackages: List<String>,
    appsByPackage: Map<String, AppInfo>,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dockApps = dockPackages.mapNotNull { appsByPackage[it] }
    if (dockApps.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dockApps.forEach { app ->
                AppIconView(
                    app = app,
                    onClick = { onAppClick(app.packageName) },
                    modifier = Modifier.size(52.dp),
                    showLabel = false,
                    iconSize = 52.dp,
                    newBadgeEnabled = false,
                    notificationBadgeEnabled = true,
                )
            }
        }
    }
}
