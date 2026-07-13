package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo

/**
 * K2 — Override'lardan öğrenen öneri katmanı (kısmi uygulama).
 * Kullanıcı bir uygulamayı elle yeni bir kategoriye taşıdığında, benzer sinyale sahip
 * (aynı üretici prefix'i veya aynı keyword eşleşmesi) diğer uygulamalar burada listelenir.
 * Toplu kabul/red YOKTUR (opsiyonel kısayol butonları hariç) — her satır kendi checkbox'ıyla
 * bağımsız seçilir.
 */
@Composable
fun SimilarAppsSuggestionDialog(
    apps: List<AppInfo>,
    categoryName: String,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember(apps) {
        mutableStateOf(apps.map { it.packageName }.toSet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Benzer uygulamalar bulundu") },
        text = {
            Column {
                Text(
                    "\"$categoryName\" kategorisine taşıdığın uygulamaya benzeyen ${apps.size} uygulama bulundu. " +
                        "Bunlardan hangilerini de aynı kategoriye taşımak istersin?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { selected = apps.map { it.packageName }.toSet() }) {
                        Text("Hepsini Seç", fontSize = 12.sp)
                    }
                    TextButton(onClick = { selected = emptySet() }) {
                        Text("Hiçbirini Seçme", fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        val isChecked = app.packageName in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (isChecked) selected - app.packageName else selected + app.packageName
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = { checked ->
                                selected = if (checked) selected + app.packageName else selected - app.packageName
                            })
                            Spacer(Modifier.width(4.dp))
                            Text(app.appName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected) },
                enabled = selected.isNotEmpty()
            ) {
                Text("Seçilenleri Taşı (${selected.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}
