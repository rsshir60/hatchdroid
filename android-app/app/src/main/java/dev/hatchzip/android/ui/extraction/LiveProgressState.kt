package dev.hatchzip.android.ui.extraction

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uniffi.hatchzip_android.ExtractionPhase
import uniffi.hatchzip_android.ExtractionProgressListener
import uniffi.hatchzip_android.ExtractionTelemetry
import kotlin.math.roundToLong

data class LiveExtractionUiModel(
    val isActive: Boolean = false,
    val phase: ExtractionPhase = ExtractionPhase.ScanningIndex,
    val currentFileName: String = "Preparing archive…",
    val entriesCompleted: Long = 0L,
    val entriesTotal: Long = 0L,
    val bytesHatched: Long = 0L,
    val bytesUncompressedTotal: Long = 1L,
    val bytesArchiveReclaimed: Long = 0L,
    val bytesBatchStaged: Long = 0L,
    val bytesArchiveInitial: Long = 1L,
    val speedBytesPerSec: Double = 0.0,
    val etaSeconds: Long? = null
) {
    /** 0.0f..1.0f — Files written to target directory */
    val hatchedFraction: Float
        get() = (bytesHatched.toDouble() / bytesUncompressedTotal.coerceAtLeast(1L).toDouble())
            .coerceIn(0.0, 1.0)
            .toFloat()

    /** 0.0f..1.0f — Archive bytes permanently freed via ftruncate */
    val reclaimedFraction: Float
        get() = (bytesArchiveReclaimed.toDouble() / bytesArchiveInitial.coerceAtLeast(1L).toDouble())
            .coerceIn(0.0, 1.0)
            .toFloat()

    /** 0.0f..1.0f — Archive bytes decoded in current batch and ready to be cut on next ftruncate */
    val stagedForCutFraction: Float
        get() = (bytesBatchStaged.toDouble() / bytesArchiveInitial.coerceAtLeast(1L).toDouble())
            .coerceIn(0.0, (1.0f - reclaimedFraction).toDouble())
            .toFloat()

    /** 0.0f..1.0f — Remaining compressed archive bytes untouched on disk */
    val remainingArchiveFraction: Float
        get() = (1.0f - reclaimedFraction - stagedForCutFraction).coerceIn(0.0f, 1.0f)
}

class TelemetryBridge : ExtractionProgressListener {
    private val _liveState = MutableStateFlow(LiveExtractionUiModel())
    val liveState: StateFlow<LiveExtractionUiModel> = _liveState.asStateFlow()

    private var lastTimestampNs: Long = 0L
    private var lastBytesHatched: Long = 0L
    private var smoothedSpeedBps: Double = 0.0

    fun reset() {
        lastTimestampNs = 0L
        lastBytesHatched = 0L
        smoothedSpeedBps = 0.0
        _liveState.value = LiveExtractionUiModel()
    }

    override fun onTelemetry(snapshot: ExtractionTelemetry) {
        val nowNs = System.nanoTime()
        val hatched = snapshot.bytesHatched.toLong()
        if (lastTimestampNs > 0L) {
            val elapsedSec = (nowNs - lastTimestampNs) / 1_000_000_000.0
            val byteDelta = (hatched - lastBytesHatched).coerceAtLeast(0L)

            if (elapsedSec >= 0.05) {
                val instantSpeed = byteDelta / elapsedSec
                // Exponential Moving Average (alpha = 0.25) for jitter-free MB/s readout
                smoothedSpeedBps = if (smoothedSpeedBps == 0.0) {
                    instantSpeed
                } else {
                    (0.25 * instantSpeed) + (0.75 * smoothedSpeedBps)
                }
                lastTimestampNs = nowNs
                lastBytesHatched = hatched
            }
        } else {
            lastTimestampNs = nowNs
            lastBytesHatched = hatched
        }

        val totalUncompressed = snapshot.bytesUncompressedTotal.toLong().coerceAtLeast(1L)
        val remainingBytes = (totalUncompressed - hatched).coerceAtLeast(0L)
        val etaSec = if (smoothedSpeedBps > 64 * 1024 && remainingBytes > 0) {
            (remainingBytes / smoothedSpeedBps).roundToLong()
        } else {
            null
        }

        val completed = snapshot.phase == ExtractionPhase.Completed ||
                snapshot.phase is ExtractionPhase.Failed ||
                snapshot.phase == ExtractionPhase.Paused

        _liveState.value = LiveExtractionUiModel(
            isActive = !completed,
            phase = snapshot.phase,
            currentFileName = snapshot.currentEntryName.ifEmpty { "Processing…" },
            entriesCompleted = snapshot.entriesCompleted.toLong(),
            entriesTotal = snapshot.entriesTotal.toLong(),
            bytesHatched = hatched,
            bytesUncompressedTotal = totalUncompressed,
            bytesArchiveReclaimed = snapshot.bytesArchiveReclaimed.toLong(),
            bytesBatchStaged = snapshot.bytesBatchStaged.toLong(),
            bytesArchiveInitial = snapshot.bytesArchiveInitial.toLong().coerceAtLeast(1L),
            speedBytesPerSec = smoothedSpeedBps,
            etaSeconds = etaSec
        )
    }
}
