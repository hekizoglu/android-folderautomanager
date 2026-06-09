package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
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
    val selectionCount  = screenState.selectedApps.size
    val isSelecting     = selectionCount > 0

    var showMenu       by remember { mutableStateOf(false) }
    var showSortMenu   by remember { mutableStateOf(false) }
    var appForCategory by remember { mutableStateOf<AppInfo?>(null) }

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
                            text = { Text("Yeniden Sınıflandır") },
                            onClick = { showMenu = false; viewModel.resetAndReclassifyAllApps() },
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
        floatingActionButton = {
            if (!isSelecting) {
                FloatingActionButton(
                    onClick = { viewModel.classifyUnclassifiedApps() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.AutoFixHigh, "Otomatik sınıflandır", tint = MaterialTheme.colorScheme.onPrimary)
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
                        Icon(Icons.Default.DriveFileMove, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Kategori Değiştir")
                    }
                    if (showBulkCategory) {
                        BulkCategoryPicker(
                            categories = screenState.categories,
                            onCategorySelected = { catId ->
                                viewModel.updateAppsCategory(screenState.selectedApps.toList(), catId)
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
                items(screenState.categories.filter { it.categoryId != Category.CAT_UNCATEGORIZED }) { cat ->
                    val cnt = screenState.apps.count { it.categoryId == cat.categoryId }
                    if (cnt > 0) {
                        CategoryChip(
                            label = cat.categoryName,
                            emoji = cat.iconEmoji,
                            count = cnt,
                            selected = selectedCategory == cat.categoryId,
                            onClick = { viewModel.setSelectedCategory(cat.categoryId) }
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                    selectedApps = screenState.selectedApps,
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
}

// ── Kategori chip ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryChip(
    label: String,
    emoji: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(emoji, fontSize = 14.sp)
                Text(label, fontSize = 13.sp)
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        count.toString(),
                        fontSize = 11.sp,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

// ── App listesi içeriği ────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppListContent(
    apps: List<AppInfo>,
    selectedApps: Set<String>,
    categories: List<Category>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            val isSelected = app.packageName in selectedApps
            val cat = categories.find { it.categoryId == app.categoryId }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isSelected) Modifier.border(
                            1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)
                        ) else Modifier
                    )
                    .combinedClickable(
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) }
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // App ikonu
                    val px = (48 * context.resources.displayMetrics.density).toInt()
                    val icon = remember(app.packageName) {
                        runCatching {
                            context.packageManager.getApplicationIcon(app.packageName)
                                .toBitmap(px, px).asImageBitmap()
                        }.getOrNull()
                    }
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            androidx.compose.foundation.Image(
                                bitmap = icon,
                                contentDescription = app.appName,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Text(app.appName.take(1), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }

                    // İsim + kategori
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            app.appName,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (cat != null) {
                            Text(
                                "${cat.iconEmoji} ${cat.categoryName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Seçim işareti
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
