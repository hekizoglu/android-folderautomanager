package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.ui.launcher.AppIconView

@Composable
internal fun SmartAccessAppItem(
    app: AppInfo,
    notificationCount: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .testTag("smart_access_item_${app.packageName}")
            .size(width = 52.dp, height = 66.dp),
        contentAlignment = Alignment.Center,
    ) {
        BadgedBox(
            badge = {
                if (notificationCount != null && notificationCount > 0) {
                    Badge(
                        modifier = Modifier.testTag("smart_access_notification_badge_${app.packageName}"),
                        containerColor = Color(0xFFFF3B30),
                        contentColor = Color.White,
                    ) {
                        Text(if (notificationCount > 99) "99+" else notificationCount.toString())
                    }
                }
            }
        ) {
            AppIconView(
                app = app,
                onClick = onClick,
                onLongClick = onLongClick,
                showLabel = true,
                iconSize = 48.dp,
                newBadgeEnabled = false,
                notificationBadgeEnabled = false,
            )
        }
    }
}
