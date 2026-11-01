package dev.hatchzip.android.ui.components

import androidx.compose.foundation.background
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Device Storage",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${StorageUtil.formatBytes(storageInfo.freeBytes)} available of ${StorageUtil.formatBytes(storageInfo.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalIconButton(
                    onClick = onRefreshScan,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Scan Archives",
                        tint = ArchivePurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Storage Bar
            val total = storageInfo.totalBytes.coerceAtLeast(1L).toFloat()
            val usedFraction = (storageInfo.usedBytes.toFloat() / total).coerceIn(0f, 1f)
            val reclaimableFraction = (storageInfo.reclaimableArchiveBytes.toFloat() / total).coerceIn(0f, 1f)
            val freeFraction = (1f - usedFraction).coerceAtLeast(0f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Used Space (excluding reclaimable archives)
                val baseUsedFraction = (usedFraction - reclaimableFraction).coerceAtLeast(0f)
                if (baseUsedFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(baseUsedFraction.coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .background(StorageUsedGray)
                    )
                }
                // Reclaimable Archive Space (Highlighted Purple)
                if (reclaimableFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(reclaimableFraction.coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .background(ArchivePurple)
                    )
                }
                // Free Space (Emerald Green)
                if (freeFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(freeFraction.coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .background(EmeraldGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend & Reclaimable Metric
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageLegendItem(
                    color = StorageUsedGray,
                    label = "Used",
                    value = StorageUtil.formatBytes(storageInfo.usedBytes)
                )
                StorageLegendItem(
                    color = ArchivePurple,
                    label = "Archives",
                    value = StorageUtil.formatBytes(storageInfo.reclaimableArchiveBytes)
                )
                StorageLegendItem(
                    color = EmeraldGreen,
                    label = "Free",
                    value = StorageUtil.formatBytes(storageInfo.freeBytes)
                )
            }

            if (storageInfo.reclaimableArchiveBytes > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = ArchivePurpleContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ArchivePurple)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hatching will reclaim up to ${StorageUtil.formatBytes(storageInfo.reclaimableArchiveBytes)} without double storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArchivePurpleText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
