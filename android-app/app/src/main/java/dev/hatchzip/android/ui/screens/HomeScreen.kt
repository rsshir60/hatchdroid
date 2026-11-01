package dev.hatchzip.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hatchzip.android.ui.components.*
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.viewmodel.ExtractionUiState
import dev.hatchzip.android.viewmodel.ExtractionViewModel
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ExtractionViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Predictive Back Handling (Android 15+)
    // Swiping back navigates up the directory hierarchy if not at root
    val canNavigateUp = uiState.currentDirectory.parentFile != null &&
            uiState.currentDirectory.path != "/storage/emulated/0"

    PredictiveBackHandler(enabled = canNavigateUp || uiState.selectedArchive != null) { progress ->
        try {
            progress.collect { /* Animated preview */ }
            if (uiState.selectedArchive != null) {
                viewModel.dismissPreFlight()
            } else if (canNavigateUp) {
                viewModel.navigateUp()
            }
        } catch (e: Exception) {
            // Cancelled
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HatchBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HatchDroid",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HatchBackground
                ),
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {
            // 1. High-Priority Crash-Safe Resume Banner
            if (uiState.interruptedArchives.isNotEmpty()) {
                val firstInterrupted = uiState.interruptedArchives.first()
                ResumeBanner(
                    interruptedArchivePath = firstInterrupted,
                    onResumeClick = { viewModel.resumeExtraction(it) },
                    onRestoreClick = { viewModel.restoreArchive(it) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 2. Storage Telemetry Card (Reduced 25-35% footprint)
            StorageTelemetryCard(
                storageInfo = uiState.storageInfo,
                onRefreshScan = { viewModel.scanStorage() },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 3. File Browser & Quick Filter Chips
            FileBrowserView(
                currentDirectory = uiState.currentDirectory,
                files = uiState.directoryFiles,
                activeFilter = uiState.activeFilterChip,
                onFilterChange = { viewModel.setFilterChip(it) },
                onNavigateToDir = { viewModel.navigateToDirectory(it) },
                onNavigateUp = { viewModel.navigateUp() },
                onArchiveSelected = { viewModel.selectArchive(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Password Prompt Sheet (Header decryption during archive inspection)
    uiState.passwordDialog?.let { dialogState ->
        PasswordPromptSheet(
            state = dialogState,
            onSubmit = { password -> viewModel.submitPassword(password) },
            onDismiss = { viewModel.dismissPasswordDialog() }
        )
    }

    // Pre-Flight Modal Bottom Sheet
    if (uiState.selectedArchive != null) {
        val archive = uiState.selectedArchive!!
        PreFlightBottomSheet(
            archive = archive,
            plan = uiState.spacePlan,
            extractionMode = uiState.extractionMode,
            conflictResolution = uiState.conflictResolution,
            keepFreeBufferGb = uiState.keepFreeBufferGb,
            verifyFirst = uiState.verifyFirst,
            onExtractionModeChange = { viewModel.setExtractionMode(it) },
            onConflictResolutionChange = { viewModel.setConflictResolution(it) },
            onKeepFreeChange = { viewModel.setKeepFreeBuffer(it) },
            onVerifyFirstChange = { viewModel.setVerifyFirst(it) },
            onConfirmExtract = { viewModel.startExtraction() },
            onDismiss = { viewModel.dismissPreFlight() }
        )
    }
}
