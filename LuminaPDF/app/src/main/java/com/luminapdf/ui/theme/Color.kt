package com.luminapdf.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Midnight / Slate Palette ─────────────────────────────────────────────────

// Primary — a cool indigo-blue that pops on midnight backgrounds
val MidnightPrimary       = Color(0xFF82AAFF)   // cornflower blue
val MidnightOnPrimary     = Color(0xFF00215B)
val MidnightPrimaryContainer = Color(0xFF193872)
val MidnightOnPrimaryContainer = Color(0xFFD8E2FF)

// Secondary — muted slate violet
val MidnightSecondary     = Color(0xFFBBC4E8)
val MidnightOnSecondary   = Color(0xFF243055)
val MidnightSecondaryContainer = Color(0xFF3B476D)
val MidnightOnSecondaryContainer = Color(0xFFD8E0FF)

// Tertiary — soft teal accent
val MidnightTertiary      = Color(0xFF96CEFF)
val MidnightOnTertiary    = Color(0xFF003354)
val MidnightTertiaryContainer = Color(0xFF0E4F7A)
val MidnightOnTertiaryContainer = Color(0xFFCCE5FF)

// Backgrounds — deep midnight tones
val MidnightBackground    = Color(0xFF0D1117)   // GitHub dark-ish
val MidnightOnBackground  = Color(0xFFE2E8F0)
val MidnightSurface       = Color(0xFF161B22)
val MidnightOnSurface     = Color(0xFFE2E8F0)
val MidnightSurfaceVariant = Color(0xFF1E2837)
val MidnightOnSurfaceVariant = Color(0xFFBFC8E0)

val MidnightOutline       = Color(0xFF3A4556)
val MidnightOutlineVariant = Color(0xFF2A3447)

// Error
val MidnightError         = Color(0xFFFF8A80)
val MidnightOnError       = Color(0xFF690005)

// ── Light Palette ────────────────────────────────────────────────────────────

val LightPrimary          = Color(0xFF2B5EAF)
val LightOnPrimary        = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD8E2FF)
val LightOnPrimaryContainer = Color(0xFF00215B)
val LightBackground       = Color(0xFFF8F9FF)
val LightOnBackground     = Color(0xFF1A1C22)
val LightSurface          = Color(0xFFFFFFFF)
val LightOnSurface        = Color(0xFF1A1C22)
val LightSurfaceVariant   = Color(0xFFE1E5F3)
val LightOnSurfaceVariant = Color(0xFF44485A)
val LightOutline          = Color(0xFF74788C)

// ── Color Schemes ────────────────────────────────────────────────────────────

val DarkColorScheme = darkColorScheme(
    primary             = MidnightPrimary,
    onPrimary           = MidnightOnPrimary,
    primaryContainer    = MidnightPrimaryContainer,
    onPrimaryContainer  = MidnightOnPrimaryContainer,
    secondary           = MidnightSecondary,
    onSecondary         = MidnightOnSecondary,
    secondaryContainer  = MidnightSecondaryContainer,
    onSecondaryContainer = MidnightOnSecondaryContainer,
    tertiary            = MidnightTertiary,
    onTertiary          = MidnightOnTertiary,
    tertiaryContainer   = MidnightTertiaryContainer,
    onTertiaryContainer = MidnightOnTertiaryContainer,
    background          = MidnightBackground,
    onBackground        = MidnightOnBackground,
    surface             = MidnightSurface,
    onSurface           = MidnightOnSurface,
    surfaceVariant      = MidnightSurfaceVariant,
    onSurfaceVariant    = MidnightOnSurfaceVariant,
    outline             = MidnightOutline,
    outlineVariant      = MidnightOutlineVariant,
    error               = MidnightError,
    onError             = MidnightOnError
)

val LightColorScheme = lightColorScheme(
    primary             = LightPrimary,
    onPrimary           = LightOnPrimary,
    primaryContainer    = LightPrimaryContainer,
    onPrimaryContainer  = LightOnPrimaryContainer,
    background          = LightBackground,
    onBackground        = LightOnBackground,
    surface             = LightSurface,
    onSurface           = LightOnSurface,
    surfaceVariant      = LightSurfaceVariant,
    onSurfaceVariant    = LightOnSurfaceVariant,
    outline             = LightOutline
)
