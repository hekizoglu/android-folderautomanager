package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val screenState    by viewModel.screenState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery    by viewModel.searchQuery.collectAsState()
    val sortOption     by viewModel.sortOption.collectAsState()
    val selectionCount = screenState.selectedApps.size

    var showMenu       by remember { mutableStateOf(false) }
    var showSortMenu   by remember { mutableStateOf(false) }
    var appForCategory by remember { mutableStateOf<AppInfo?>(null) }
    val isSelecting    = selectionCount > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelecting)
                        Text("$selectionCount seçili", fontWeight = FontWeight.Bold)
                    else
                        Text("App Organizer")
                },
                navigationIcon = {
                    if (isSelecting) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, "Seçimi temizle")
                        }
                    }
                },
                actions = {
                    // Sıralama butonu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.SortByAlpha, "Sırala")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = { viewModel.setSortOption(option); showSortMenu = false },
                                    leadingIcon = {
                                        if (sortOption == option)
                                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                )
                            }
                        }
                    }
                    // Menü
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
                var showOrganizeDialog by remember { mutableStateOf(false) }
                val organizeState by viewModel.organizeState.collectAsState()

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Launcher'da grupla
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.resetOrganizeState(); showOrganizeDialog = true },
                        icon = { Icon(Icons.Default.GridView, null) },
                        text = { Text("Launcher'da Grupla") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    // Otomatik sınıflandır
                    FloatingActionButton(onClick = { viewModel.classifyUnclassifiedApps() }) {
                        Icon(Icons.Default.AutoFixHigh, "Otomatik sınıflandır")
                    }
                }

                if (showOrganizeDialog) {
                    val a11yConnected = com.armutlu.apporganizer.service.LauncherAccessibilityService.instance != null
                    val a11yInSystem  = viewModel.isAccessibilityServiceEnabledInSystem()
                    LauncherOrganizeDialog(
                        launcherType    = viewModel.detectedLauncher,
                        organizeState   = organizeState,
                        a11yConnected   = a11yConnected,
                        a11yInSystem    = a11yInSystem,
                        onOrganize      = { useAccessibility -> viewModel.organizeOnLauncher(useAccessibility) },
                        onOpenA11ySettings = {
                            showOrganizeDialog = false
                            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            viewModel.launchIntent(intent)
                        },
                        onRestart = { viewModel.resetOrganizeState() },
                        onDismiss = { showOrganizeDialog = false }
                    )
                }
            }
        },
        // Toplu işlem alt bar
        bottomBar = {
            AnimatedVisibility(
                visible = isSelecting,
                enter = slideInVertically { it },
                exit  = slideOutVertically { it }
            ) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    TextButton(
                        onClick = { viewModel.selectAllVisibleApps() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SelectAll, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Tümünü Seç")
                    }
                    var showBulkCategory by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { showBulkCategory = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DriveFileMove, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Kategori Değiştir")
                    }
                    if (showBulkCategory) {
                        BulkCategoryPicker(
                            categories = screenState.categories,
                            onCategorySelected = { catId ->
                                viewModel.updateAppsCategory(
                                    screenState.selectedApps.toList(), catId
                                )
                                showBulkCategory = false
                            },
                            onDismiss = { showBulkCategory = false }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                onClear = { viewModel.clearSearch() }
            )
            CategoryTabs(
                categories    = screenState.categories,
                appCounts     = screenState.apps.groupBy { it.categoryId }.mapValues { it.value.size },
                totalCount    = screenState.apps.size,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.setSelectedCategory(it) }
            )

            when {
                screenState.isProcessing -> LoadingSkeleton()

                screenState.error != null -> EmptyState(
                    icon    = Icons.Default.ErrorOutline,
                    title   = "Hata oluştu",
                    subtitle = screenState.error ?: ""
                )

                screenState.filteredApps.isEmpty() && searchQuery.isNotBlank() -> EmptyState(
                    icon     = Icons.Default.SearchOff,
                    title    = "Sonuç bulunamadı",
                    subtitle = "\"$searchQuery\" için eşleşen uygulama yok"
                )

                screenState.filteredApps.isEmpty() -> EmptyState(
                    icon     = Icons.Default.Apps,
                    title    = "Uygulama yok",
                    subtitle = "Bu kategoride henüz uygulama bulunmuyor"
                )

                else -> AppListContent(
                    apps         = screenState.filteredApps,
                    selectedApps = screenState.selectedApps,
                    categories   = screenState.categories,
                    onAppClick   = { app ->
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

    appForCategory?.let { app ->
        CategoryPickerSheet(
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

// ── Search bar ──────────────────────────────────────────────────────────────

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Uygulama ara...") },
        leadingIcon  = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Default.Clear, null) }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

// ── Category tabs ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTabs(
    categories: List<Category>,
    appCounts: Map<String, Int>,
    totalCount: Int,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            CategoryChip(
                label    = "Tümü",
                emoji    = "📱",
                count    = totalCount,
                isSelected = selectedCategory == "all",
                onClick  = { onCategorySelected("all") }
            )
        }
        items(categories) { cat ->
            CategoryChip(
                label    = cat.categoryName,
                emoji    = cat.iconEmoji,
                count    = appCounts[cat.categoryId] ?: 0,
                isSelected = selectedCategory == cat.categoryId,
                onClick  = { onCategorySelected(cat.categoryId) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(label: String, emoji: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick  = onClick,
        label    = { Text("$emoji $label  $count", fontSize = 13.sp) },
        modifier = Modifier.height(36.dp)
    )
}

// ── App list ────────────────────────────────────────────────────────────────

@Composable
fun AppListContent(
    apps: List<AppInfo>,
    selectedApps: Set<String>,
    categories: List<Category>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    val catMap = remember(categories) { categories.associateBy { it.categoryId } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppListItem(
                app        = app,
                category   = catMap[app.categoryId],
                isSelected = selectedApps.contains(app.packageName),
                onClick    = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppInfo,
    category: Category?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    // Kategori rengi
    val catColor = remember(category?.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(category?.colorHex ?: "#9E9E9E")) }
            .getOrDefault(Color.Gray)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Seçim / ikon
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, null, tint = Color.White)
                        }
                    }
                } else if (icon != null) {
                    androidx.compose.foundation.Image(
                        bitmap = icon,
                        contentDescription = app.appName,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(app.appName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Kategori rozeti
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(catColor)
                    )
                    Text(
                        text  = category?.let { "${it.iconEmoji} ${it.categoryName}" } ?: app.categoryId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

// ── Empty / loading states ──────────────────────────────────────────────────

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LoadingSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(12) {
            Card(modifier = Modifier.fillMaxWidth().height(68.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)))
                        Box(modifier = Modifier.width(80.dp).height(10.dp).clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)))
                    }
                }
            }
        }
    }
}

