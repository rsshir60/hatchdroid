package dev.hatchzip.android.viewmodel

import dev.hatchzip.android.util.DeviceStorageInfo
import java.io.File

enum class ExtractionMode {
    IN_PLACE_HATCH,  // Shrink & delete archive in real-time
    NORMAL_KEEP      // Keep original archive whole (--keep-archive)
}

enum class ConflictResolution {
    SKIP_EXISTING,
    REPLACE,
    KEEP_BOTH
}

enum class FilterChip {
    ALL_FOLDERS,
    LARGE_ARCHIVES,  // > 1 GB
    SPLIT_VOLUMES,   // .001 / .part1.rar
    DOWNLOADS
}

sealed class ExtractionUiPhase {
    data object Idle : ExtractionUiPhase()
    data object Checking : ExtractionUiPhase()
    data object Planning : ExtractionUiPhase()
    data object Ready : ExtractionUiPhase()
    data object Extracting : ExtractionUiPhase()
    data object Paused : ExtractionUiPhase()
    data object Finished : ExtractionUiPhase()
    data class Failed(val message: String) : ExtractionUiPhase()
}

data class ArchiveDetails(
    val path: String,
    val fileName: String,
    val format: String,
    val itemCount: Long,
    val totalSize: Long,
    val isEncrypted: Boolean,
    val isSingleRoot: Boolean,
    val displayName: String,
    val defaultOutputDir: String,
    val inPlaceRefusal: String? = null,
)

data class SpacePlanDetails(
    val freeBytes: Long,
    val neededBytes: Long,
    val keepFreeBytes: Long,
    val fits: Boolean,
    val freeAfterBytes: Long,
    val keepArchive: Boolean,
    val inPlaceNeeds: Long,
    val normalNeeds: Long,
    val reclaims: Boolean,
)

data class PasswordPromptState(
    val archivePath: String,
    val archiveName: String,
    val isHeaderEncryption: Boolean,
    val failedAttempts: Int = 0,
    val errorMessage: String? = null
)

data class ExtractionUiState(
    val phase: ExtractionUiPhase = ExtractionUiPhase.Idle,
    val selectedArchive: ArchiveDetails? = null,
    val spacePlan: SpacePlanDetails? = null,
    val storageInfo: DeviceStorageInfo = DeviceStorageInfo(0, 0, 0, 0),

    // Active extraction telemetry
    val originalArchiveSize: Long = 0L,
    val currentArchiveSize: Long = 0L,
    val extractedBytes: Long = 0L,
    val totalOutputBytes: Long = 0L,
    val freeSpaceFloor: Long = 2L * 1024 * 1024 * 1024, // 2 GB default safety floor
    val currentFreeSpace: Long = 0L,
    val currentFileName: String = "",

    // Password modal state
    val passwordDialog: PasswordPromptState? = null,

    // User configurable extraction options
    val extractionMode: ExtractionMode = ExtractionMode.IN_PLACE_HATCH,
    val keepFreeBufferGb: Float = 2.0f,
    val verifyFirst: Boolean = false,
    val conflictResolution: ConflictResolution = ConflictResolution.SKIP_EXISTING,
    val passwordInput: String? = null,
    val outputDirectoryOverride: String? = null,

    // Interrupted extraction detection (crash recovery)
    val interruptedArchives: List<String> = emptyList(),

    // File Browser
    val currentDirectory: File = File("/storage/emulated/0"),
    val directoryFiles: List<File> = emptyList(),
    val activeFilterChip: FilterChip = FilterChip.ALL_FOLDERS,
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
)
