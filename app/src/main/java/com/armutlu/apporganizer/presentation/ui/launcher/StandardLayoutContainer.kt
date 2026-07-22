package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun StandardLayoutContainer(
    modifier: Modifier = Modifier,
    content: @Composable (contentModifier: Modifier) -> Unit
) {
    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600
    val horizontalPadding = if (isTablet) 24.dp else 16.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
        ) {
            content(Modifier.fillMaxSize())
        }
    }
}
