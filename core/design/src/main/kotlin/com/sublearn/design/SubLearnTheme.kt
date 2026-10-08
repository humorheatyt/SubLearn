package com.sublearn.design

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sublearn.domain.ThemeMode

/** Centralized, accessible colors and geometry; feature modules consume these tokens only. */
object SubLearnColors {
    val Ink = Color(0xFF121318)
    val InkMuted = Color(0xFF747781)
    val Canvas = Color(0xFFF6F4EF)
    val CanvasContainer = Color(0xFFFFFFFF)
    val Brand = Color(0xFF5556B5)
    val BrandDark = Color(0xFF34358A)
    val BrandSoft = Color(0xFFE9E8FF)
    val Learning = Color(0xFFFFC857)
    val LearningSoft = Color(0x33FFC857)
    val Native = Color(0xFFF2F0EB)
    val Success = Color(0xFF2D805C)
    val Warning = Color(0xFF9A5700)
    val Danger = Color(0xFFBA1A1A)
    val DarkCanvas = Color(0xFF15161B)
    val DarkSurface = Color(0xFF202127)
    val DarkElevated = Color(0xFF2B2C34)
    val DarkText = Color(0xFFF2F0F6)
    val DarkMuted = Color(0xFFB8B6C2)
    val PureBlack = Color(0xFF000000)
    val PlayerSubtitleScrim = Color(0x99000000)
    val PlayerSurface = Color(0xFF050506)
    val PlayerText = Color(0xFFFFFFFF)
    val Transparent = Color.Transparent
}

object SubLearnSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val screen = 20.dp
    val target = 48.dp
    val playerControl = 52.dp
    val iconHero = 64.dp
    val gestureEdgeGuard = 32.dp
    val floatingButtonSeparation = 90.dp
    val homeHeroIcon = 56.dp
    val continueIcon = 48.dp
    val mediaThumbnail = 52.dp
    val subtitleListMinWidth = 260.dp
    val subtitleListMaxWidth = 420.dp
    val gestureTouchSlop = 8.dp
    val twoFingerTouchSlop = 24.dp
    val posterHeight = 184.dp
}

object SubLearnRadii {
    val small = 12.dp
    val medium = 20.dp
    val large = 28.dp
    val pill = 100.dp
}

object SubLearnElevation {
    val flat = 0.dp
    val card = 2.dp
    val wordCard = 12.dp
    val popup = 6.dp
}

object SubLearnStroke {
    val underline = 1.5.dp
    val outline = 1.dp
    val inset = 2.dp
    val corner = 3.dp
    val dash = 2.dp
}

object SubLearnMotion {
    const val quickMs = 120
    const val standardMs = 220
    const val emphasizedMs = 360
    const val overlayHideMs = 3_000L
    const val feedbackMs = 3_000L
    const val subtitleTapWindowMs = 360L
    const val popupEnterMs = 280
}

object SubLearnAlpha {
    const val emptyCard = 0.52f
    const val cardAccent = 0.75f
    const val wordOutlineFill = 0.16f
    const val wordMarkFill = 0.28f
    const val playerTopScrim = 0.82f
    const val playerBottomScrim = 0.92f
    const val playerCenterControl = 0.92f
    const val playerBufferedTrack = 0.8f
    const val playerInactiveTrack = 0.38f
    const val statusSurface = 0.86f
}

private val lightColors = lightColorScheme(
    primary = SubLearnColors.Brand,
    onPrimary = Color.White,
    primaryContainer = SubLearnColors.BrandSoft,
    onPrimaryContainer = SubLearnColors.BrandDark,
    secondary = SubLearnColors.Learning,
    onSecondary = SubLearnColors.Ink,
    secondaryContainer = Color(0xFFFFE8B0),
    onSecondaryContainer = Color(0xFF3C2D08),
    background = SubLearnColors.Canvas,
    onBackground = SubLearnColors.Ink,
    surface = SubLearnColors.CanvasContainer,
    onSurface = SubLearnColors.Ink,
    surfaceVariant = Color(0xFFEAE8E2),
    onSurfaceVariant = Color(0xFF4A4A52),
    outline = Color(0xFF797983),
    error = SubLearnColors.Danger,
)

private val darkColors = darkColorScheme(
    primary = Color(0xFFB8B5FF),
    onPrimary = Color(0xFF262574),
    primaryContainer = Color(0xFF3D3C88),
    onPrimaryContainer = Color(0xFFE2E0FF),
    secondary = SubLearnColors.Learning,
    onSecondary = Color(0xFF302100),
    secondaryContainer = Color(0xFF594412),
    onSecondaryContainer = Color(0xFFFFE6AD),
    background = SubLearnColors.DarkCanvas,
    onBackground = SubLearnColors.DarkText,
    surface = SubLearnColors.DarkSurface,
    onSurface = SubLearnColors.DarkText,
    surfaceVariant = SubLearnColors.DarkElevated,
    onSurfaceVariant = SubLearnColors.DarkMuted,
    outline = Color(0xFF8A8994),
    error = Color(0xFFFFB4AB),
)

private val amoledColors = darkColors.copy(
    background = SubLearnColors.PureBlack,
    surface = SubLearnColors.PureBlack,
    surfaceVariant = Color(0xFF141414),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF202020),
)

@Composable
fun SubLearnTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val useDark = mode == ThemeMode.DARK || mode == ThemeMode.AMOLED || (mode == ThemeMode.SYSTEM && systemDark)
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && useDark -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        mode == ThemeMode.AMOLED -> amoledColors
        useDark -> darkColors
        else -> lightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            displayLarge = Typography().displayLarge.copy(fontSize = 32.sp),
            headlineSmall = Typography().headlineSmall.copy(fontSize = 22.sp),
            titleLarge = Typography().titleLarge.copy(fontSize = 20.sp),
            bodyLarge = Typography().bodyLarge.copy(fontSize = 16.sp),
            bodyMedium = Typography().bodyMedium.copy(fontSize = 14.sp),
            labelLarge = Typography().labelLarge.copy(fontSize = 14.sp),
        ),
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(SubLearnRadii.small),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(SubLearnRadii.medium),
            large = androidx.compose.foundation.shape.RoundedCornerShape(SubLearnRadii.large),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        ),
        content = content,
    )
}
