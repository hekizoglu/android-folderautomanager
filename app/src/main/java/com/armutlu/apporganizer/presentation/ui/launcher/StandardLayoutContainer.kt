package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Responsive padding container — telefon/tablet adaptive.
 * Phone (<600dp): 16dp · Tablet (600–800dp): 24dp · Large (800+dp): 32dp
 */
@Composable
fun StandardLayoutContainer(
    modifier: Modifier = Modifier,
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp

    val horizontalPadding = when {
        widthDp < 600 -> 16.dp
        widthDp < 800 -> 24.dp
        else -> 32.dp
    }

    val verticalPadding = when {
        widthDp < 600 -> 12.dp
        else -> 16.dp
    }

    val padding = PaddingValues(
        horizontal = horizontalPadding,
        vertical = verticalPadding,
    )

    Box(
        modifier = modifier.padding(padding),
    ) {
        content(padding)
    }
}

/**
 * Responsive grid column count — screenWidth + device type.
 * <600dp: 4 cols · 600–800dp: 5 cols · 800+dp: 6 cols
 */
@Composable
fun getResponsiveGridColumns(): Int {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp

    return when {
        widthDp < 600 -> 4
        widthDp < 800 -> 5
        else -> 6
    }
}
