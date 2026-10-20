package com.intima.log.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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
    CompositionLocalProvider(LocalIntimaPalette provides palette) {
        MiuixTheme(colors = colors, content = content)
    }
}

val LocalDarkTheme = staticCompositionLocalOf { false }