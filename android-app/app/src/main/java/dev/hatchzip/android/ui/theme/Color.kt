package dev.hatchzip.android.ui.theme

import androidx.compose.ui.graphics.Color

// Material 3 Seed Colors
val HatchPrimaryLight = Color(0xFF006874)
val HatchOnPrimaryLight = Color(0xFFFFFFFF)
val HatchPrimaryContainerLight = Color(0xFF9EEFFD)
val HatchOnPrimaryContainerLight = Color(0xFF001F24)

val HatchPrimaryDark = Color(0xFF82D3E0)
val HatchOnPrimaryDark = Color(0xFF00363D)
val HatchPrimaryContainerDark = Color(0xFF004F58)
val HatchOnPrimaryContainerDark = Color(0xFF9EEFFD)

// Semantic Locked Colors (Strictly preserved across all wallpapers/dynamic themes)
val EmeraldGreen = Color(0xFF10B981)      // Safety floor protected & successful extractions
val EmeraldGreenContainer = Color(0xFFD1FAE5)
val EmeraldGreenText = Color(0xFF065F46)

val AmberWarning = Color(0xFFF59E0B)      // Interrupted jobs, paused/resumable state, warnings
val AmberWarningContainer = Color(0xFFFEF3C7)
val AmberWarningText = Color(0xFF92400E)

val CoralRed = Color(0xFFEF4444)          // Archive shrinking bar, space shortages, critical errors
val CoralRedContainer = Color(0xFFFEE2E2)
val CoralRedText = Color(0xFF991B1B)

val ArchivePurple = Color(0xFF8B5CF6)     // Detected archives / reclaimable storage
val ArchivePurpleContainer = Color(0xFFEDE9FE)
val ArchivePurpleText = Color(0xFF5B21B6)

val StorageUsedGray = Color(0xFF6B7280)   // General OS/app used space
