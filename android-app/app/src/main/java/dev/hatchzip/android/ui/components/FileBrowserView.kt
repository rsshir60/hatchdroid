package dev.hatchzip.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.util.StorageUtil
import dev.hatchzip.android.viewmodel.FilterChip
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileBrowserView(
    currentDirectory: File,
    files: List<File>,
    activeFilter: FilterChip,
    onFilterChange: (FilterChip) -> Unit,
    onNavigateToDir: (File) -> Unit,
    onNavigateUp: () -> Unit,
    onArchiveSelected: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Quick-Filter Chips (Compact 34 dp height, subtle fill)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item {
                FilterChipItem(
                    selected = activeFilter == FilterChip.ALL_FOLDERS,
                    onClick = { onFilterChange(FilterChip.ALL_FOLDERS) },
                    label = "All Files",
                    icon = Icons.Outlined.Folder
                )
            }
            item {
                FilterChipItem(
                    selected = activeFilter == FilterChip.LARGE_ARCHIVES,
                    onClick = { onFilterChange(FilterChip.LARGE_ARCHIVES) },
                    label = "Large (>1 GB)",
                    icon = Icons.Outlined.FolderZip
                )
            }
            item {
                FilterChipItem(
                    selected = activeFilter == FilterChip.SPLIT_VOLUMES,
                    onClick = { onFilterChange(FilterChip.SPLIT_VOLUMES) },
                    label = "Split (.001)",
                    icon = Icons.Outlined.Layers
                )
            }
            item {
                FilterChipItem(
                    selected = activeFilter == FilterChip.DOWNLOADS,
                    onClick = { onFilterChange(FilterChip.DOWNLOADS) },
                    label = "Downloads",
                    icon = Icons.Outlined.Download
                )
            }
        }

        // 2. Interactive Breadcrumb Navigation Bar (Solid Explorer Pattern)
        BreadcrumbBar(
            currentDirectory = currentDirectory,
            onNavigateToDir = onNavigateToDir,
            onNavigateUp = onNavigateUp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        // 3. Flat Borderless File List with 0.5 dp dividers (ZArchiver & ES File Explorer Pattern)
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No files found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(files, key = { it.absolutePath }) { file ->
                    FileListItemRow(
                        file = file,
                        onClick = {
                            if (file.isDirectory) {
                                onNavigateToDir(file)
                            } else if (StorageUtil.isArchive(file)) {
                                onArchiveSelected(file)
                            }
                        },
                        onHatchClick = {
                            if (StorageUtil.isArchive(file)) {
                                onArchiveSelected(file)
                            }
                        }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color(0xFF1E1E28)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) HatchPrimary else Color(0xFF181820),
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262633)),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color(0xFF0A0A0C) else TextSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color(0xFF0A0A0C) else TextPrimary
            )
        }
    }
}

@Composable
private fun BreadcrumbBar(
    currentDirectory: File,
    onNavigateToDir: (File) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Auto-scroll to the right whenever directory changes
    LaunchedEffect(currentDirectory.path) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // Build hierarchy segments: root is /storage/emulated/0
    val segments = remember(currentDirectory.path) {
        buildPathSegments(currentDirectory)
    }

    Surface(
        color = Color(0xFF121218),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Up Button
            if (currentDirectory.parentFile != null && currentDirectory.path != "/storage/emulated/0") {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Navigate Up",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Icon(
                    imageVector = Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(16.dp)
                )
            }

            // Scrollable breadcrumbs
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                segments.forEachIndexed { index, seg ->
                    val isLast = index == segments.size - 1

                    Surface(
                        onClick = { onNavigateToDir(seg.file) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isLast) Color(0xFF1C1C26) else Color.Transparent,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = seg.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isLast) TextPrimary else TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    if (!isLast) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF4A4A5A),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class PathSegment(val name: String, val file: File)

private fun buildPathSegments(current: File): List<PathSegment> {
    val segments = mutableListOf<PathSegment>()
    var curr: File? = current

    while (curr != null) {
        val name = when {
            curr.path == "/storage/emulated/0" -> "Storage"
            curr.path == "/" -> "Root"
            curr.name.isEmpty() -> curr.path
            else -> curr.name
        }
        segments.add(0, PathSegment(name, curr))
        if (curr.path == "/storage/emulated/0" || curr.parentFile == null) break
        curr = curr.parentFile
    }

    return segments
}

@Composable
private fun FileListItemRow(
    file: File,
    onClick: () -> Unit,
    onHatchClick: () -> Unit
) {
    val isArchive = StorageUtil.isArchive(file)
    val isDir = file.isDirectory
    val ext = if (isArchive) file.extension.uppercase() else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon with soft container
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isArchive -> ArchivePurple.copy(alpha = 0.15f)
                        isDir -> HatchPrimary.copy(alpha = 0.12f)
                        else -> Color(0xFF1C1C24)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isDir -> Icons.Outlined.Folder
                    isArchive -> Icons.Outlined.FolderZip
                    else -> Icons.AutoMirrored.Outlined.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    isArchive -> ArchivePurple
                    isDir -> HatchPrimary
                    else -> TextSecondary
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center: Dual-Line Title + Subtitle
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Archive Format Badge (e.g. .ZIP, .7Z, .RAR)
                if (isArchive && ext.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ArchivePurple.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = ".$ext",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ArchivePurple,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            val metadataText = remember(file.lastModified(), file.length()) {
                val dateStr = formatFileDate(file.lastModified())
                if (isDir) {
                    val count = file.list()?.size ?: 0
                    "$count items • $dateStr"
                } else {
                    "${StorageUtil.formatBytes(file.length())} • $dateStr"
                }
            }

            Text(
                text = metadataText,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Action: "Hatch" chip for archives, Chevron for directories
        if (isArchive) {
            Button(
                onClick = onHatchClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HatchPrimary,
                    contentColor = Color(0xFF0A0A0C)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "Hatch",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (isDir) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF454556),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatFileDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Unknown"
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
