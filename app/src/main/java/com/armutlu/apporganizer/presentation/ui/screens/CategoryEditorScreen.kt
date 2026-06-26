package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val screenState by viewModel.screenState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kategoriler") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Kategori ekle")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = screenState.categories, key = { category -> category.categoryId }) { category ->
                CategoryItem(
                    category = category,
                    appCount = screenState.countAppsByCategory(category.categoryId),
                    onEdit = { editingCategory = category },
                    onDelete = { viewModel.deleteCategory(category) }
                )
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            title = "Kategori Ekle",
            confirmLabel = "Ekle",
            initialName = "",
            initialEmoji = "\uD83D\uDCC1",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, emoji ->
                viewModel.addCategory(name, emoji)
                showAddDialog = false
            }
        )
    }

    LaunchedEffect(screenState.error) {
        if (screenState.error != null) {
            viewModel.clearError()
        }
    }

    editingCategory?.let { category ->
        CategoryDialog(
            title = "Kategori Düzenle",
            confirmLabel = "Kaydet",
            initialName = category.categoryName,
            initialEmoji = category.iconEmoji,
            onDismiss = { editingCategory = null },
            onConfirm = { name, emoji ->
                viewModel.updateCategory(category.copy(categoryName = name, iconEmoji = emoji))
                editingCategory = null
            }
        )
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    appCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
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

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Color(android.graphics.Color.parseColor(category.colorHex)),
                        shape = MaterialTheme.shapes.small
                    )
            )

            if (!category.isSystemCategory) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil")
                }
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialEmoji: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var categoryName by remember(initialName) { mutableStateOf(initialName) }
    var selectedEmoji by remember(initialEmoji) { mutableStateOf(initialEmoji) }
    val trimmedName = categoryName.trim()
    val showError = categoryName.isNotEmpty() && trimmedName.isBlank()
    val emojiOptions = listOf("\uD83D\uDCC1", "\uD83C\uDFAE", "\uD83C\uDFA8", "\uD83C\uDFB5", "\uD83C\uDFAC")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Kategori adı") },
                    isError = showError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                if (showError) {
                    Text(
                        text = "Kategori adı boş olamaz",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text("Emoji seç:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojiOptions.forEach { emoji ->
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
                enabled = trimmedName.isNotEmpty(),
                onClick = { onConfirm(trimmedName, selectedEmoji) }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
