package com.ibrahim.qrcodegenerator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import kotlin.math.roundToInt

@Composable
fun BuiltInColorPicker(
    modifier: Modifier = Modifier,
    initialHue: Float = 0f,
    onColorSelected: (Color) -> Unit
) {
    var hue by remember { mutableFloatStateOf(initialHue) }
    val selectedColor by remember(hue) { mutableStateOf(Color.hsv(hue, 1f, 1f)) }

    // Create a smooth hue gradient
    val hueGradient = remember {
        Brush.horizontalGradient(
            (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(hueGradient)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        hue = (offset.x / size.width).coerceIn(0f, 1f) * 360f
                        onColorSelected(Color.hsv(hue, 1f, 1f))
                    },
                    onDrag = { change, _ ->
                        hue = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                        onColorSelected(Color.hsv(hue, 1f, 1f))
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    hue = (offset.x / size.width).coerceIn(0f, 1f) * 360f
                    onColorSelected(Color.hsv(hue, 1f, 1f))
                }
            }
    ) {
        // Indicator position (always inside bounds)
        val indicatorOffset = (hue / 360f) * (maxWidth - 24.dp)

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clip(CircleShape)
                .background(selectedColor)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}
