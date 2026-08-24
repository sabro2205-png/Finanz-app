package de.sabro.finanzapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Farben mit fester Bedeutung – unabhaengig vom (dynamischen) Farbschema. */
val IncomeGreen = Color(0xFF2E7D32)
val IncomeGreenDark = Color(0xFF7BC67E)
val ExpenseRed = Color(0xFFC62828)
val ExpenseRedDark = Color(0xFFEF8A85)
val SavingsBlue = Color(0xFF1565C0)
val SavingsBlueDark = Color(0xFF7FB4F0)

/**
 * Farben der Spartoepfe – ein zusammenhaengender Ausschnitt einer auf
 * Farbfehlsichtigkeit geprueften Reihe. Der Topfname steht immer daneben,
 * die Farbe traegt die Unterscheidung nie allein.
 */
val PotColorsLight = listOf(
    Color(0xFF1BAF7A), Color(0xFFEDA100), Color(0xFFE87BA4),
    Color(0xFF008300), Color(0xFF4A3AA7), Color(0xFFE34948)
)
val PotColorsDark = listOf(
    Color(0xFF199E70), Color(0xFFC98500), Color(0xFFD55181),
    Color(0xFF008300), Color(0xFF9085E9), Color(0xFFE66767)
)

val CategoryColors = listOf(
    Color(0xFF00695C),
    Color(0xFF5E35B1),
    Color(0xFFEF6C00),
    Color(0xFF546E7A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F5132),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7E4C7),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4C6358),
    surfaceVariant = Color(0xFFDDE5DE)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD9AE),
    onPrimary = Color(0xFF003820),
    primaryContainer = Color(0xFF1F4B36),
    onPrimaryContainer = Color(0xFFB7E4C7),
    secondary = Color(0xFFB3CCC0)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 15.sp),
    bodySmall = TextStyle(fontSize = 13.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun FinanzAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

/** Grün fuer Einnahmen / positive Salden, passend zum Hell- oder Dunkelmodus. */
@Composable
fun incomeColor(): Color = if (isSystemInDarkTheme()) IncomeGreenDark else IncomeGreen

@Composable
fun expenseColor(): Color = if (isSystemInDarkTheme()) ExpenseRedDark else ExpenseRed

@Composable
fun savingsColor(): Color = if (isSystemInDarkTheme()) SavingsBlueDark else SavingsBlue

/** Topffarben passend zum Hell- oder Dunkelmodus. */
val PotColors: List<Color>
    @Composable get() = if (isSystemInDarkTheme()) PotColorsDark else PotColorsLight

@Composable
fun amountColor(cents: Long): Color = when {
    cents > 0 -> incomeColor()
    cents < 0 -> expenseColor()
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
