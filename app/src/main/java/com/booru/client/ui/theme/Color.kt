package com.booru.client.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Claymorphism / Soft UI color palette.
 * Muted, organic tones with warm undertones. No neon, no cyberpunk.
 */
object ClayColors {

    // Base backgrounds
    val Background = Color(0xFFF0EDE8)
    val BackgroundDeep = Color(0xFFE8E3DB)

    // Surface cards
    val Surface = Color(0xFFFFFFFF)
    val SurfaceElevated = Color(0xFFF8F6F3)
    val SurfacePressed = Color(0xFFEDE9E2)

    // Primary (warm brown-gray)
    val Primary = Color(0xFF8B7E74)
    val PrimaryLight = Color(0xFFB0A497)
    val PrimaryDark = Color(0xFF5C534B)

    // Accent (soft gold)
    val Accent = Color(0xFFC4A882)
    val AccentLight = Color(0xFFDDD0BB)
    val AccentDark = Color(0xFFA68B63)

    // Text
    val TextPrimary = Color(0xFF3D3530)
    val TextSecondary = Color(0xFF7A716A)
    val TextTertiary = Color(0xFFA89F96)
    val TextOnPrimary = Color(0xFFFFFCF8)

    // Interactive states
    val ActiveGlow = Color(0x33C4A882) // Accent at 20% opacity
    val PressedShadow = Color(0x408B7E74) // Primary at 25% opacity
    val InnerGlow = Color(0x22FFFFFF)

    // Feedback
    val Success = Color(0xFF8FBF8F)
    val Warning = Color(0xFFD4A76A)
    val Error = Color(0xFFC48B8B)

    // Shadows for Claymorphism
    val ShadowLight = Color(0xFFFFFFFF)
    val ShadowDark = Color(0x1A3D3530) // TextPrimary at 10%
    val ShadowMedium = Color(0x283D3530) // TextPrimary at ~16%
    val ShadowHeavy = Color(0x3D3D3530) // TextPrimary at ~24%
}