// ── Category picker sheets ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    app: AppInfo,
    categories: List<Category>,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)) {
                if (icon != null) {
                    androidx.compose.foundation.Image(bitmap = icon, contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                }
                Column {
                    Text(app.appName, style = MaterialTheme.typography.titleMedium)
                    Text("Kategori seç", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Divider()
            Spacer(Modifier.height(4.dp))
            CategoryList(
                categories = categories,
                currentCategoryId = app.categoryId,
                onCategorySelected = onCategorySelected
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkCategoryPicker(
    categories: List<Category>,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Seçili uygulamalar için kategori seç",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp))
            Divider()
            Spacer(Modifier.height(4.dp))
            CategoryList(categories = categories, currentCategoryId = null,
                onCategorySelected = onCategorySelected)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<Category>,
    currentCategoryId: String?,
    onCategorySelected: (String) -> Unit
) {
    categories.forEach { category ->
        val isCurrent = category.categoryId == currentCategoryId
        val catColor = runCatching {
            Color(android.graphics.Color.parseColor(category.colorHex))
        }.getOrDefault(Color.Gray)

        Surface(
            onClick = { onCategorySelected(category.categoryId) },
            shape   = RoundedCornerShape(8.dp),
            color   = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                      else            MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(50))
                    .background(catColor))
                Text("${category.iconEmoji} ${category.categoryName}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge)
                if (isCurrent) Icon(Icons.Default.Check, null,
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

// ── Launcher organize dialog ────────────────────────────────────────────────

@Composable
fun LauncherOrganizeDialog(
    launcherType: com.armutlu.apporganizer.utils.LauncherType,
    organizeState: OrganizeState,
    a11yConnected: Boolean,
    a11yInSystem: Boolean,
    onOrganize: (useAccessibility: Boolean) -> Unit,
    onOpenA11ySettings: () -> Unit,
    onRestart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (organizeState !is OrganizeState.Running) onDismiss() },
        icon = { Icon(Icons.Default.GridView, null) },
        title = { Text("Launcher'da Grupla") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (organizeState) {
                    is OrganizeState.Idle -> {
                        // Launcher bilgisi
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (launcherType.supportsAccessibility) Icons.Default.CheckCircle
                                    else Icons.Default.Info,
                                    null,
                                    tint = if (launcherType.supportsAccessibility)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text("Launcher: ${launcherType.displayName}",
                                        style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        if (launcherType.supportsAccessibility)
                                            "Fiziksel drag & drop destekleniyor"
                                        else
                                            "Shortcut pinleme kullanılacak",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (launcherType.supportsAccessibility) {
                            when {
                                a11yConnected -> {
                                    // Bağlı — hiçbir uyarı gösterme
                                }
                                a11yInSystem -> {
                                    // Ayarlarda var ama instance null — APK güncellendi, yeniden başlatma gerekiyor
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Refresh, null,
                                                tint = MaterialTheme.colorScheme.tertiary)
                                            Column {
                                                Text("Servis yeniden başlatılmalı",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                                                Text(
                                                    "APK güncellendikten sonra erişilebilirlik servisi bağlantısı kopuyor.\n" +
                                                    "Ayarlardan 'App Organizer'ı KAPAT → tekrar AÇ.",
                                                    style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // Hiç etkin değil
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, null,
                                                tint = MaterialTheme.colorScheme.error)
                                            Column {
                                                Text("Erişilebilirlik izni gerekli",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.error)
                                                Text(
                                                    "Drag & drop için:\nAyarlar → Erişilebilirlik → Yüklü uygulamalar → App Organizer → Aç",
                                                    style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            "Kategorilere göre uygulamalar ana ekranda gruplandırılacak.\n\n" +
                            "• Shortcut yöntemi: her kategoriye kısayol ekler\n" +
                            "• Drag & drop yöntemi: ikonları fiziksel olarak taşır (izin gerekir)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is OrganizeState.Running -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(organizeState.status, style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }

                    is OrganizeState.Done -> {
                        Surface(shape = RoundedCornerShape(8.dp),
                            color = if (organizeState.success)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer) {
                            Text(organizeState.message,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (organizeState) {
                is OrganizeState.Idle -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (launcherType.supportsAccessibility && !a11yConnected) {
                            Button(onClick = onOpenA11ySettings, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Settings, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (a11yInSystem) "Erişilebilirlik Ayarlarını Aç (Yeniden Başlat)" else "Erişilebilirlik Ayarlarını Aç")
                            }
                        }
                        if (a11yConnected) {
                            Button(onClick = { onOrganize(true) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.DragIndicator, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Drag & Drop ile Grupla")
                            }
                        }
                        OutlinedButton(onClick = { onOrganize(false) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AddLink, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Kısayol ile Grupla (Evrensel)")
                        }
                    }
                }
                is OrganizeState.Done -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tekrar Organize Et")
                        }
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("Kapat")
                        }
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            if (organizeState is OrganizeState.Idle) {
                TextButton(onClick = onDismiss) { Text("Vazgeç") }
            }
        }
    )
}

sealed class OrganizeState {
    object Idle : OrganizeState()
    data class Running(val status: String) : OrganizeState()
    data class Done(val success: Boolean, val message: String) : OrganizeState()
}
