package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

@Composable
fun ClassificationReviewScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
) {
    val pendingApps by viewModel.pendingClassificationApps.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val categories = screenState.categories
        .filter { it.categoryId != Category.CAT_UNCATEGORIZED }
        .sortedBy { it.displayOrder }
    val categoryNames = categories.associate { it.categoryId to it.categoryName }

    SettingsSubScreenScaffold(
        title = "Kontrol Bekleyenler",
        onNavigateBack = onNavigateBack,
    ) {
        item {
            SettingsCard {
                SettingsInfoRow(
                    icon = Icons.Default.ErrorOutline,
                    title = "${pendingApps.size} uygulama inceleme bekliyor",
                    subtitle = "Dusuk guvenli veya celiskili siniflandirmalari onayla, duzelt ya da 7 gun ertele."
                )
            }
        }

        if (pendingApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Bekleyen is yok", fontWeight = FontWeight.SemiBold)
                        Text("Yeni dusuk guvenli siniflandirma geldiginde burada gorunecek.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(560.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingApps, key = { it.packageName }) { app ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(app.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Oneri: ${categoryNames[app.categoryId] ?: app.categoryId} | Guven: ${app.classificationConfidence}% | Kaynak: ${app.classificationSource}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { viewModel.confirmPendingClassification(app.packageName) }) {
                                        Text("Direkt Onayla")
                                    }
                                    OutlinedButton(onClick = { viewModel.skipPendingClassification(app.packageName) }) {
                                        Text("7 gun ertele")
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Direkt Onayla: uygulama onerilen kategoriye tasinir; istersen daha sonra klasorden tekrar degistirebilirsin.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                Text("Duzelt:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(categories, key = { it.categoryId }) { category ->
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.correctPendingClassification(app.packageName, category.categoryId)
                                            }
                                        ) {
                                            Text(category.categoryName, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsCard {
                SettingsInfoRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Kural",
                    subtitle = "Onay ve duzeltme kullanici karari sayilir; otomatik siniflandirma artik bu uygulamayi ezmez."
                )
            }
        }
    }
}
