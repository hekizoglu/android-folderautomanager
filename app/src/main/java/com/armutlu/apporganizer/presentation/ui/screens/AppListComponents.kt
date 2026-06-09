package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category

// ── Yükleniyor iskelet ──────────────────────────────────────────────────────

@Composable
internal fun LoadingSkeleton() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(12) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().height(68.dp)
            ) {}
        }
    }
}

// ── Boş durum ──────────────────────────────────────────────────────────────

@Composable
internal fun AppEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Kategori seçici dialog ─────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CategoryPickerDialog(
    app: AppInfo,
    categories: List<Category>,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kategori Seç — ${app.appName}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(categories.filter { it.categoryId != Category.CAT_UNCATEGORIZED }) { cat ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (app.categoryId == cat.categoryId)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                            .combinedClickable(onClick = { onCategorySelected(cat.categoryId) })
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.iconEmoji, fontSize = 20.sp)
                            Text(cat.categoryName, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (app.categoryId == cat.categoryId)
                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
