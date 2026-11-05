package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Font family: Tomorrow
val TomorrowFontFamily = FontFamily(
    Font(R.font.tomorrow, FontWeight.Normal),
    Font(R.font.tomorrow, FontWeight.Medium),
    Font(R.font.tomorrow, FontWeight.SemiBold),
    Font(R.font.tomorrow, FontWeight.Bold),
    Font(R.font.tomorrow, FontWeight.ExtraBold)
)

data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val sheet: Color,
    val border: Color,
    val divider: Color,
    val text: Color,
    val textMuted: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentButtonFill: Color,
    val accentButtonText: Color,
    val dangerRed: Color,
    val accentSoft: Color,
    val redSoft: Color,
    val modalOverlay: Color
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF0F1315),
    surface = Color(0xFF171D20),
    sheet = Color(0xFF131A1D),
    border = Color(0xFF2D3A3F),
    divider = Color(0xFF212B2F),
    text = Color(0xFFD0DAD3),
    textMuted = Color(0xFF8FA198),
    textSecondary = Color(0xFFAEBBB3),
    accent = Color(0xFF6FB59F),
    accentButtonFill = Color(0xFF6FB59F),
    accentButtonText = Color(0xFF0F1315),
    dangerRed = Color(0xFFE08A93),
    accentSoft = Color(0x266FB59F), // rgba(111,181,159,.15)
    redSoft = Color(0x33E08A93), // rgba(224,138,147,.20)
    modalOverlay = Color(0xAB000000) // rgba(0,0,0,.67)
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFEEF1EA),
    surface = Color(0xFFF9FBF6),
    sheet = Color(0xFFF9FBF6),
    border = Color(0xFFCBD5C6),
    divider = Color(0xFFDDE4D8),
    text = Color(0xFF1C2621),
    textMuted = Color(0xFF5A6B61),
    textSecondary = Color(0xFF46574D),
    accent = Color(0xFF176A5C), // deep teal
    accentButtonFill = Color(0xFFA9D9CA),
    accentButtonText = Color(0xFF14221E),
    dangerRed = Color(0xFFB3303F),
    accentSoft = Color(0x1A176A5C), // rgba(23,106,92,.10)
    redSoft = Color(0x21B3303F), // rgba(179,48,63,.13)
    modalOverlay = Color(0x661C2621) // rgba(28,38,33,.4)
)

data class AppTypographyTokens(
    val screenTitle: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    val cardName: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    val amountList: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    val heroAmount: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp
    ),
    val netBalance: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    val body: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.5.sp,
        lineHeight = 20.sp
    ),
    val bodyBold: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
        lineHeight = 20.sp
    ),
    val notes: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    val secondary: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    val smallLabel: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.14.sp
    ),
    val primaryButton: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 0.1.sp
    ),
    val chip: TextStyle = TextStyle(
        fontFamily = TomorrowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
)

object AppDimens {
    val screenPadding: Dp = 20.dp
    val gapBetweenBlocks: Dp = 13.dp
    val cardRadius: Dp = 14.dp
    val buttonRadius: Dp = 12.dp
    val chipRadius: Dp = 10.dp
    val iconSquareRadius: Dp = 12.dp
    val sheetTopRadius: Dp = 24.dp
    val loginLogoRadius: Dp = 30.dp

    val standardBorder: Dp = 1.5.dp
    val emphasizedBorder: Dp = 2.dp
    val thickBorder: Dp = 2.5.dp

    val minTouchTarget: Dp = 44.dp
    val primaryButtonHeight: Dp = 56.dp
    val chipHeight: Dp = 44.dp
}

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }
val LocalAppTypography = staticCompositionLocalOf { AppTypographyTokens() }

object PartnerTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val dimens: AppDimens
        get() = AppDimens
}
