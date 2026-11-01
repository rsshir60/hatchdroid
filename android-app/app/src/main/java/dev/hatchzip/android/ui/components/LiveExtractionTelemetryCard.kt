package dev.hatchzip.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.hatchzip.android.ui.extraction.LiveExtractionUiModel
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.util.StorageUtil
import dev.hatchzip.android.viewmodel.ExtractionMode
import uniffi.hatchzip_android.ExtractionPhase

@Composable
fun LiveExtractionTelemetryCard(
    archiveName: String,
    mode: ExtractionMode,
    model: LiveExtractionUiModel,
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onAbortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Haptic milestone: batch ftruncate committed & storage freed
    LaunchedEffect(model.bytesArchiveReclaimed) {
        if (model.bytesArchiveReclaimed > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Haptic milestone: extraction fully completed
    LaunchedEffect(model.phase) {
        if (model.phase == ExtractionPhase.Completed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Archive Name + Live Phase Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = archiveName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (mode == ExtractionMode.IN_PLACE_HATCH) "In-Place Tail-First Shrink" else "Standard Extraction",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                LivePhaseBadge(phase = model.phase, isPaused = isPaused)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // File Ticker & Progress Ratio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.currentFileName,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (model.entriesTotal > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[${model.entriesCompleted}/${model.entriesTotal}]",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Hatched Output Progress (Shimmer Bar)
            LiveShimmerProgressBar(
                progressFraction = model.hatchedFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hatched: ${StorageUtil.formatBytes(model.bytesHatched)} / ${StorageUtil.formatBytes(model.bytesUncompressedTotal)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldGreen
                )
                Text(
                    text = String.format("%.1f%%", model.hatchedFraction * 100f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 2. Three-Segment Archive Space Bar (In-Place Mode Only)
            if (mode == ExtractionMode.IN_PLACE_HATCH) {
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Archive Storage State",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                ThreeSegmentArchiveShrinkBar(
                    reclaimedFraction = model.reclaimedFraction,
                    stagedFraction = model.stagedForCutFraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "● Freed: ${StorageUtil.formatBytes(model.bytesArchiveReclaimed)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldGreen
                    )
                    Text(
                        text = "● Staged: ${StorageUtil.formatBytes(model.bytesBatchStaged)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning
                    )
                    val remainingBytes = (model.bytesArchiveInitial - model.bytesArchiveReclaimed - model.bytesBatchStaged).coerceAtLeast(0L)
                    Text(
                        text = "● Left: ${StorageUtil.formatBytes(remainingBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Telemetry Footer: Speed + ETA + Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (model.speedBytesPerSec > 64 * 1024) {
                            "${StorageUtil.formatBytes(model.speedBytesPerSec.toLong())}/s"
                        } else {
                            "— MB/s"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "ETA: ${StorageUtil.formatDuration(model.etaSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAbortClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Abort", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Abort", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = { if (isPaused) onResumeClick() else onPauseClick() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPaused) "Resume" else "Pause", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun LivePhaseBadge(
    phase: ExtractionPhase,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    val (label, containerColor, contentColor, isPulsing) = when {
        isPaused -> Quad("Paused", AmberWarningContainer, AmberWarningText, false)
        phase is ExtractionPhase.ScanningIndex -> Quad("Scanning Index", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, true)
        phase is ExtractionPhase.DecodingEntry -> Quad("Hatching", EmeraldGreenContainer, EmeraldGreenText, true)
        phase is ExtractionPhase.FlushingFsync -> Quad("Flushing Flash (fsync)", AmberWarningContainer, AmberWarningText, true)
        phase is ExtractionPhase.TruncatingTail -> Quad("Truncating (ftruncate)", ArchivePurpleContainer, ArchivePurpleText, true)
        phase is ExtractionPhase.Completed -> Quad("Complete", EmeraldGreenContainer, EmeraldGreenText, false)
        phase is ExtractionPhase.Paused -> Quad("Paused", AmberWarningContainer, AmberWarningText, false)
        phase is ExtractionPhase.Failed -> Quad("Failed", CoralRedContainer, CoralRedText, false)
        else -> Quad("Processing", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, false)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = if (isPulsing) alphaPulse else 1f))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun LiveShimmerProgressBar(
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(120),
        label = "shimmerProgress"
    )

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val shimmerTranslate by transition.animateFloat(
        initialValue = -1.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val baseFillColor = EmeraldGreen
    val highlightColor = Color(0xFF6EE7B7) // Light Mint Emerald highlight

    Canvas(modifier = modifier.clip(RoundedCornerShape(6.dp))) {
        val width = size.width
        val height = size.height

        // 1. Background Track
        drawRect(color = trackColor, size = size)

        val fillWidth = width * animatedProgress
        if (fillWidth > 0f) {
            // 2. Shimmer Brush across active filled area
            val shimmerStart = fillWidth * shimmerTranslate
            val brush = Brush.linearGradient(
                colors = listOf(baseFillColor, highlightColor, baseFillColor),
                start = Offset(shimmerStart, 0f),
                end = Offset(shimmerStart + (fillWidth * 0.4f).coerceAtLeast(100f), height)
            )

            drawRect(
                brush = brush,
                size = Size(fillWidth, height)
            )
        }
    }
}

@Composable
fun ThreeSegmentArchiveShrinkBar(
    reclaimedFraction: Float,
    stagedFraction: Float,
    modifier: Modifier = Modifier
) {
    val animReclaimed by animateFloatAsState(
        targetValue = reclaimedFraction.coerceIn(0f, 1f),
        animationSpec = tween(200),
        label = "animReclaimed"
    )
    val animStaged by animateFloatAsState(
        targetValue = stagedFraction.coerceIn(0f, 1f - reclaimedFraction),
        animationSpec = tween(200),
        label = "animStaged"
    )

    val reclaimedColor = EmeraldGreen
    val stagedColor = AmberWarning
    val stagedStripeColor = Color(0xFFFDE68A) // Light Amber stripe
    val remainingColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.clip(RoundedCornerShape(6.dp))) {
        val totalW = size.width
        val h = size.height

        val reclaimedW = totalW * animReclaimed
        val stagedW = totalW * animStaged
        val remainingW = (totalW - reclaimedW - stagedW).coerceAtLeast(0f)

        // 1. Reclaimed Segment (Permanently cut from disk)
        if (reclaimedW > 0f) {
            drawRect(
                color = reclaimedColor,
                topLeft = Offset(0f, 0f),
                size = Size(reclaimedW, h)
            )
        }

        // 2. Staged Segment (Decoded, waiting for next fsync + ftruncate barrier)
        if (stagedW > 0f) {
            // Draw amber base
            drawRect(
                color = stagedColor,
                topLeft = Offset(reclaimedW, 0f),
                size = Size(stagedW, h)
            )

            // Draw diagonal candy stripes
            val stripeWidth = 6.dp.toPx()
            val stripeSpacing = 12.dp.toPx()
            var x = reclaimedW
            while (x < reclaimedW + stagedW) {
                drawLine(
                    color = stagedStripeColor.copy(alpha = 0.5f),
                    start = Offset(x, h),
                    end = Offset(x + stripeWidth, 0f),
                    strokeWidth = 3.dp.toPx()
                )
                x += stripeSpacing
            }
        }

        // 3. Untouched Compressed Archive
        if (remainingW > 0f) {
            drawRect(
                color = remainingColor,
                topLeft = Offset(reclaimedW + stagedW, 0f),
                size = Size(remainingW, h)
            )
        }
    }
}
