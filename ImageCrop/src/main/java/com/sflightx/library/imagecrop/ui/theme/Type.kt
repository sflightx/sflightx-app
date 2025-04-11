package com.sflightx.library.imagecrop.ui.theme

import androidx.compose.material3.Typography

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