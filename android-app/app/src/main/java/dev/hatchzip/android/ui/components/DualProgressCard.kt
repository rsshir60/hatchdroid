package dev.hatchzip.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.util.StorageUtil
import dev.hatchzip.android.viewmodel.ExtractionMode

@Composable
fun DualProgressCard(
    archiveName: String,
    mode: ExtractionMode,
    originalArchiveSize: Long,
    currentArchiveSize: Long,
    extractedBytes: Long,
    totalOutputBytes: Long,
    freeSpaceFloor: Long,
    currentFreeSpace: Long,
    currentFileName: String,
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onAbortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val archiveSafeTotal = originalArchiveSize.coerceAtLeast(1L)
    val outputSafeTotal = totalOutputBytes.coerceAtLeast(1L)

    // Animated fractions
    val archiveShrinkFraction by animateFloatAsState(
        targetValue = (currentArchiveSize.toFloat() / archiveSafeTotal.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(400),
        label = "ArchiveShrink"
    )
    val outputGrowFraction by animateFloatAsState(
        targetValue = (extractedBytes.toFloat() / outputSafeTotal.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(400),
        label = "OutputGrow"
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPaused) "Extraction Paused" else "Hatching Archive",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isPaused) AmberWarningText else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = archiveName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = if (mode == ExtractionMode.IN_PLACE_HATCH) "In-Place" else "Normal",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (mode == ExtractionMode.IN_PLACE_HATCH) ArchivePurpleContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bar 1 — Source Archive (Shrinking)
            if (mode == ExtractionMode.IN_PLACE_HATCH) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Source Archive (Shrinking)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CoralRed
                    )
                    Text(
                        text = "${StorageUtil.formatBytes(currentArchiveSize)} / ${StorageUtil.formatBytes(originalArchiveSize)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { archiveShrinkFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = CoralRed,
                    trackColor = CoralRedContainer
                )

                val reclaimed = (originalArchiveSize - currentArchiveSize).coerceAtLeast(0L)
                Text(
                    text = "${StorageUtil.formatBytes(reclaimed)} space reclaimed directly on disk",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoralRedText,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bar 2 — Hatched Output (Growing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hatched Output (Growing)",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldGreen
                )
                Text(
                    text = "${StorageUtil.formatBytes(extractedBytes)} / ${StorageUtil.formatBytes(totalOutputBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { outputGrowFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = EmeraldGreen,
                trackColor = EmeraldGreenContainer
            )

            if (currentFileName.isNotEmpty()) {
                Text(
                    text = "Writing: $currentFileName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Safety Margin Gauge
            Surface(
                color = EmeraldGreenContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = EmeraldGreenText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Free Space Floor: ${StorageUtil.formatBytes(currentFreeSpace)} (${StorageUtil.formatBytes(freeSpaceFloor)} Protected)",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldGreenText
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reassuring Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAbortClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Abort / Revert", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = if (isPaused) onResumeClick else onPauseClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) EmeraldGreen else AmberWarning
                    )
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Outlined.Check else Icons.Outlined.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPaused) "Resume" else "Pause & Save State",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
