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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.classify.CategorySuggestionEngine
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationAttentionPolicy
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

@Composable
fun ClassificationReviewScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
) {
    val pendingApps by viewModel.classificationAttentionApps.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val allApps = screenState.apps
    val categories = screenState.categories
        .filter { it.categoryId != Category.CAT_UNCATEGORIZED }
        .sortedBy { it.displayOrder }
    val categoryNames = categories.associate { it.categoryId to it.categoryName }

    // Seçim durumu, kartın içindeki koşullu bloktan bağımsız yaşar. Böylece kullanıcı
    // kategori seçmeden öneriyi onaylayamaz ve liste yeniden çizildiğinde seçim kaybolmaz.
    val selectedCategoryByPackage = remember { mutableStateMapOf<String, String>() }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.classification_review_title),
        onNavigateBack = onNavigateBack,
    ) {
        item {
            SettingsCard {
                SettingsInfoRow(
                    icon = Icons.Default.ErrorOutline,
                    title = stringResource(R.string.classification_review_pending_count, pendingApps.size),
                    subtitle = stringResource(R.string.classification_review_pending_subtitle),
                )
            }
        }

        if (pendingApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.classification_review_empty_title),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.classification_review_empty_subtitle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            item {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(560.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(pendingApps, key = { it.packageName }) { app ->
                        val selectedCategoryId = selectedCategoryByPackage[app.packageName]

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    text = app.packageName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(
                                        R.string.classification_review_suggestion,
                                        categoryNames[app.categoryId] ?: app.categoryId,
                                        app.classificationConfidence,
                                        app.classificationSource,
                                    ),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                val attentionReason = remember(app) { ClassificationAttentionPolicy.evaluate(app) }
                                if (attentionReason != null) {
                                    Text(
                                        text = stringResource(ClassificationAttentionPolicy.reasonStringRes(attentionReason)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                // P0.7: CAT_OTHER/dusuk guvenli uygulamalar icin kategori onerisi.
                                val suggestion = remember(app.packageName, allApps.size) {
                                    CategorySuggestionEngine.suggestFor(app, allApps)
                                }
                                Spacer(Modifier.height(8.dp))
                                if (suggestion != null) {
                                    val suggestedCategoryName = categoryNames[suggestion.categoryId] ?: suggestion.categoryId
                                    val signalLabel = stringResource(
                                        when (suggestion.signal) {
                                            CategorySuggestionEngine.SignalType.VENDOR ->
                                                R.string.classification_suggestion_signal_vendor
                                            CategorySuggestionEngine.SignalType.KEYWORD ->
                                                R.string.classification_suggestion_signal_keyword
                                            CategorySuggestionEngine.SignalType.SIMILAR_PACKAGE ->
                                                R.string.classification_suggestion_signal_similar_package
                                        }
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.classification_suggestion_label,
                                                suggestedCategoryName,
                                                signalLabel,
                                            ),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.weight(1f),
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                selectedCategoryByPackage[app.packageName] = suggestion.categoryId
                                            },
                                        ) {
                                            Text(
                                                stringResource(R.string.classification_suggestion_apply),
                                                fontSize = 12.sp,
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.classification_suggestion_none),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.classification_review_choose_category),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                )
                                Spacer(Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(categories, key = { it.categoryId }) { category ->
                                        val selected = selectedCategoryId == category.categoryId
                                        if (selected) {
                                            Button(
                                                onClick = {
                                                    selectedCategoryByPackage[app.packageName] = category.categoryId
                                                },
                                            ) {
                                                Text(category.categoryName, fontSize = 12.sp)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = {
                                                    selectedCategoryByPackage[app.packageName] = category.categoryId
                                                },
                                            ) {
                                                Text(category.categoryName, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        enabled = selectedCategoryId != null,
                                        onClick = {
                                            val categoryId = selectedCategoryId ?: return@Button
                                            if (categoryId == app.categoryId) {
                                                viewModel.confirmPendingClassification(app.packageName)
                                            } else {
                                                viewModel.correctPendingClassification(app.packageName, categoryId)
                                            }
                                            selectedCategoryByPackage.remove(app.packageName)
                                        },
                                    ) {
                                        Text(stringResource(R.string.classification_review_confirm_selection))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            selectedCategoryByPackage.remove(app.packageName)
                                            viewModel.skipPendingClassification(app.packageName)
                                        },
                                    ) {
                                        Text(stringResource(R.string.classification_review_snooze))
                                    }
                                }
                                if (selectedCategoryId == null) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.classification_review_selection_required),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
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
                    title = stringResource(R.string.classification_review_rule_title),
                    subtitle = stringResource(R.string.classification_review_rule_subtitle),
                )
            }
        }
    }
}
