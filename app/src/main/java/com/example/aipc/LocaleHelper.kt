package com.example.aipc

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "Chinese" -> Locale.SIMPLIFIED_CHINESE
            "English" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun updateResources(context: Context, languageCode: String) {
        val newContext = setLocale(context, languageCode)
        context.resources.updateConfiguration(newContext.resources.configuration, newContext.resources.displayMetrics)
    }
}