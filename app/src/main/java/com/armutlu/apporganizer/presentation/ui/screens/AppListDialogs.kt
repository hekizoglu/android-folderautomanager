package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.Category

// ── Toplu kategori seçici ──────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BulkCategoryPicker(
    categories: List<Category>,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kategori Seç") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(categories.filter { it.categoryId != Category.CAT_UNCATEGORIZED }) { cat ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                            .combinedClickable(onClick = { onCategorySelected(cat.categoryId) })
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.iconEmoji, fontSize = 20.sp)
                            Text(cat.categoryName, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

// ── Eski uyumluluk — LauncherOrganizeDialog artık kullanılmıyor ────────────

@Composable
internal fun LauncherOrganizeDialog(
    launcherType: String,
    organizeState: OrganizeState,
    a11yConnected: Boolean,
    a11yInSystem: Boolean,
    onOrganize: (Boolean) -> Unit,
    onOpenA11ySettings: () -> Unit,
    onRestart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Otomatik Organize") },
        text = { Text("Uygulamalar launcher'da otomatik olarak kategorilere göre klasörlenecek.") },
        confirmButton = {
            Button(onClick = { onOrganize(false); onDismiss() }) { Text("Başlat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

sealed class OrganizeState {
    object Idle : OrganizeState()
    data class Running(val message: String) : OrganizeState()
    data class Done(val success: Boolean, val message: String) : OrganizeState()
}
