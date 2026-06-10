package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category

private val SheetBg     = Color(0xFF1A1A2A)
private val TealCat     = Color(0xFF00897B)
private val RowHover    = Color.White.copy(alpha = 0.07f)
private val TextPrim    = Color.White
private val TextSec     = Color.White.copy(alpha = 0.55f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    app: AppInfo,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit,
) {
    val categories = Category.getDefaultCategories()
        .filter { it.categoryId != Category.CAT_UNCATEGORIZED }
        .sortedBy { it.displayOrder }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.2f)))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)
        ) {
            Text(
                "Kategori Seç",
                color = TextPrim,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
            )
            Text(
                app.appName,
                color = TextSec,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
            )
            Divider(color = Color.White.copy(0.08f), modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn {
                items(categories) { cat ->
                    val isCurrent = cat.categoryId == app.categoryId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(cat.categoryId); onDismiss() }
                            .background(if (isCurrent) TealCat.copy(0.12f) else Color.Transparent)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(cat.iconEmoji, fontSize = 24.sp)
                        Text(
                            cat.categoryName,
                            color = if (isCurrent) TealCat else TextPrim,
                            fontSize = 15.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCurrent) {
                            Icon(Icons.Default.Check, null, tint = TealCat, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
