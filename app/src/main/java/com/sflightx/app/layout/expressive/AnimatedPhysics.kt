package com.sflightx.app.layout.expressive

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*

/**
 * Expressive motion physics from M3 guidelines.
 */
object ExpressiveSpring {
    val Bouncy = SpringSpec<Float>(dampingRatio = 0.35f, stiffness = 300f)
    val Expressive = SpringSpec<Float>(dampingRatio = 0.4f, stiffness = 300f)
    val Standard = SpringSpec<Float>(dampingRatio = 1f, stiffness = 300f)
}

/**
 * Animate any float property with expressive physics.
 */
fun Modifier.expressiveAnimatedFloat(
    target: Float,
    spring: SpringSpec<Float> = ExpressiveSpring.Expressive,
    onUpdate: (Float) -> Modifier = { scale(it) }, // Default: scale
): Modifier = composed {
    val animated by animateFloatAsState(target, animationSpec = spring, label = "ExpressiveAnim")
    this.then(onUpdate(animated))
}

/**
 * Shrink (scale) with expressive physics on press.
 */
fun Modifier.expressiveShrink(
    scaleDown: Float = 0.93f,
    spring: SpringSpec<Float> = ExpressiveSpring.Expressive,
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    expressiveAnimatedFloat(
        target = if (pressed) scaleDown else 1f,
        spring = spring,
        onUpdate = { scale -> scale(scale) }
    )
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                }
            )
        }
}

/**
 * Animate translationX with expressive physics (for grouped bounce, etc).
 */
fun Modifier.expressiveTranslateX(
    offsetX: Float,
    spring: SpringSpec<Float> = ExpressiveSpring.Expressive,
): Modifier = expressiveAnimatedFloat(
    target = offsetX,
    spring = spring,
    onUpdate = { x -> graphicsLayer { translationX = x } }
)

/**
 * Animate both scale and translation (for grouped effects).
 */
fun Modifier.expressiveScaleAndTranslate(
    scale: Float,
    offsetX: Float,
    spring: SpringSpec<Float> = ExpressiveSpring.Expressive,
): Modifier = composed {
    val scaleAnim by animateFloatAsState(scale, animationSpec = spring, label = "ExpressiveScale")
    val xAnim by animateFloatAsState(offsetX, animationSpec = spring, label = "ExpressiveX")
    this.graphicsLayer {
        this.scaleX = scaleAnim
        this.scaleY = scaleAnim
        this.translationX = xAnim
    }
}
