package com.sflightx.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class ThemePreferences(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE)

    // Function to save the 'Material You' theme preference
    fun saveMaterialYouEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("material_you_enabled", enabled) }
    }

    // Function to get the 'Material You' theme preference
    fun isMaterialYouEnabled(): Boolean {
        // Default is true if not found
        return sharedPreferences.getBoolean("material_you_enabled", true)
    }
}
