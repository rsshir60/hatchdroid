package dev.hatchzip.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hatchzip.android.ui.theme.ArchivePurple
import dev.hatchzip.android.util.StorageUtil
import dev.hatchzip.android.viewmodel.FilterChip
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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
        // Quick-Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = activeFilter == FilterChip.ALL_FOLDERS,
                    onClick = { onFilterChange(FilterChip.ALL_FOLDERS) },
                    label = { Text("All Folders") },
                    leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null, Modifier.size(16.dp)) }
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == FilterChip.LARGE_ARCHIVES,
                    onClick = { onFilterChange(FilterChip.LARGE_ARCHIVES) },
                    label = { Text("Large (>1 GB)") },
                    leadingIcon = { Icon(Icons.Outlined.FolderZip, contentDescription = null, Modifier.size(16.dp)) }
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == FilterChip.SPLIT_VOLUMES,
                    onClick = { onFilterChange(FilterChip.SPLIT_VOLUMES) },
                    label = { Text("Split Volumes (.001)") },
                    leadingIcon = { Icon(Icons.Outlined.Layers, contentDescription = null, Modifier.size(16.dp)) }
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == FilterChip.DOWNLOADS,
                    onClick = { onFilterChange(FilterChip.DOWNLOADS) },
                    label = { Text("Downloads") },
                    leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null, Modifier.size(16.dp)) }
                )
            }
        }

        // Current Path Navigation Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentDirectory.parentFile != null) {
                    IconButton(
                        onClick = onNavigateUp,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Up", Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = currentDirectory.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Files List
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No files found in this directory",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(files, key = { it.absolutePath }) { file ->
                    FileListItem(
                        file = file,
                        onClick = {
                            if (file.isDirectory) {
                                onNavigateToDir(file)
                            } else if (StorageUtil.isArchive(file)) {
                                onArchiveSelected(file)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileListItem(
    file: File,
    onClick: () -> Unit
) {
    val isArchive = StorageUtil.isArchive(file)
    val isDir = file.isDirectory

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isArchive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
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
                    isDir -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = if (isDir) {
                        "${file.list()?.size ?: 0} items"
                    } else {
                        StorageUtil.formatBytes(file.length())
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isArchive) {
                FilledTonalButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Hatch", style = MaterialTheme.typography.labelSmall)
                }
            } else if (isDir) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
