package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsDrawer(
    apps: List<AppInfo>,
    onClose: () -> Unit,
    onAppClick: (String) -> Unit,
    iconSize: Dp = 56.dp
) {
    var query by remember { mutableStateOf("") }

    val grouped: Map<String, List<AppInfo>> = remember(apps) {
        apps
            .sortedBy { it.appName.lowercase() }
            .groupBy { app ->
                val first = app.appName.firstOrNull()
                if (first != null && first.isLetter()) first.uppercaseChar().toString()
                else "#"
            }
            // Put "#" first, then alphabetical
            .entries
            .sortedWith(compareBy { if (it.key == "#") "" else it.key })
            .associate { it.key to it.value }
    }

    val filteredApps: List<AppInfo> = remember(apps, query) {
        if (query.isBlank()) emptyList()
        else apps.filter { it.appName.contains(query, ignoreCase = true) }
            .sortedBy { it.appName.lowercase() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A).copy(alpha = 0.96f))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tüm Uygulamalar",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Color.White
                    )
                }
            }

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Ara...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            cursorBrush = SolidColor(Color.White),
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            if (query.isNotEmpty()) {
                // Search results: flat grid (4 columns)
                val rows = filteredApps.chunked(4)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(rows) { rowApps ->
                        AppRow(
                            rowApps = rowApps,
                            columnCount = 4,
                            iconSize = iconSize,
                            onAppClick = onAppClick
                        )
                    }
                }
            } else {
                // Alphabetical grouped view
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    grouped.forEach { (letter, letterApps) ->
                        stickyHeader(key = "header_$letter") {
                            Text(
                                text = letter,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0A0A1A).copy(alpha = 0.96f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        val rows = letterApps.chunked(4)
                        items(rows, key = { row -> "${letter}_${row.first().packageName}" }) { rowApps ->
                            AppRow(
                                rowApps = rowApps,
                                columnCount = 4,
                                iconSize = iconSize,
                                onAppClick = onAppClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    rowApps: List<AppInfo>,
    columnCount: Int,
    iconSize: Dp,
    onAppClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        rowApps.forEach { app ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AppIconView(
                    app = app,
                    onClick = { onAppClick(app.packageName) },
                    iconSize = iconSize
                )
            }
        }
        // Fill remaining slots so layout stays aligned
        repeat(columnCount - rowApps.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
