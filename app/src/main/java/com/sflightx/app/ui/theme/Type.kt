package com.sflightx.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val defaultTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = MyAppFontFamily),
        displayMedium = displayMedium.copy(fontFamily = MyAppFontFamily),
        displaySmall = displaySmall.copy(fontFamily = MyAppFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = MyAppFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = MyAppFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = MyAppFontFamily),
        titleLarge = titleLarge.copy(fontFamily = MyAppFontFamily),
        titleMedium = titleMedium.copy(fontFamily = MyAppFontFamily),
        titleSmall = titleSmall.copy(fontFamily = MyAppFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = MyAppFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = MyAppFontFamily),
        bodySmall = bodySmall.copy(fontFamily = MyAppFontFamily),
        labelLarge = labelLarge.copy(fontFamily = MyAppFontFamily),
        labelMedium = labelMedium.copy(fontFamily = MyAppFontFamily),
        labelSmall = labelSmall.copy(fontFamily = MyAppFontFamily)
    )
}