package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestionType
import com.armutlu.apporganizer.presentation.ui.launcher.LauncherActivity
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

@Composable
fun FolderSuggestionsScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val suggestions by viewModel.folderSuggestions.collectAsState()
    val infoDismissed by viewModel.folderSuggestionsInfoDismissed.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val categoryNames = screenState.categories.associate { it.categoryId to it.categoryName }

    SettingsSubScreenScaffold(
        title = "Klasor Onerileri",
        onNavigateBack = onNavigateBack,
    ) {
        item {
            SettingsCard {
                SettingsInfoRow(
                    icon = Icons.Default.Lightbulb,
                    title = "${suggestions.size} uygulanabilir oneri",
                    subtitle = "Oneriler yalnizca sen Kabul Et dediginde kategori tasir; silme veya gizleme yapmaz."
                )
            }
        }

        if (!infoDismissed) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Bu ozellik ne yapiyor?", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Buyuk, cok kucuk veya uzun suredir atil kalan klasorleri bulur. Dusuk guvenli siniflandirmalar otomatik uygulanmaz; Kontrol Bekleyenler akisina gider.",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = viewModel::dismissFolderSuggestionsInfo) {
                            Text("Anladim")
                        }
                    }
                }
            }
        }

        if (suggestions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Yeni oneri yok", fontWeight = FontWeight.SemiBold)
                        Text("Klasorler dengelendiginde ya da oneriler ertelendiginde bu ekran bos kalir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(560.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(suggestions, key = { it.id }) { suggestion ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(suggestion.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(suggestion.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${typeLabel(suggestion.type)} | Hedef: ${categoryNames[suggestion.targetCategoryId] ?: suggestion.targetCategoryId} | Guven: ${suggestion.confidence}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        viewModel.acceptFolderSuggestion(suggestion.id)
                                        // Kabul edilen oneri hedef klasore tasindi — kullaniciyi
                                        // dogrudan LauncherActivity'deki ilgili klasore yonlendir.
                                        val intent = Intent(context, LauncherActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            putExtra(
                                                LauncherActivity.EXTRA_OPEN_FOLDER_CATEGORY_ID,
                                                suggestion.targetCategoryId,
                                            )
                                        }
                                        context.startActivity(intent)
                                    }) {
                                        Text("Kabul et")
                                    }
                                    OutlinedButton(onClick = { viewModel.snoozeFolderSuggestion(suggestion.id) }) {
                                        Text("7 gun ertele")
                                    }
                                    OutlinedButton(onClick = { viewModel.dismissFolderSuggestion(suggestion.id) }) {
                                        Text("Gizle")
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
                    icon = Icons.Default.AutoFixHigh,
                    title = "Geri al",
                    subtitle = "Kabul edilen oneriler kullanici duzeltmesi olarak kaydedilir; ayni paketleri Uygulamalar ekranindan tekrar tasiyabilirsin."
                )
            }
        }
    }
}

private fun typeLabel(type: FolderSuggestionType): String = when (type) {
    FolderSuggestionType.SPLIT_LARGE_FOLDER -> "Bolme"
    FolderSuggestionType.MERGE_SMALL_FOLDER -> "Birlestirme"
    FolderSuggestionType.CLEAN_UNUSED_APPS -> "Temizlik"
}
