package com.aifitnesscoach.core.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aifitnesscoach.core.database.AppColorPalette
import com.aifitnesscoach.core.database.AppThemeMode
import com.aifitnesscoach.core.database.UserGender

// 1) ATHLETIC COBALT (default suggestion for Male profile)
private val CobaltLightScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0F2B6B),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF0B3B36),
    tertiary = Color(0xFF0369A1),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF94A3B8),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

private val CobaltDarkScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF082054),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

// 2) STUDIO VIOLET (default suggestion for Female profile)
private val VioletLightScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFFBE185D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF500724),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFCFAFF),
    onBackground = Color(0xFF1B1426),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1426),
    surfaceVariant = Color(0xFFEDE8F5),
    onSurfaceVariant = Color(0xFF493E5C),
    outline = Color(0xFFA79BBF),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

private val VioletDarkScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF2E1065),
    primaryContainer = Color(0xFF5B21B6),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFFF472B6),
    onSecondary = Color(0xFF500724),
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF042F2E),
    background = Color(0xFF130E1D),
    onBackground = Color(0xFFF5F3FF),
    surface = Color(0xFF1C1429),
    onSurface = Color(0xFFF5F3FF),
    surfaceVariant = Color(0xFF2D2140),
    onSurfaceVariant = Color(0xFFDCD3EE),
    outline = Color(0xFF6B5A87),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

// 3) TEAL (default suggestion for Neutral / Unspecified profile)
private val TealLightScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7FAFA),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE0ECEB),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF94A3B8),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

private val TealDarkScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = Color(0xFF091414),
    onBackground = Color(0xFFF0FDFA),
    surface = Color(0xFF101F1F),
    onSurface = Color(0xFFF0FDFA),
    surfaceVariant = Color(0xFF193030),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

// 4) NEUTRAL PERFORMANCE SLATE & EMERALD
private val NeutralLightScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF334155),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAF9),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE5EAE7),
    onSurfaceVariant = Color(0xFF374151),
    outline = Color(0xFF9CA3AF),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

private val NeutralDarkScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = Color(0xFF0B110F),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF131B18),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF1F2B26),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF4B5563),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val StudioLight = lightColorScheme(
    primary = Color(0xFF365214), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF6A4), onPrimaryContainer = Color(0xFF172506),
    secondary = Color(0xFF52614B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5EDDF), onSecondaryContainer = Color(0xFF233220),
    tertiary = Color(0xFF79593C), tertiaryContainer = Color(0xFFFFDCC0),
    background = Color(0xFFF7F8F2), onBackground = Color(0xFF182119),
    surface = Color(0xFFF7F8F2), onSurface = Color(0xFF182119),
    surfaceVariant = Color(0xFFE4E9DE), onSurfaceVariant = Color(0xFF465142),
    surfaceContainer = Color(0xFFECF0E7), surfaceContainerLow = Color.White,
    outline = Color(0xFF727D6D), outlineVariant = Color(0xFFCDD6C8)
)

private val StudioDark = darkColorScheme(
    primary = Color(0xFFD6FF72), onPrimary = Color(0xFF243300),
    primaryContainer = Color(0xFF35491C), onPrimaryContainer = Color(0xFFE4FFA9),
    secondary = Color(0xFFC3D2B7), onSecondary = Color(0xFF273225),
    secondaryContainer = Color(0xFF293527), onSecondaryContainer = Color(0xFFDBE9D2),
    tertiary = Color(0xFFFFC49A), tertiaryContainer = Color(0xFF493124),
    background = Color(0xFF131A14), onBackground = Color(0xFFF0F4EB),
    surface = Color(0xFF131A14), onSurface = Color(0xFFF0F4EB),
    surfaceVariant = Color(0xFF283428), onSurfaceVariant = Color(0xFFBFCABA),
    surfaceContainer = Color(0xFF202A20), surfaceContainerLow = Color(0xFF192219),
    outline = Color(0xFF93A18E), outlineVariant = Color(0xFF42513F)
)

fun resolveEffectivePalette(
    palettePreference: AppColorPalette,
    gender: UserGender
): AppColorPalette {
    return if (palettePreference != AppColorPalette.AUTO) {
        palettePreference
    } else {
        when (gender) {
            UserGender.MALE -> AppColorPalette.ATHLETIC_COBALT
            UserGender.FEMALE -> AppColorPalette.VIOLET
            UserGender.NEUTRAL -> AppColorPalette.TEAL
        }
    }
}

fun selectColorScheme(
    gender: UserGender,
    darkTheme: Boolean,
    palettePreference: AppColorPalette = AppColorPalette.AUTO
): ColorScheme {
    val effective = resolveEffectivePalette(palettePreference, gender)
    return when (effective) {
        AppColorPalette.ATHLETIC_COBALT -> if (darkTheme) CobaltDarkScheme else CobaltLightScheme
        AppColorPalette.VIOLET -> if (darkTheme) VioletDarkScheme else VioletLightScheme
        AppColorPalette.TEAL -> if (darkTheme) TealDarkScheme else TealLightScheme
        AppColorPalette.NEUTRAL, AppColorPalette.AUTO -> if (darkTheme) NeutralDarkScheme else NeutralLightScheme
    }
}

fun palettePreviewPrimaryColor(palette: AppColorPalette, gender: UserGender, darkTheme: Boolean): Color {
    return appPalette(gender, darkTheme, palette).primary
}

private fun appPalette(gender: UserGender, darkTheme: Boolean, palette: AppColorPalette): ColorScheme =
    if (palette == AppColorPalette.AUTO) {
        if (darkTheme) StudioDark else StudioLight
    } else {
        selectColorScheme(gender, darkTheme, palette)
    }

private fun resolveCoachFontFamily(context: Context): FontFamily {
    val regularId = context.resources.getIdentifier("vazirmatn_regular", "font", context.packageName)
    val boldId = context.resources.getIdentifier("vazirmatn_bold", "font", context.packageName)
    return if (regularId != 0 && boldId != 0) {
        FontFamily(
            Font(regularId, FontWeight.Normal),
            Font(boldId, FontWeight.Bold)
        )
    } else {
        FontFamily.SansSerif
    }
}

@Composable
fun AIFitnessCoachTheme(
    gender: UserGender,
    themeMode: AppThemeMode,
    colorPalette: AppColorPalette = AppColorPalette.AUTO,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        appPalette(gender, darkTheme, colorPalette)
    }

    val fontFamily = resolveCoachFontFamily(context)

    val typography = Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 56.sp,
            lineHeight = 64.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 42.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 34.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

