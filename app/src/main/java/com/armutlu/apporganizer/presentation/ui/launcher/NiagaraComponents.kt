package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category

@Composable
internal fun NiagaraLetterHeader(letter: Char, label: String? = null) {
    Text(
        text = label ?: letter.toString(),
        fontSize = if (label != null) 13.sp else 34.sp,
        fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 18.dp, bottom = 2.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NiagaraAppRow(
    app: AppInfo,
    iconSize: Dp = 40.dp,
    isActive: Boolean = false,
    sortMode: AllAppsSortMode = AllAppsSortMode.ALPHA,
    notifTextEnabled: Boolean = false,
    unusedGreyDays: Int = 0,
    iconPackPkg: String = "",
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val context       = LocalContext.current
    val primary       = MaterialTheme.colorScheme.primary
    val secondary     = MaterialTheme.colorScheme.secondary
    val onSurface     = MaterialTheme.colorScheme.onSurface
    val textSecondary = onSurface.copy(alpha = 0.55f)
    val rowHover      = onSurface.copy(alpha = 0.08f)
    val icon = rememberAppIcon(app.packageName, iconPackPkg)
    val notifColor = when {
        app.notificationCount == 0 -> null
        app.notificationImportance >= 4 -> BadgeRed
        app.notificationImportance >= 3 -> BadgeGreen
        else -> BadgeYellow
    }

    val trailingText: String? = when (sortMode) {
        AllAppsSortMode.SIZE_DESC, AllAppsSortMode.SIZE_ASC ->
            if (app.appSizeBytes > 0) formatBytes(app.appSizeBytes) else null
        AllAppsSortMode.INSTALL_DATE, AllAppsSortMode.INSTALL_DATE_ASC ->
            if (app.installTime > 0) fmtMonth(app.installTime) else null
        AllAppsSortMode.USAGE, AllAppsSortMode.USAGE_ASC ->
            if (app.usageCount > 0) formatUsageMs(app.usageCount) else null
        AllAppsSortMode.SMART, AllAppsSortMode.ALPHA, AllAppsSortMode.ALPHA_DESC -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .semantics {
                val categoryLabel = app.categoryId.ifBlank { "Uygulama" }
                val notificationLabel = when {
                    app.notificationCount <= 0 -> ""
                    app.notificationCount == 1 -> ", 1 bildirim"
                    else -> ", ${app.notificationCount} bildirim"
                }
                contentDescription = "${app.appName}, $categoryLabel$notificationLabel"
                onClick(label = context.getString(R.string.open_app)) { onClick(); true }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (isActive) rowHover else Color.Transparent)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val saturation = when {
            unusedGreyDays <= 0  -> 1f
            app.usageCount == 0L -> 0f
            app.usageCount < 5L  -> 0.4f + (app.usageCount * 0.12f)
            else                 -> 1f
        }
        val iconAlpha = if (unusedGreyDays > 0 && app.usageCount == 0L) 0.5f else 1f
        val greyFilter = if (saturation < 1f)
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) })
        else null

        Box(modifier = Modifier.size(iconSize + 8.dp), contentAlignment = Alignment.Center) {
            icon?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = app.appName,
                    modifier = Modifier.size(iconSize).clip(RoundedCornerShape(10.dp)),
                    alpha = iconAlpha,
                    colorFilter = greyFilter
                )
            } ?: run {
                Box(
                    modifier = Modifier.size(iconSize).clip(RoundedCornerShape(10.dp))
                        .background(primary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.appName.firstOrNull()?.toString() ?: "?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (notifColor != null) {
                val count = app.notificationCount
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(if (count > 9) 18.dp else 14.dp)
                        .clip(CircleShape).background(notifColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (count > 0) Text(if (count > 99) "99+" else count.toString(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.appName, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (notifTextEnabled && app.notificationText.isNotBlank()) {
                Text(
                    app.notificationText,
                    fontSize = 11.sp, color = textSecondary.copy(alpha = 0.8f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                val catLabel = remember(app.categoryId) {
                    Category.getDefaultCategories()
                        .firstOrNull { it.categoryId == app.categoryId }?.categoryName
                }
                if (!catLabel.isNullOrBlank() && app.categoryId != "other" && app.categoryId != "uncategorized") {
                    Text(catLabel, fontSize = 11.sp, color = textSecondary, maxLines = 1)
                }
            }
        }

        if (trailingText != null) {
            Text(
                trailingText, fontSize = 11.sp, color = secondary, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
