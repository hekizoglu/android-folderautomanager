package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import timber.log.Timber

/**
 * CategoryEditorScreen - Manage categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val screenState by viewModel.screenState.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kategoriler") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, "Kategori ekle")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Categories List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(screenState.categories, key = { it.categoryId }) { category ->
                    CategoryItem(
                        category = category,
                        appCount = screenState.countAppsByCategory(category.categoryId),
                        onEdit = {
                            Timber.d("Edit: ${category.categoryName}")
                        }
                    )
                }
            }
        }
    }
    
    // Add category dialog
    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, _ ->
                Timber.d("Add category: $name")
                showAddDialog = false
            }
        )
    }
}

/**
 * Category item in the list
 */
@Composable
fun CategoryItem(
    category: Category,
    appCount: Int,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${category.iconEmoji} ${category.categoryName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$appCount uygulama",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Color indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Color(android.graphics.Color.parseColor(category.colorHex)),
                        shape = MaterialTheme.shapes.small
                    )
            )
            
            // Edit button
            if (!category.isSystemCategory) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Düzenle")
                }
            }
        }
    }
}

/**
 * Dialog for adding new category
 */
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📁") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kategori Ekle") },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Kategori adı") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                Text("Emoji seç:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("📁", "🎮", "🎨", "🎵", "🎬").forEach { emoji ->
                        Button(
                            onClick = { selectedEmoji = emoji },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(emoji)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onAdd(categoryName, selectedEmoji)
                    }
                }
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
