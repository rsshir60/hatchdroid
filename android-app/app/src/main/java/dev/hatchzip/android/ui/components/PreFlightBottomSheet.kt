package dev.hatchzip.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.util.StorageUtil
import dev.hatchzip.android.viewmodel.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreFlightBottomSheet(
    archive: ArchiveDetails,
    plan: SpacePlanDetails?,
    extractionMode: ExtractionMode,
    conflictResolution: ConflictResolution,
    keepFreeBufferGb: Float,
    verifyFirst: Boolean,
    onExtractionModeChange: (ExtractionMode) -> Unit,
    onConflictResolutionChange: (ConflictResolution) -> Unit,
    onKeepFreeChange: (Float) -> Unit,
    onVerifyFirstChange: (Boolean) -> Unit,
    onConfirmExtract: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSafetyControls by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = archive.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "${archive.format.uppercase()} • ${archive.itemCount} items • ${StorageUtil.formatBytes(archive.totalSize)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (archive.isEncrypted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AmberWarningContainer
                            ) {
                                Text(
                                    text = "ENCRYPTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberWarningText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Verdict Header Pill
            val fits = plan?.fits ?: true
            val normalFits = plan?.let { it.freeBytes >= it.normalNeeds + it.keepFreeBytes } ?: false

            Surface(
                color = if (fits) EmeraldGreenContainer else CoralRedContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (fits) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = if (fits) EmeraldGreenText else CoralRedText,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (fits) {
                                if (extractionMode == ExtractionMode.IN_PLACE_HATCH) {
                                    val savedDoubleStorage = (archive.totalSize - (plan?.neededBytes ?: 0L)).coerceAtLeast(0L)
                                    "Fits In-Place! Saves ${StorageUtil.formatBytes(savedDoubleStorage)} storage"
                                } else {
                                    "Ready for Normal Extraction"
                                }
                            } else {
                                val shortage = ((plan?.neededBytes ?: 0L) + (plan?.keepFreeBytes ?: 0L) - (plan?.freeBytes ?: 0L)).coerceAtLeast(0L)
                                "Cannot Extract: Need ${StorageUtil.formatBytes(shortage)} more space"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (fits) EmeraldGreenText else CoralRedText
                        )
                        Text(
                            text = if (fits) {
                                "Free space stays above ${StorageUtil.formatBytes(plan?.keepFreeBytes ?: (2L * 1024 * 1024 * 1024))} throughout"
                            } else {
                                "Storage is too tight to protect the safety buffer. Free up space and try again."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (fits) EmeraldGreenText else CoralRedText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Before vs. After Storage Bar
            Text(
                text = "Storage Projection",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            plan?.let { p ->
                val maxBar = (p.freeBytes + archive.totalSize).coerceAtLeast(1L).toFloat()
                
                // Current State Bar
                Text(
                    text = "Current State: Archive (${StorageUtil.formatBytes(archive.totalSize)}) + Free (${StorageUtil.formatBytes(p.freeBytes)})",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val archiveFrac = (archive.totalSize.toFloat() / maxBar).coerceIn(0.01f, 1f)
                    val freeFrac = (p.freeBytes.toFloat() / maxBar).coerceIn(0.01f, 1f)
                    Box(modifier = Modifier.weight(archiveFrac).fillMaxHeight().background(ArchivePurple))
                    Box(modifier = Modifier.weight(freeFrac).fillMaxHeight().background(EmeraldGreen))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Post-Hatch State Bar
                Text(
                    text = "Post-Hatch State: Output (${StorageUtil.formatBytes(archive.totalSize)}) + Floor (${StorageUtil.formatBytes(p.freeAfterBytes)})",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val outFrac = (archive.totalSize.toFloat() / maxBar).coerceIn(0.01f, 1f)
                    val freeAfterFrac = (p.freeAfterBytes.toFloat() / maxBar).coerceIn(0.01f, 1f)
                    Box(modifier = Modifier.weight(outFrac).fillMaxHeight().background(HatchPrimaryLight))
                    Box(modifier = Modifier.weight(freeAfterFrac).fillMaxHeight().background(EmeraldGreen))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Extraction Mode Toggle
            Text(
                text = "Extraction Mode",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = extractionMode == ExtractionMode.IN_PLACE_HATCH,
                    onClick = { onExtractionModeChange(ExtractionMode.IN_PLACE_HATCH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("In-Place Hatch", style = MaterialTheme.typography.labelSmall)
                }
                SegmentedButton(
                    selected = extractionMode == ExtractionMode.NORMAL_KEEP,
                    onClick = { onExtractionModeChange(ExtractionMode.NORMAL_KEEP) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    enabled = normalFits
                ) {
                    Text("Keep Archive", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (!normalFits) {
                Text(
                    text = "Keep Archive disabled: device lacks space for two copies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Conflict Resolution Selector
            Text(
                text = "File Collision Strategy",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = conflictResolution == ConflictResolution.SKIP_EXISTING,
                    onClick = { onConflictResolutionChange(ConflictResolution.SKIP_EXISTING) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Skip", style = MaterialTheme.typography.labelSmall)
                }
                SegmentedButton(
                    selected = conflictResolution == ConflictResolution.REPLACE,
                    onClick = { onConflictResolutionChange(ConflictResolution.REPLACE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Replace", style = MaterialTheme.typography.labelSmall)
                }
                SegmentedButton(
                    selected = conflictResolution == ConflictResolution.KEEP_BOTH,
                    onClick = { onConflictResolutionChange(ConflictResolution.KEEP_BOTH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("Keep Both", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Safety Controls (Expandable)
            OutlinedCard(
                onClick = { showSafetyControls = !showSafetyControls },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Safety Margins & Checksums", style = MaterialTheme.typography.titleSmall)
                        }
                        Icon(
                            imageVector = if (showSafetyControls) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = showSafetyControls) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            // Keep-Free Buffer Slider
                            Text(
                                text = "Keep-Free Buffer: ${String.format("%.1f GB", keepFreeBufferGb)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = keepFreeBufferGb,
                                onValueChange = onKeepFreeChange,
                                valueRange = 1.0f..5.0f,
                                steps = 7
                            )
                            Text(
                                text = "Guarantees Android OS, system cache, and apps keep at least ${String.format("%.1f GB", keepFreeBufferGb)} free space at all times.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Pre-Verify Checksums Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Pre-Verify Checksums", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Scans entire archive before touching disk. Recommended for rare ROMs.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = verifyFirst,
                                    onCheckedChange = onVerifyFirstChange
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = onConfirmExtract,
                enabled = fits,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = if (extractionMode == ExtractionMode.IN_PLACE_HATCH) Icons.Outlined.Compress else Icons.Outlined.FolderZip,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (extractionMode == ExtractionMode.IN_PLACE_HATCH) "Hatch in Place" else "Extract Normally",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
