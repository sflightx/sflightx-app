package com.sflightx.app.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    durationMillis: Int = 4000, // Duration for the animation
    content: @Composable BoxScope.() -> Unit  // Content to overlay on the background
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

// Blend each with transparency to create depth
    val color1Start = primaryColor.copy(alpha = 0.3f)
    val color1End = secondaryColor.copy(alpha = 0.15f)
    val color2Start = tertiaryColor.copy(alpha = 0.05f)
    val color2End = tertiaryColor.copy(alpha = 0.25f)


    // Infinite transition to animate the gradient
    val transition = rememberInfiniteTransition(label = "GradientTransition")

    val color1 by transition.animateColor(
        initialValue = color1Start,
        targetValue = color1End,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Color1"
    )

    val color2 by transition.animateColor(
        initialValue = color2Start,
        targetValue = color2End,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Color2"
    )

    val color3 = primaryColor.copy(alpha = 0.1f)

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to color1,
                        0.5f to color3,
                        1.0f to color2
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(600f, 600f)
                )

            )
            .fillMaxSize()
    ) {
        content()
    }
}
