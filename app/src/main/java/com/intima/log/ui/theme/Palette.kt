package com.intima.log.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 夜间模式调色板。页面代码通过 LocalIntimaPalette.current 取色，
 * 不再硬编码浅色值。Miuix 主题色（primary 等）仍由 IntimaTheme 提供。
 */
data class IntimaPalette(
    val bg: Color,        // 页面背景
    val card: Color,      // 卡片/容器背景
    val text: Color,      // 主文字
    val textSub: Color,   // 次要文字
    val textHint: Color,  // 占位/禁用文字
    val chipBg: Color,    // 未选中 Chip 底
    val divider: Color,   // 分隔线/边框
    val isDark: Boolean,
)

val LightPalette = IntimaPalette(
    bg = Color(0xFFF7F7F7),
    card = Color.White,
    text = Color(0xFF1A1A1A),
    textSub = Color(0xFF666666),
    textHint = Color(0xFF999999),
    chipBg = Color(0xFFF0F1F3),
    divider = Color(0xFFE5E6E8),
    isDark = false,
)

val DarkPalette = IntimaPalette(
    bg = Color.Black,
    card = Color(0xFF1C1C1E),
    text = Color(0xFFECECEE),
    textSub = Color(0xFFA8A8AD),
    textHint = Color(0xFF6E6E73),
    chipBg = Color(0xFF2C2C2E),
    divider = Color(0xFF38383A),
    isDark = true,
)

val LocalIntimaPalette = staticCompositionLocalOf { LightPalette }
