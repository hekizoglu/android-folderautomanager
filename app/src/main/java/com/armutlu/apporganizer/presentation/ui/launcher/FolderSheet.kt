// FILE 2: FolderSheet.kt
package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SheetBackground = Color(0xFF1A1A2A)
private val DividerColor    = Color(0xFFFFFFFF).copy(alpha = 0.08f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSheet(
    folder: AppFolder,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onAppClick: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = SheetBackground,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text     = folder.category.iconEmoji,
                    fontSize = 36.sp,
                )
                Column {
                    Text(
                        text       = folder.category.categoryName,
                        color      = Color.White,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text     = "${folder.apps.size} uygulama",
                        color    = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                    )
                }
            }

            // ── Divider ───────────────────────────────────────────────────────
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerColor),
            )

            // ── App grid / Empty state ────────────────────────────────────────
            if (folder.apps.isEmpty()) {
                Box(
                    modifier        = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text     = "Bu klasör boş",
                        color    = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns        = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                    modifier       = Modifier.fillMaxWidth(),
                ) {
                    items(folder.apps, key = { it.packageName }) { app ->
                        AppIconView(
                            app       = app,
                            onClick   = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAppClick(app.packageName)
                                onDismiss()
                            },
                            iconSize  = 56.dp,
                            showLabel = true,
                        )
                    }
                }
            }
        }
    }
}
