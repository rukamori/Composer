package moe.rukamori.composerapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ComposerTheme(
    useDynamicColor: Boolean,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        remember(useDynamicColor, darkTheme) {
            when {
                useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }

                darkTheme -> {
                    composerDarkColorScheme()
                }

                else -> {
                    composerLightColorScheme()
                }
            }
        }
    val shapes =
        remember {
            Shapes(
                extraSmall =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(8.dp),
                small =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(12.dp),
                medium =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(16.dp),
                large =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(24.dp),
                extraLarge =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(32.dp),
            )
        }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = shapes,
        content = content,
    )
}

private fun composerLightColorScheme(): ColorScheme =
    lightColorScheme(
        primary = Color(0xFFBA1A1A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410002),
        secondary = Color(0xFF775651),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDAD6),
        onSecondaryContainer = Color(0xFF2C1512),
        tertiary = Color(0xFF705C2E),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFCDFA6),
        onTertiaryContainer = Color(0xFF251A00),
    )

private fun composerDarkColorScheme(): ColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFB4AB),
        onPrimary = Color(0xFF690005),
        primaryContainer = Color(0xFF93000A),
        onPrimaryContainer = Color(0xFFFFDAD6),
        secondary = Color(0xFFE7BDB6),
        onSecondary = Color(0xFF442925),
        secondaryContainer = Color(0xFF5D3F3B),
        onSecondaryContainer = Color(0xFFFFDAD6),
        tertiary = Color(0xFFDFC38C),
        onTertiary = Color(0xFF3E2E04),
        tertiaryContainer = Color(0xFF564419),
        onTertiaryContainer = Color(0xFFFCDFA6),
    )
