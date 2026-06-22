package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContextMenuSheet(
    folder: AppFolder,
    allFolders: List<AppFolder> = emptyList(),
    onDismiss: () -> Unit,
    onOpenFolder: () -> Unit,
    onOpenAllApps: () -> Unit,
    onMove: ((newIndex: Int) -> Unit)? = null,
) {
    val catColor = runCatching {
        Color(android.graphics.Color.parseColor(folder.category.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)
    val primary   = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface   = MaterialTheme.colorScheme.surface

    var showMoveDialog by remember { mutableStateOf(false) }
    var selectedMoveIndex by remember { mutableStateOf(-1) }
    val currentIndex = allFolders.indexOfFirst { it.category.categoryId == folder.category.categoryId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(onSurface.copy(0.2f)))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(catColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) { Text(folder.category.iconEmoji, fontSize = 22.sp) }
                Column {
                    Text(folder.category.categoryName, color = onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${folder.apps.size} uygulama${if (currentIndex >= 0) " · ${currentIndex + 1}. sıra" else ""}", color = onSurface.copy(0.55f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(onSurface.copy(0.08f)))
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFolder)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FolderOpen, null, tint = primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.folder_open), color = onSurface, fontSize = 15.sp)
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(onSurface.copy(0.08f)))
            if (allFolders.size > 1 && onMove != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showMoveDialog = true }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, null, tint = onSurface.copy(0.7f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.folder_move), color = onSurface, fontSize = 15.sp)
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(onSurface.copy(0.08f)))
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAllApps)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Apps, null, tint = onSurface.copy(0.7f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.folder_goto_all_apps), color = onSurface, fontSize = 15.sp)
            }
        }
    }

    if (showMoveDialog) {
        FolderPositionPickerSheet(
            allFolders = allFolders,
            currentIndex = currentIndex,
            selectedIndex = selectedMoveIndex,
            onSelectIndex = { selectedMoveIndex = it },
            onConfirm = {
                if (selectedMoveIndex >= 0 && selectedMoveIndex != currentIndex) {
                    onMove?.invoke(selectedMoveIndex)
                    showMoveDialog = false
                    selectedMoveIndex = -1
                    onDismiss()
                }
            },
            onDismiss = { showMoveDialog = false; selectedMoveIndex = -1 }
        )
    }
}
