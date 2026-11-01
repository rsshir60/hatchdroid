package dev.hatchzip.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hatchzip.android.ui.components.LiveExtractionTelemetryCard
import dev.hatchzip.android.ui.components.PasswordPromptSheet
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionScreen(
    viewModel: ExtractionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val liveModel by viewModel.liveProgressState.collectAsState()
    var showAbortDialog by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }

    // Intercept back button when extracting so user doesn't accidentally cancel
    BackHandler(enabled = uiState.phase == ExtractionUiPhase.Extracting) {
        showAbortDialog = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HatchBackground,
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HatchBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val phase = uiState.phase) {
                is ExtractionUiPhase.Extracting, is ExtractionUiPhase.Paused -> {
                    val archiveName = uiState.selectedArchive?.displayName ?: "Archive"

                    // Primary Live Telemetry Card
                    LiveExtractionTelemetryCard(
                        archiveName = archiveName,
                        mode = uiState.extractionMode,
                        model = liveModel,
                        isPaused = phase is ExtractionUiPhase.Paused,
                        onPauseClick = { viewModel.pauseExtraction() },
                        onResumeClick = { viewModel.resumeExtraction() },
                        onAbortClick = { showAbortDialog = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Collapsible Technical Diagnostics
                    Surface(
                        color = Color(0xFF121218),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222230)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDiagnostics = !showDiagnostics }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Terminal,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Technical Diagnostics",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Icon(
                                    imageVector = if (showDiagnostics) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            AnimatedVisibility(visible = showDiagnostics) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF222230))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TelemetryRow(
                                        label = "Native Engine",
                                        value = "hatchzip-core (aarch64 POSIX)"
                                    )
                                    TelemetryRow(
                                        label = "Target Directory",
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
                                    TelemetryRow(
                                        label = "Cancellation Token",
                                        value = "Arc<AtomicBool> cooperative"
                                    )
                                }
                            }
                        }
                    }
                }

                is ExtractionUiPhase.Finished -> {
                    Surface(
                        color = Color(0xFF0F1E17),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
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
                                tint = EmeraldGreen,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Hatched Successfully!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All ${uiState.selectedArchive?.itemCount ?: 0} entries extracted cleanly. Source archive storage was safely reclaimed with zero excess flash footprint.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onNavigateBack,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen,
                                    contentColor = Color(0xFF0A0A0C)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text("Done", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                is ExtractionUiPhase.Failed -> {
                    Surface(
                        color = Color(0xFF1E1014),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.3f)),
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
                                tint = CoralRed,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Extraction Interrupted",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = phase.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoralRedText,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onNavigateBack,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Text("Back", style = MaterialTheme.typography.labelLarge)
                                }
                                Button(
                                    onClick = { viewModel.resumeExtraction() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CoralRed,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Text("Retry", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                        CircularProgressIndicator(color = HatchPrimary)
                    }
                }
            }
        }
    }

    // Password Prompt Sheet (Payload decryption during extraction)
    uiState.passwordDialog?.let { dialogState ->
        PasswordPromptSheet(
            state = dialogState,
            onSubmit = { password -> viewModel.submitPassword(password) },
            onDismiss = { viewModel.dismissPasswordDialog() }
        )
    }

    // Abort Confirmation Dialog
    if (showAbortDialog) {
        AlertDialog(
            onDismissRequest = { showAbortDialog = false },
            containerColor = Color(0xFF14141C),
            title = {
                Text(
                    text = "Pause or Revert Extraction?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "HatchDroid is crash-safe. Pausing saves your state so you can resume later. Reverting reconstructs the original archive from the safety journal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAbortDialog = false
                        viewModel.pauseExtraction()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HatchPrimary,
                        contentColor = Color(0xFF0A0A0C)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Pause & Save", fontWeight = FontWeight.Bold)
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
                    Text("Revert Archive", color = CoralRed)
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
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = TextSecondary
        )
    }
}
