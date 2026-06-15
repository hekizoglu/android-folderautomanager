package com.armutlu.apporganizer.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.*

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val controller = rememberColorPickerController()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renk Seç") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HsvColorPicker(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    controller = controller,
                    initialColor = initialColor
                )
                Spacer(Modifier.height(8.dp))
                BrightnessSlider(
                    modifier = Modifier.fillMaxWidth().height(35.dp),
                    controller = controller
                )
                Spacer(Modifier.height(8.dp))
                AlphaSlider(
                    modifier = Modifier.fillMaxWidth().height(35.dp),
                    controller = controller
                )
                Spacer(Modifier.height(8.dp))
                AlphaTile(
                    modifier = Modifier.fillMaxWidth().height(35.dp),
                    controller = controller
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorSelected(controller.selectedColor.value)
                onDismiss()
            }) { Text("Uygula") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
