package dev.hatchzip.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.util.StorageUtil
import dev.hatchzip.android.viewmodel.*

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
        containerColor = Color(0xFF0F0F14),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Header with Close Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = archive.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Technical Metadata Pill Strip (Monospace badges)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ArchivePurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = archive.format.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ArchivePurple,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E1E28)
                        ) {
                            Text(
                                text = "${archive.itemCount} items",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E1E28)
                        ) {
                            Text(
                                text = StorageUtil.formatBytes(archive.totalSize),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (archive.isEncrypted) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AmberWarningContainer
                            ) {
                                Text(
                                    text = "ENCRYPTED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AmberWarningText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Verdict Card
            val fits = plan?.fits ?: true
            val normalFits = plan?.let { it.freeBytes >= it.normalNeeds + it.keepFreeBytes } ?: false

            Surface(
                color = if (fits) EmeraldGreenContainer else CoralRedContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (fits) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = if (fits) EmeraldGreenText else CoralRedText,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (fits) {
                                if (extractionMode == ExtractionMode.IN_PLACE_HATCH) {
                                    val savedStorage = (archive.totalSize - (plan?.neededBytes ?: 0L)).coerceAtLeast(0L)
                                    "Fits In-Place! Reclaims ${StorageUtil.formatBytes(savedStorage)}"
                                } else {
                                    "Ready for Normal Extraction"
                                }
                            } else {
                                val shortage = ((plan?.neededBytes ?: 0L) + (plan?.keepFreeBytes ?: 0L) - (plan?.freeBytes ?: 0L)).coerceAtLeast(0L)
                                "Cannot Extract: Need ${StorageUtil.formatBytes(shortage)} more"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (fits) EmeraldGreenText else CoralRedText
                        )
                        Text(
                            text = if (fits) {
                                "Safety floor stays above ${StorageUtil.formatBytes(plan?.keepFreeBytes ?: (2L * 1024 * 1024 * 1024))} throughout"
                            } else {
                                "Free space is too constrained to maintain the safety buffer."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (fits) EmeraldGreenText.copy(alpha = 0.85f) else CoralRedText.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Synchronized Dual 8 dp Before/After Storage Bars
            Text(
                text = "STORAGE IMPACT",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            plan?.let { p ->
                val maxBar = (p.freeBytes + archive.totalSize).coerceAtLeast(1L).toFloat()

                Surface(
                    color = Color(0xFF14141C),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Before Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Before Hatching",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Free: ${StorageUtil.formatBytes(p.freeBytes)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = EmeraldGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF222230))
                        ) {
                            val archiveFrac = (archive.totalSize.toFloat() / maxBar).coerceIn(0.01f, 1f)
                            val freeFrac = (p.freeBytes.toFloat() / maxBar).coerceIn(0.01f, 1f)
                            Box(modifier = Modifier.weight(archiveFrac).fillMaxHeight().background(ArchivePurple))
                            Box(modifier = Modifier.weight(freeFrac).fillMaxHeight().background(EmeraldGreen))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // After Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "After Hatching",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Floor: ${StorageUtil.formatBytes(p.freeAfterBytes)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = EmeraldGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF222230))
                        ) {
                            val outFrac = (archive.totalSize.toFloat() / maxBar).coerceIn(0.01f, 1f)
                            val freeAfterFrac = (p.freeAfterBytes.toFloat() / maxBar).coerceIn(0.01f, 1f)
                            Box(modifier = Modifier.weight(outFrac).fillMaxHeight().background(HatchPrimary))
                            Box(modifier = Modifier.weight(freeAfterFrac).fillMaxHeight().background(EmeraldGreen))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Compact Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(color = ArchivePurple, label = "Archive")
                            LegendDot(color = HatchPrimary, label = "Extracted")
                            LegendDot(color = EmeraldGreen, label = "Free")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Extraction Mode Selector (40 dp height)
            Text(
                text = "EXTRACTION MODE",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                SegmentedButton(
                    selected = extractionMode == ExtractionMode.IN_PLACE_HATCH,
                    onClick = { onExtractionModeChange(ExtractionMode.IN_PLACE_HATCH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("In-Place Hatch", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = extractionMode == ExtractionMode.NORMAL_KEEP,
                    onClick = { onExtractionModeChange(ExtractionMode.NORMAL_KEEP) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    enabled = normalFits
                ) {
                    Text("Keep Archive", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
            if (!normalFits) {
                Text(
                    text = "Keep Archive disabled: storage lacks space for two simultaneous copies.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Collision Strategy (40 dp height)
            Text(
                text = "COLLISION STRATEGY",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(40.dp)) {
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

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Collapsible Safety Section
            Surface(
                color = Color(0xFF14141C),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSafetyControls = !showSafetyControls }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Safety Margins & Checksums",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Icon(
                            imageVector = if (showSafetyControls) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = showSafetyControls) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            // Buffer Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Keep-Free Buffer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = String.format("%.1f GB", keepFreeBufferGb),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                            Slider(
                                value = keepFreeBufferGb,
                                onValueChange = onKeepFreeChange,
                                valueRange = 1.0f..5.0f,
                                steps = 7
                            )
                            Text(
                                text = "Guarantees Android OS and apps retain at least ${String.format("%.1f GB", keepFreeBufferGb)} free space at all times.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Pre-Verify Checksums
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pre-Verify Checksums",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Scans archive integrity before modifying disk.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary
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

            Spacer(modifier = Modifier.height(20.dp))

            // 7. Primary Action Button
            Button(
                onClick = onConfirmExtract,
                enabled = fits,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HatchPrimary,
                    contentColor = Color(0xFF0A0A0C),
                    disabledContainerColor = Color(0xFF2A2A38),
                    disabledContentColor = Color(0xFF6A6A7E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = if (extractionMode == ExtractionMode.IN_PLACE_HATCH) Icons.Outlined.Compress else Icons.Outlined.FolderZip,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (extractionMode == ExtractionMode.IN_PLACE_HATCH) "Hatch in Place" else "Extract Normally",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextSecondary
        )
    }
}
