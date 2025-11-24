package barsikbarbosik.midimapper.ui.theme

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
    primary = MidiGreen,
    secondary = TealGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = MidiGreen,
    secondary = TealGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

object KnobColors {
    val palette1 = listOf(
        Color(0xFF9C0418),
        Color(0xFFD21519),
        Color(0xFFFF3E16),
        Color(0xFFFF7C0A),
        Color(0xFFFFCA00),
        Color(0xFFEDFF00),
        Color(0xFFB4FF00),
        Color(0xFF36FF24),
        Color(0xFF00FA46),
        Color(0xFF00E76E),
        Color(0xFF00CC9B),
        Color(0xFF00AEC3),
        Color(0xFF008CDF),
        Color(0xFF0066EC),
        Color(0xFF6839F2),
        Color(0xFFA211F4)
    )
    val palette2 = listOf(
        Color(0xFF771630),
        Color(0xFFA92930),
        Color(0xFFE14D31),
        Color(0xFFFA8542),
        Color(0xFFFCCB59),
        Color(0xFFEBF769),
        Color(0xFFC2F76E),
        Color(0xFF87E46F),
        Color(0xFF46CA71),
        Color(0xFF00B77C),
        Color(0xFF00A389),
        Color(0xFF008D96),
        Color(0xFF00749F),
        Color(0xFF0059A4),
        Color(0xFF4F39A5),
        Color(0xFF751CA7)
    )
}

@Composable
fun MidiMapperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
