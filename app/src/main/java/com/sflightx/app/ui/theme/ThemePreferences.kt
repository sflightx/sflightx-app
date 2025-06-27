package com.sflightx.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sflightx.app.`class`.ThemeMode

class ThemePreferences(context: Context) {
    private val themePreferences: SharedPreferences = context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE)
    fun saveMaterialYouEnabled(enabled: Boolean) {
        themePreferences.edit { putBoolean("material_you_enabled", enabled) }
    }
    fun isMaterialYouEnabled(): Boolean {
        return themePreferences.getBoolean("material_you_enabled", true)
    }

    fun saveAppTheme(theme: ThemeMode) {
        themePreferences.edit { putString("app_theme", theme.name) }
    }
    fun getAppTheme(): ThemeMode {
        return ThemeMode.valueOf(themePreferences.getString("app_theme", ThemeMode.SYSTEM.name)!!)
    }
}
