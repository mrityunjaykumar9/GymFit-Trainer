package com.freegymtrainer.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Centralized palette - every screen references these, never hardcodes a hex value.
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

val LightBackground = Color(0xFFF7F7FA)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1B1F3B)

val DarkBackground = Color(0xFF12141F)
val DarkSurface = Color(0xFF1B1F30)
val DarkOnSurface = Color(0xFFEDEDF2)
