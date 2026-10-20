package com.intima.log.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * Miuix 主题封装。
 * 浅色背景 #F7F7F7 / 卡片纯白；深色背景 #000000 / 卡片 #1C1C1E（HyperOS 规范）。
 * 主色可切换 蓝/绿/紫。
 */
object IntimaColors {
    val Blue = Color(0xFF3482FF)
    val Green = Color(0xFF03B585)
    val Purple = Color(0xFF7B5CFF)

    val LightBg = Color(0xFFF7F7F7)
    val LightCard = Color.White
    val DarkBg = Color.Black
    val DarkCard = Color(0xFF1C1C1E)
}

/** 参与者头像底色板（8 色） */
val AvatarPalette = listOf(
    Color(0xFF5B8DEF), Color(0xFF03B585), Color(0xFF7B5CFF), Color(0xFFFF8F5B),
    Color(0xFFE85A8A), Color(0xFF2FB8D8), Color(0xFF9BB33D), Color(0xFFB085F5),
)

fun avatarColor(colorIndex: Int): Color = AvatarPalette[colorIndex.coerceIn(0, AvatarPalette.size - 1)]

enum class AccentColor(val seed: Color) { BLUE(IntimaColors.Blue), GREEN(IntimaColors.Green), PURPLE(IntimaColors.Purple) }

/** 主题模式：跟随系统 / 浅色 / 深色 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun IntimaTheme(
    accent: AccentColor = AccentColor.BLUE,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (darkTheme) DarkPalette else LightPalette
    val colors: Colors = if (darkTheme) {
        darkColorScheme(
            primary = accent.seed,
            background = IntimaColors.DarkBg,
            surface = IntimaColors.DarkBg,
            surfaceContainer = IntimaColors.DarkCard,
        )
    } else {
        lightColorScheme(
            primary = accent.seed,
            background = IntimaColors.LightBg,
            surface = IntimaColors.LightBg,
            surfaceContainer = IntimaColors.LightCard,
        )
    }

    // 系统栏图标随主题实时反色：浅色主题黑图标、深色主题白图标。
    // WindowInsetsControllerCompat 覆盖状态栏 + 导航栏，themeMode 切换时立即重执行。
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowInsetsControllerCompat(window, view)
            val lightIcons = !darkTheme
            controller.isAppearanceLightStatusBars = lightIcons
            controller.isAppearanceLightNavigationBars = lightIcons
        }
    }

    CompositionLocalProvider(
        LocalIntimaPalette provides palette,
        LocalDarkTheme provides darkTheme,
        // material3 Text/Icon 未显式指定颜色时取 LocalContentColor（默认黑），
        // 深色主题下必须翻转，否则满屏黑字不可见
        androidx.compose.material3.LocalContentColor provides palette.text,
    ) {
        MiuixTheme(colors = colors, content = content)
    }
}

val LocalDarkTheme = staticCompositionLocalOf { false }
