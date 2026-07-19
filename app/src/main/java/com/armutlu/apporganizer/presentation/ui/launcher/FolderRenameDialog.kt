package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.armutlu.apporganizer.domain.usecase.missions.FolderEmojiSets
import com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem

internal val EMOJI_PICKER = listOf(
    "📁","📝","🎮","👥","🛍️","📰","❤️","💰","🎓","🔧",
    "✈️","🎬","🍔","📸","📦","⭐","🏠","🎵","💼","🎯",
    "🔑","📱","💻","🎁","🌟","🚀","🎪","🏆","💡","📚",
    "🌙","☀️","🎨","🏋️","🐶","🌿","🔔","💬","🗓️","🧩"
)

internal val COLOR_PRESETS = listOf(
    "" to "Varsayılan",
    "#00897B" to "Turkuaz",
    "#1976D2" to "Mavi",
    "#7B1FA2" to "Mor",
    "#D32F2F" to "Kırmızı",
    "#F57C00" to "Turuncu",
    "#388E3C" to "Yeşil",
    "#C2185B" to "Pembe",
    "#FBC02D" to "Sarı",
    "#303F9F" to "Lacivert",
)

@Composable
internal fun FolderRenameDialog(
    currentName: String,
    currentEmoji: String,
    currentColor: String = "",
    // Dongu G6 — Yildiz Ekonomisi kozmetik acilim: kilit durumu icin toplam ⭐. Varsayilan 0
    // (cagiran taraf gecmediyse) TUM yeni setleri kilitli gosterir — GUVENLI varsayilan, mevcut
    // EMOJI_PICKER hicbir sekilde etkilenmez (kirmizi cizgi).
    totalStars: Int = 0,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, color: String) -> Unit,
) {
    var nameField by remember { mutableStateOf(currentName) }
    var selectedEmoji by remember { mutableStateOf(currentEmoji) }
    var selectedColor by remember { mutableStateOf(currentColor) }
    val trimmedName = nameField.trim()
    val showNameError = nameField.isNotEmpty() && trimmedName.isBlank()
    val primary   = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface   = MaterialTheme.colorScheme.surface

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surface,
        title = {
            Text(stringResource(R.string.folder_edit), color = onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nameField,
                    onValueChange = { nameField = it },
                    label = { Text(stringResource(R.string.folder_rename_hint), color = onSurface.copy(0.6f)) },
                    singleLine = true,
                    isError = showNameError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = onSurface.copy(0.25f),
                        focusedTextColor = onSurface,
                        unfocusedTextColor = onSurface,
                        cursorColor = primary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showNameError) {
                    Text(
                        text = "Klasör adı boş bırakılamaz",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
                Text(stringResource(R.string.folder_emoji_pick), color = onSurface.copy(0.6f), fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(EMOJI_PICKER, key = { _, emoji -> emoji }) { _, emoji ->
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (emoji == selectedEmoji) primary.copy(0.35f) else onSurface.copy(0.08f))
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) { Text(emoji, fontSize = 20.sp) }
                    }
                }
                // Dongu G6 — kozmetik acilim: YENI emoji setleri (mevcut EMOJI_PICKER'a dokunulmadi).
                // Kilitliyse tiklayinca "X seviyesinde acilir (N ⭐)" bilgisi gosterilir, secim yapilmaz.
                var lockInfoMessage by remember { mutableStateOf<String?>(null) }
                FolderEmojiSets.SETS.forEach { set ->
                    val unlocked = FolderEmojiSets.isUnlocked(set, totalStars)
                    val setName = stringResource(set.nameRes)
                    Text(
                        text = if (unlocked) setName else "$setName 🔒",
                        color = onSurface.copy(0.6f),
                        fontSize = 13.sp,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(set.emojis, key = { _, emoji -> "${set.id}_$emoji" }) { _, emoji ->
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            !unlocked -> onSurface.copy(0.04f)
                                            emoji == selectedEmoji -> primary.copy(0.35f)
                                            else -> onSurface.copy(0.08f)
                                        }
                                    )
                                    .clickable {
                                        if (unlocked) {
                                            selectedEmoji = emoji
                                        } else {
                                            val (level, threshold) = FolderEmojiSets.lockInfo(set)
                                            lockInfoMessage = level.labelTr + "|" + threshold
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 20.sp, color = if (unlocked) Color.Unspecified else onSurface.copy(0.35f))
                            }
                        }
                    }
                }
                lockInfoMessage?.let { raw ->
                    val (levelName, threshold) = raw.split("|").let { it[0] to it[1].toInt() }
                    Text(
                        text = stringResource(R.string.folder_emoji_set_locked_hint, levelName, threshold),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                    )
                }
                Text(stringResource(R.string.folder_color_pick), color = onSurface.copy(0.6f), fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(COLOR_PRESETS, key = { _, preset -> preset.first }) { _, preset ->
                        val hex = preset.first
                        val isSelected = selectedColor == hex
                        val resolvedColor = if (hex.isBlank()) onSurface.copy(0.2f)
                            else runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(onSurface)
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(resolvedColor)
                                .then(if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = trimmedName.isNotEmpty(),
                onClick = { onSave(trimmedName, selectedEmoji, selectedColor) }
            ) {
                Text(stringResource(R.string.btn_save), color = primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = onSurface.copy(0.6f))
            }
        }
    )
}
