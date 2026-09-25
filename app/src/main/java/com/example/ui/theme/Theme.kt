package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TelegramLightBlue,
    onPrimary = VaultDarkBg,
    primaryContainer = TelegramBlue,
    onPrimaryContainer = Color.White,
    secondary = TelegramCyan,
    onSecondary = VaultDarkBg,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = EncryptGoldLight,
    onTertiary = VaultDarkBg,
    background = VaultDarkBg,
    onBackground = TextPrimaryDark,
    surface = VaultDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = VaultDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = VaultCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = TelegramBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = TelegramCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF0E7490),
    tertiary = EncryptGold,
    onTertiary = Color.White,
    background = VaultLightBg,
    onBackground = TextPrimaryLight,
    surface = VaultLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = VaultLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun TeleVaultTheme(
    darkTheme: Boolean = true, // Default to sleek midnight vault theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
