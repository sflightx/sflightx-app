package com.sflightx.app.layout.expressive

import android.annotation.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.*

@SuppressLint("SuspiciousModifierThen")
fun Modifier.fluid(
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMediumLow
): Modifier = this.then(
    animateContentSize(
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        )
    )
)