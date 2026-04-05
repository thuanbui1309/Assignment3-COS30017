package com.example.assignment3_cos30017.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    private const val PREF_NAME = "locale_pref"
    private const val KEY_LANGUAGE = "language"

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun setLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    fun applyLocale(context: Context): Context {
        val language = getLanguage(context)
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    fun toggleLanguage(activity: Activity) {
        val current = getLanguage(activity)
        val next = if (current == "en") "vi" else "en"
        setLanguage(activity, next)
        activity.recreate()
    }
}
