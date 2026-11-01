package dev.hatchzip.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.util.DeviceStorageInfo
import dev.hatchzip.android.util.StorageUtil

@Composable
fun StorageTelemetryCard(
    storageInfo: DeviceStorageInfo,
    onRefreshScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = HatchSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HatchOutline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 1. Two-Level Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Storage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${StorageUtil.formatBytes(storageInfo.freeBytes)} free of ${StorageUtil.formatBytes(storageInfo.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                FilledTonalIconButton(
                    onClick = onRefreshScan,
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = ArchivePurpleContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Scan Archives",
                        tint = ArchivePurpleText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Ultra-Thin Aligned Storage Micro-Bar (8 dp height)
            val total = storageInfo.totalBytes.coerceAtLeast(1L).toFloat()
            val usedFraction = (storageInfo.usedBytes.toFloat() / total).coerceIn(0f, 1f)
            val reclaimableFraction = (storageInfo.reclaimableArchiveBytes.toFloat() / total).coerceIn(0f, 1f)
            val freeFraction = (1f - usedFraction).coerceAtLeast(0f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HatchSurfaceVariant)
            ) {
                val baseUsedFraction = (usedFraction - reclaimableFraction).coerceAtLeast(0f)
                if (baseUsedFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(baseUsedFraction.coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .background(StorageUsedGray)
                    )
                }
                if (reclaimableFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(reclaimableFraction.coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .background(ArchivePurple)
                    )
                }
                if (freeFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(freeFraction.coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .background(EmeraldGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Three Equal-Width Aligned Metric Columns with Shared Baselines
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageMetricColumn(
                    modifier = Modifier.weight(1f),
                    color = StorageUsedGray,
                    label = "USED",
                    value = StorageUtil.formatBytes(storageInfo.usedBytes)
                )
                StorageMetricColumn(
                    modifier = Modifier.weight(1f),
                    color = ArchivePurple,
                    label = "ARCHIVES",
                    value = StorageUtil.formatBytes(storageInfo.reclaimableArchiveBytes)
                )
                StorageMetricColumn(
                    modifier = Modifier.weight(1f),
                    color = EmeraldGreen,
                    label = "FREE",
                    value = StorageUtil.formatBytes(storageInfo.freeBytes)
                )
            }

            // 4. Compact Inline Reclaim Banner (Single Line, 32 dp height)
            if (storageInfo.reclaimableArchiveBytes > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = ArchivePurpleContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✦",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArchivePurpleText
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hatching can reclaim up to ${StorageUtil.formatBytes(storageInfo.reclaimableArchiveBytes)} without 2× storage",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArchivePurpleText,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageMetricColumn(
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
