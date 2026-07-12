package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val screenState     by viewModel.screenState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery     by viewModel.searchQuery.collectAsState()
    val sortOption      by viewModel.sortOption.collectAsState()
    val classifyLoading by viewModel.classifyLoading.collectAsState()
    val classifyResult  by viewModel.classifyResult.collectAsState()
    val selectedApps    by viewModel.selectedApps.collectAsState()
    val suggestedSimilarApps by viewModel.suggestedSimilarApps.collectAsState()
    val suggestedSimilarCategoryId by viewModel.suggestedSimilarCategoryId.collectAsState()
    val selectionCount  = selectedApps.size
    val isSelecting     = selectionCount > 0
    val hasUnclassifiedApps = screenState.apps.any { it.categoryId == Category.CAT_UNCATEGORIZED }
    val snackbarHostState = remember { SnackbarHostState() }

    var showMenu       by remember { mutableStateOf(false) }
    var showSortMenu   by remember { mutableStateOf(false) }
    var appForCategory by remember { mutableStateOf<AppInfo?>(null) }
    var showResetCategoriesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(classifyResult) {
        if (classifyResult.isNotBlank()) {
            snackbarHostState.showSnackbar(classifyResult)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (isSelecting)
                        Text("$selectionCount seçili", fontWeight = FontWeight.SemiBold)
                    else
                        Text("App Organizer", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    if (isSelecting) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, "Seçimi temizle")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.SortByAlpha, "Sırala")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        SortOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = { viewModel.setSortOption(opt); showSortMenu = false },
                                leadingIcon = {
                                    if (sortOption == opt)
                                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, "Menü")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Kategoriler") },
                            onClick = { showMenu = false; onNavigateToCategories() },
                            leadingIcon = { Icon(Icons.Default.Category, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Kategorileri Sıfırla ve Yeniden Sınıflandır") },
                            onClick = { showMenu = false; showResetCategoriesDialog = true },
                            leadingIcon = { Icon(Icons.Default.RestartAlt, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Ayarlar") },
                            onClick = { showMenu = false; onNavigateToSettings() },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isSelecting)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isSelecting && hasUnclassifiedApps) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!classifyLoading) viewModel.classifyUnclassifiedApps()
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    if (classifyLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.AutoFixHigh, "Otomatik sınıflandır", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (classifyLoading) "Sınıflandırılıyor" else "Sınıflandır",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelecting,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                var showBulkCategory by remember { mutableStateOf(false) }
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    TextButton(onClick = { viewModel.selectAllVisibleApps() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.SelectAll, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Tümünü Seç")
                    }
                    TextButton(onClick = { showBulkCategory = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Kategori Değiştir")
                    }
                    if (showBulkCategory) {
                        BulkCategoryPicker(
                            categories = screenState.categories,
                            onCategorySelected = { catId ->
                                viewModel.updateAppsCategory(selectedApps.toList(), catId)
                                showBulkCategory = false
                            },
                            onDismiss = { showBulkCategory = false }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Arama çubuğu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Uygulama ara...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Close, null)
                        }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Kategori sekmeleri
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                item {
                    CategoryChip(
                        label = "Tümü",
                        emoji = "📱",
                        count = screenState.apps.size,
                        selected = selectedCategory == "all",
                        onClick = { viewModel.setSelectedCategory("all") }
                    )
                }
                items(screenState.visibleCategories, key = { it.categoryId }) { cat ->
                    CategoryChip(
                        label = cat.categoryName,
                        emoji = cat.iconEmoji,
                        count = screenState.countAppsByCategory(cat.categoryId),
                        selected = selectedCategory == cat.categoryId,
                        onClick = { viewModel.setSelectedCategory(cat.categoryId) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // İçerik
            when {
                screenState.isProcessing -> LoadingSkeleton()
                screenState.error != null -> AppEmptyState(
                    icon = Icons.Default.ErrorOutline,
                    title = "Hata oluştu",
                    subtitle = screenState.error ?: ""
                )
                screenState.filteredApps.isEmpty() && searchQuery.isNotBlank() -> AppEmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "Sonuç bulunamadı",
                    subtitle = "\"$searchQuery\" için eşleşen uygulama yok"
                )
                screenState.filteredApps.isEmpty() -> AppEmptyState(
                    icon = Icons.Default.Apps,
                    title = "Uygulama yok",
                    subtitle = "Bu kategoride henüz uygulama bulunmuyor"
                )
                else -> AppListContent(
                    apps = screenState.filteredApps,
                    selectedApps = selectedApps,
                    categories = screenState.categories,
                    onAppClick = { app ->
                        if (isSelecting) viewModel.toggleAppSelection(app.packageName)
                        else viewModel.launchApp(app.packageName)
                    },
                    onAppLongClick = { app ->
                        if (isSelecting) viewModel.toggleAppSelection(app.packageName)
                        else appForCategory = app
                    }
                )
            }
        }
    }

    // Kategori değiştir dialog
    appForCategory?.let { app ->
        CategoryPickerDialog(
            app = app,
            categories = screenState.categories,
            onCategorySelected = { catId ->
                viewModel.updateAppCategory(app.packageName, catId)
                appForCategory = null
            },
            onDismiss = { appForCategory = null }
        )
    }

    if (suggestedSimilarApps.isNotEmpty() && suggestedSimilarCategoryId != null) {
        val categoryName = screenState.categories
            .firstOrNull { it.categoryId == suggestedSimilarCategoryId }
            ?.categoryName
            ?: "seçilen kategori"
        AlertDialog(
            onDismissRequest = { viewModel.clearSimilarCategorySuggestions() },
            title = { Text("Benzer uygulamalar bulundu") },
            text = {
                Text(
                    suggestedSimilarApps
                        .take(4)
                        .joinToString(
                            separator = "\n",
                            prefix = "$categoryName için bunları da taşıyalım mı?\n\n",
                        ) { "• ${it.appName}" }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.acceptSimilarCategorySuggestions() }) {
                    Text("Taşı")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearSimilarCategorySuggestions() }) {
                    Text("Atla")
                }
            }
        )
    }

    if (showResetCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showResetCategoriesDialog = false },
            title = { Text("Kategorileri sıfırla ve yeniden sınıflandır") },
            text = { Text("Tüm mevcut kategori atamaları silinecek ve uygulamalar yeniden sınıflandırılacak. Bu işlem geri alınamaz. Devam etmek istiyor musunuz?") },
            confirmButton = {
                TextButton(onClick = {
                    showResetCategoriesDialog = false
                    viewModel.resetAndReclassifyAllApps()
                }) {
                    Text("Sıfırla", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetCategoriesDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

