package com.armutlu.apporganizer.presentation.ui.screens.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.ui.screens.AppListScreenState
import com.armutlu.apporganizer.presentation.ui.screens.OnboardingAccentPurple
import com.armutlu.apporganizer.presentation.ui.screens.OnboardingButtonGradient
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

/**
 * Yenilenmiş Onboarding Kategori Önizleme Ekranı.
 * Gerçek kategoriler, pastel kartlar, ikon grid'leri ve kullanıcı onay butonları.
 */
@Composable
fun OnboardingCategoryPreview(
    viewModel: AppListViewModel,
    onUseLayout: () -> Unit,
    onEditFolders: () -> Unit,
    onReviewPending: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.screenState.collectAsState()
    val pendingApps by viewModel.classificationAttentionApps.collectAsState()
    val context = LocalContext.current

    val uiModel = remember(state.apps, state.categories, state.isProcessing, pendingApps) {
        buildOnboardingCategoryPreviewUiModel(context, state, pendingApps)
    }

    var showAllCategories by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val gridColumns = if (isTablet) 3 else 2

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // 1. Üst Alan (Kompakt Başlık)
        OnboardingPreviewHeader(uiModel = uiModel)

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Orta İçerik (Kategori Kartları Grid'i / Yükleme / Boş Durum)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (uiModel.state) {
                OnboardingPreviewState.LOADING -> {
                    OnboardingPreviewLoadingState()
                }

                OnboardingPreviewState.EMPTY -> {
                    OnboardingPreviewEmptyState(onRetry = {
                        // Retry app sync
                    })
                }

                OnboardingPreviewState.SUCCESS, OnboardingPreviewState.ERROR -> {
                    val displayedCategories = if (showAllCategories || uiModel.categories.size <= 6) {
                        uiModel.categories
                    } else {
                        uiModel.categories.take(6)
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            items(
                                items = displayedCategories,
                                key = { it.categoryId },
                            ) { categoryCardModel ->
                                OnboardingCategoryCard(
                                    model = categoryCardModel,
                                    modifier = Modifier.height(160.dp),
                                )
                            }
                        }

                        // "Tüm klasörleri göster" butonu (6'dan fazla kategori varsa)
                        if (!showAllCategories && uiModel.categories.size > 6) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable { showAllCategories = true }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.onb_preview_show_all_folders, uiModel.categories.size),
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        // 3. Kontrol Bekleyenler Kartı (Dikkat Kartı - Varsa)
                        if (uiModel.pendingCount > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OnboardingPendingAttentionCard(
                                pendingCount = uiModel.pendingCount,
                                onReviewNow = onReviewPending,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Sabit Alt Eylem Alanı (Birincil & İkincil Butonlar)
        OnboardingBottomActionArea(
            onUseLayout = onUseLayout,
            onEditFolders = onEditFolders,
        )
    }
}

@Composable
private fun OnboardingPreviewHeader(uiModel: OnboardingCategoryPreviewUiModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.onb_preview_header_title),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (uiModel.state == OnboardingPreviewState.LOADING) {
                stringResource(R.string.onb_preview_loading_subtitle)
            } else {
                stringResource(R.string.onb_preview_header_subtitle, uiModel.totalAppCount, uiModel.totalFolderCount)
            },
            color = Color.White.copy(alpha = 0.70f),
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun OnboardingPreviewLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(
                color = OnboardingAccentPurple,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
            )
            Text(
                text = stringResource(R.string.onb_preview_loading_title),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.onb_preview_loading_subtitle),
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun OnboardingPreviewEmptyState(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.onb_preview_empty_title),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.onb_preview_empty_subtitle),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OnboardingPendingAttentionCard(
    pendingCount: Int,
    onReviewNow: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFF9800).copy(alpha = 0.15f))
            .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable { onReviewNow() }
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9800).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.onb_preview_attention_title, pendingCount),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.onb_preview_attention_desc),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        maxLines = 2,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.onb_preview_attention_action),
                    color = Color(0xFFFFB74D),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun OnboardingBottomActionArea(
    onUseLayout: () -> Unit,
    onEditFolders: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Birincil Buton: "Bu düzeni kullan"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(OnboardingButtonGradient)
                .clickable { onUseLayout() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.onb_preview_btn_use_layout),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // İkincil Buton: "Klasörleri düzenle"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(16.dp))
                .clickable { onEditFolders() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.onb_preview_btn_edit_folders),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * UI State hesaplama fonksiyonu.
 */
internal fun buildOnboardingCategoryPreviewUiModel(
    context: Context,
    state: AppListScreenState,
    pendingApps: List<AppInfo>,
): OnboardingCategoryPreviewUiModel {
    val apps = state.apps
    val isLoading = (state.isProcessing || state.isLoading) && apps.isEmpty()

    if (isLoading) {
        return OnboardingCategoryPreviewUiModel(
            totalAppCount = 0,
            totalFolderCount = 0,
            categorizedAppCount = 0,
            pendingCount = 0,
            categories = emptyList(),
            state = OnboardingPreviewState.LOADING,
        )
    }

    if (apps.isEmpty()) {
        return OnboardingCategoryPreviewUiModel(
            totalAppCount = 0,
            totalFolderCount = 0,
            categorizedAppCount = 0,
            pendingCount = 0,
            categories = emptyList(),
            state = OnboardingPreviewState.EMPTY,
        )
    }

    // Kategorilere uygulamaları eşle
    val categoryMap = apps.groupBy { it.categoryId }
    val categorizedCount = apps.count {
        it.categoryId.isNotBlank() && it.categoryId != Category.CAT_UNCATEGORIZED
    }

    val cards = mutableListOf<OnboardingCategoryCardUiModel>()
    val allCategories = if (state.categories.isNotEmpty()) state.categories else Category.getDefaultCategories()

    for (cat in allCategories) {
        val catApps = categoryMap[cat.categoryId] ?: emptyList()
        if (catApps.isEmpty() && cat.categoryId != Category.CAT_UNCATEGORIZED) {
            continue // Uygulama içermeyen kategorileri gösterimden kaldır
        }

        val descResId = CategoryDescriptionResolver.getDescriptionResId(cat.categoryId)
        val description = context.getString(descResId)

        val pendingInCat = pendingApps.count { it.categoryId == cat.categoryId }

        cards.add(
            OnboardingCategoryCardUiModel(
                categoryId = cat.categoryId,
                title = cat.categoryName,
                description = description,
                colorHex = cat.colorHex,
                iconEmoji = cat.iconEmoji,
                appCount = catApps.size,
                previewApps = catApps.take(4),
                pendingCount = pendingInCat,
            ),
        )
    }

    // Sıralama Mantığı:
    // 1. İçinde uygulama olanlar (uygulaması çok olandan aza)
    // 2. Kategorisiz en sonda
    val sortedCards = cards.sortedWith(
        compareBy<OnboardingCategoryCardUiModel> {
            it.categoryId == Category.CAT_UNCATEGORIZED
        }.thenByDescending {
            it.appCount
        },
    )

    return OnboardingCategoryPreviewUiModel(
        totalAppCount = apps.size,
        totalFolderCount = sortedCards.size,
        categorizedAppCount = categorizedCount,
        pendingCount = pendingApps.size,
        categories = sortedCards,
        state = OnboardingPreviewState.SUCCESS,
    )
}
