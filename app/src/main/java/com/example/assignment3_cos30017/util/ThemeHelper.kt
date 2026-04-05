package com.example.assignment3_cos30017.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {

    private const val PREF_NAME = "theme_pref"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    /** Returns the user's saved preference; defaults to false (light mode). */
    fun getSavedDarkMode(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
    }

    fun applyDarkMode(context: Context, isDark: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK_MODE, isDark).apply()
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /** Call once in Application.onCreate() to restore saved preference. */
    fun applyOnStartup(context: Context) {
        applyDarkMode(context, getSavedDarkMode(context))
    }
}
