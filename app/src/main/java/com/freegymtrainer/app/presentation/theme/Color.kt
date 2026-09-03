package com.freegymtrainer.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Centralized palette - every screen references these, never hardcodes a hex value.
// The whole app is built from exactly three brand hues - Orange, Navy, Green - plus Error red
// for the one place that needs it. Every other token below (backgrounds, surfaces, outlines,
// muted text) is a tint or shade of one of those hues, not a new color: e.g. LightBackground is
// OrangePrimary blended at ~6% opacity over white, LightOnSurfaceVariant is DeepNavy blended at
// ~65% opacity over white, and so on. Baked into fixed hex rather than computed with a live
// alpha at each call site, since Material3 color-scheme roles get composited over different
// content each time and a literal runtime alpha would look inconsistent screen to screen.
val OrangePrimary = Color(0xFFFF6B35)
val OrangePrimaryDark = Color(0xFFFF8C5A)
val DeepNavy = Color(0xFF1B1F3B)
val SuccessGreen = Color(0xFF2ECC71)
val WarningAmber = Color(0xFFF5A623)
val ErrorRed = Color(0xFFE74C3C)

// "Container" tones for the M3 color roles (badges, stat tiles, chip backgrounds) - keeps
// those surfaces on-brand instead of falling back to Material's default lavender palette.
val OrangeContainerLight = Color(0xFFFFDBCB)
val OrangeOnContainerLight = Color(0xFF7A2E00)
val OrangeContainerDark = Color(0xFF7A3311)
val OrangeOnContainerDark = Color(0xFFFFDBCB)

val NavyContainerLight = Color(0xFFDCE0F5)
val NavyContainerDark = Color(0xFF333A5C)
val NavyOnContainerLight = DeepNavy
val NavyOnContainerDark = Color(0xFFDDE1F2)

val GreenContainerLight = Color(0xFFD2F4DE)
val GreenOnContainerLight = Color(0xFF0B4620)
val GreenContainerDark = Color(0xFF1F4A30)
val GreenOnContainerDark = Color(0xFFD2F4DE)

val ErrorContainerLight = Color(0xFFFBDAD4)
val ErrorOnContainerLight = Color(0xFF7A1F14)
val ErrorContainerDark = Color(0xFF5C2018)
val ErrorOnContainerDark = Color(0xFFFFDAD4)

// A soft warm ivory instead of stark white - a gentle tint of OrangePrimary, so the page itself
// feels branded rather than like a blank system default. Cards (LightSurface) stay a hair
// lighter than the page so they still read as "raised" against it.
val LightBackground = Color(0xFFFFF6EE)
val LightSurface = Color(0xFFFFFDFA)
val LightOnSurface = Color(0xFF1B1F3B)
val LightSurfaceVariant = Color(0xFFEDE6E1)
val LightOnSurfaceVariant = Color(0xFF63667B)
val LightOutline = Color(0xFFAEB1BC)
val LightOutlineVariant = Color(0xFFDDE0E6)
val LightInverseSurface = Color(0xFF2D3150)
val LightInverseOnSurface = Color(0xFFF3F3F8)

val DarkBackground = Color(0xFF12141F)
val DarkSurface = Color(0xFF1B1F30)
val DarkOnSurface = Color(0xFFEDEDF2)
val DarkSurfaceVariant = Color(0xFF262B45)
val DarkOnSurfaceVariant = Color(0xFFB8BBD1)
val DarkOutline = Color(0xFF565B7A)
val DarkOutlineVariant = Color(0xFF33374F)
val DarkInverseSurface = Color(0xFFEDEDF2)
val DarkInverseOnSurface = Color(0xFF1B1F3B)
