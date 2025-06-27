package com.sflightx.app.layout.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*

enum class M3ButtonType {
    Filled, FilledTonal, Outlined, Elevated, Text
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3Button(
    modifier: Modifier = Modifier,
    iconResId: Int? = null,
    contentDescription: String,
    text: String? = null,
    enabled: Boolean? = true,
    onClick: () -> Unit,
    buttonType: M3ButtonType = M3ButtonType.FilledTonal
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ), label = "scale"
    )

    // Animate corner radius with spring for a morphing shape effect
    val cornerRadius by animateFloatAsState(
        targetValue = if (isPressed) 24f else 16f,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ), label = "cornerRadius"
    )

    // Animate color with spring for a dynamic color change
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.primary,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ), label = "color"
    )

    // Select button composable based on buttonType
    when (buttonType) {
        M3ButtonType.Filled -> Button(
            onClick = onClick,
            modifier = modifier
                .scale(scale)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(cornerRadius.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            interactionSource = interactionSource
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            if (text != null) {
                Text(text, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        M3ButtonType.FilledTonal -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier
                .scale(scale)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(cornerRadius.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            interactionSource = interactionSource
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            if (text != null) {
                Text(text, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        M3ButtonType.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .scale(scale)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(cornerRadius.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isPressed) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.outline
            ),
            interactionSource = interactionSource
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            if (text != null) {
                Text(text, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        M3ButtonType.Elevated -> ElevatedButton(
            onClick = onClick,
            modifier = modifier
                .scale(scale)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(cornerRadius.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = ButtonDefaults.elevatedButtonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            ),
            interactionSource = interactionSource
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            if (text != null) {
                Text(text)
            }
        }
        M3ButtonType.Text -> TextButton(
            onClick = onClick,
            modifier = modifier
                .scale(scale)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(cornerRadius.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isPressed) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.primary
            ),
            interactionSource = interactionSource
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            if (text != null) {
                Text(text)
            }
        }
    }
}