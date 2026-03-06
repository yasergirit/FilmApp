package com.filmapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmapp.presentation.theme.GlassBorder
import com.filmapp.presentation.theme.GlassWhite

/**
 * A Glassmorphism surface — translucent card with blur backdrop and subtle border.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .blur(blurRadius)
            .background(GlassWhite)
            .border(width = 1.dp, color = GlassBorder, shape = shape)
    ) {
        content()
    }
}

/**
 * Simplified glass overlay without blur (for use on top of images).
 */
@Composable
fun GlassOverlay(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(GlassWhite)
            .border(width = 0.5.dp, color = GlassBorder, shape = shape)
    ) {
        content()
    }
}
