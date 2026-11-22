package com.aifitnesscoach.core.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.LocaleListCompat
import com.aifitnesscoach.core.database.AppLanguage
import java.util.Locale

object LocaleManager {

    fun applyLanguage(language: AppLanguage) {
        val locales = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
            AppLanguage.FA -> LocaleListCompat.forLanguageTags("fa")
        }
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    fun isPersianActive(language: AppLanguage): Boolean {
        return when (language) {
            AppLanguage.FA -> true
            AppLanguage.EN -> false
            AppLanguage.SYSTEM -> Locale.getDefault().language.equals("fa", ignoreCase = true)
        }
    }

    fun layoutDirectionFor(language: AppLanguage): LayoutDirection {
        return if (isPersianActive(language)) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    fun activeLocale(language: AppLanguage): Locale {
        return if (isPersianActive(language)) Locale("fa", "IR") else Locale.ENGLISH
    }
}
