package dev.hatchzip.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hatchzip.android.ui.components.DualProgressCard
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.util.StorageUtil
import dev.hatchzip.android.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionScreen(
    viewModel: ExtractionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAbortDialog by remember { mutableStateOf(false) }

    // Intercept back button when extracting so user doesn't accidentally disconnect
    BackHandler(enabled = uiState.phase == ExtractionUiPhase.Extracting) {
        showAbortDialog = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.phase) {
                            is ExtractionUiPhase.Finished -> "Extraction Complete"
                            is ExtractionUiPhase.Failed -> "Extraction Error"
                            is ExtractionUiPhase.Paused -> "Extraction Paused"
                            else -> "In-Place Extraction"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.phase == ExtractionUiPhase.Extracting) {
                            showAbortDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val phase = uiState.phase) {
                is ExtractionUiPhase.Extracting, is ExtractionUiPhase.Paused -> {
                    val archiveName = uiState.selectedArchive?.displayName ?: "Archive"

                    DualProgressCard(
                        archiveName = archiveName,
                        mode = uiState.extractionMode,
                        originalArchiveSize = uiState.originalArchiveSize,
                        currentArchiveSize = uiState.currentArchiveSize,
                        extractedBytes = uiState.extractedBytes,
                        totalOutputBytes = uiState.totalOutputBytes,
                        freeSpaceFloor = uiState.freeSpaceFloor,
                        currentFreeSpace = uiState.currentFreeSpace,
                        currentFileName = uiState.currentFileName,
                        isPaused = phase is ExtractionUiPhase.Paused,
                        onPauseClick = { viewModel.pauseExtraction() },
                        onResumeClick = { viewModel.resumeExtraction() },
                        onAbortClick = { showAbortDialog = true }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Detail telemetry card
                    OutlinedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Telemetry & Disk Operations",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            TelemetryRow(
                                label = "Extraction Engine",
                                value = "hatchzip-core (aarch64 POSIX)"
                            )
                            TelemetryRow(
                                label = "Destination",
                                value = uiState.outputDirectoryOverride ?: uiState.selectedArchive?.defaultOutputDir ?: "/storage/emulated/0"
                            )
                            TelemetryRow(
                                label = "Verification Mode",
                                value = if (uiState.verifyFirst) "Full Pre-Scan" else "Batch In-Flight"
                            )
                            TelemetryRow(
                                label = "Truncation Primitives",
                                value = "ftruncate + fallocate (ext4/f2fs)"
                            )
                        }
                    }
                }

                is ExtractionUiPhase.Finished -> {
                    Surface(
                        color = EmeraldGreenContainer,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldGreenText,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Hatched Successfully!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = EmeraldGreenText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All ${uiState.selectedArchive?.itemCount ?: 0} files extracted. Source archive space was safely reclaimed with zero excess storage required.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldGreenText
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onNavigateBack,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Done", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                is ExtractionUiPhase.Failed -> {
                    Surface(
                        color = CoralRedContainer,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = CoralRedText,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Extraction Failed",
                                style = MaterialTheme.typography.headlineSmall,
                                color = CoralRedText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = phase.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoralRedText
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row {
                                OutlinedButton(
                                    onClick = onNavigateBack,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Back")
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = { viewModel.resumeExtraction() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CoralRed
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    // Abort Confirmation Dialog
    if (showAbortDialog) {
        AlertDialog(
            onDismissRequest = { showAbortDialog = false },
            title = { Text("Pause or Revert Extraction?") },
            text = {
                Text("HatchZip is designed to be crash-safe. Pausing saves your state so you can resume later. Reverting reconstructs the archive back to its standard format.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAbortDialog = false
                        viewModel.pauseExtraction()
                        onNavigateBack()
                    }
                ) {
                    Text("Pause & Save State")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAbortDialog = false
                        viewModel.abortAndRestoreArchive()
                        onNavigateBack()
                    }
                ) {
                    Text("Revert Archive")
                }
            }
        )
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
