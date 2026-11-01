package dev.hatchzip.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.viewmodel.ExtractionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAboutScreen(
    viewModel: ExtractionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HatchBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & About",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Storage Safeguards Section
            SectionHeader("STORAGE SAFEGUARDS")

            Surface(
                color = Color(0xFF121218),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222230)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Buffer Slider Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Safety Space Floor",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = String.format("%.1f GB", uiState.keepFreeBufferGb),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = uiState.keepFreeBufferGb,
                        onValueChange = { viewModel.setKeepFreeBuffer(it) },
                        valueRange = 1.0f..5.0f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = HatchPrimary,
                            activeTrackColor = HatchPrimary
                        )
                    )

                    Text(
                        text = "Protects Android system caches, SQLite WAL logs, and apps by halting before disk free space drops below this threshold.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF222230))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Pre-verify Checksums Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pre-Verify Archive Integrity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Validates CRC32 checksums of all entry headers prior to committing disk writes.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                        }
                        Switch(
                            checked = uiState.verifyFirst,
                            onCheckedChange = { viewModel.setVerifyFirst(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Algorithm & Architecture Section
            SectionHeader("THE 2× STORAGE TRAP")

            Surface(
                color = Color(0xFF121218),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222230)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CoralRed.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Storage,
                                    contentDescription = null,
                                    tint = CoralRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Why Standard Unzippers Fail",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Standard extractors need both the full archive and uncompressed files on storage simultaneously—demanding 2× to 3× free space. A 20 GB archive fails on a device with 15 GB free.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldGreen.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "HatchDroid Tail-First In-Place Reclamation",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Extracts entries in reverse order from the end of the archive, immediately truncating freed archive blocks via Linux ftruncate. You only need space for a small safety buffer.",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGreenText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Engine & Hardware Telemetry
            SectionHeader("ENGINE & HARDWARE TELEMETRY")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HardwarePill(
                    icon = Icons.Outlined.Memory,
                    title = "16 KB Aligned",
                    subtitle = "Android 15+ ELFs",
                    modifier = Modifier.weight(1f)
                )
                HardwarePill(
                    icon = Icons.Outlined.WifiOff,
                    title = "100% Offline",
                    subtitle = "Zero Network IPC",
                    modifier = Modifier.weight(1f)
                )
                HardwarePill(
                    icon = Icons.Outlined.FolderCopy,
                    title = "FUSE Resilient",
                    subtitle = "Android Storage",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Attribution & Credits
            SectionHeader("ATTRIBUTION & CREDITS")

            Surface(
                color = Color(0xFF121218),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222230)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    CreditRow(
                        role = "Android Architecture & Port",
                        author = "@rsshir60",
                        description = "Jetpack Compose UI, UniFFI JNI bridge, Android 15 16 KB page alignment, FUSE resilience, and 60 Hz throttled telemetry."
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF222230))
                    Spacer(modifier = Modifier.height(10.dp))

                    CreditRow(
                        role = "Core Algorithm & Rust Engine",
                        author = "xevrion / hatchzip",
                        description = "Original Rust hatchzip-core engine, tail-first extraction protocol, and zero-loss crash journal state machine."
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = "HatchDroid v0.1.0 • aarch64-linux-android • 16 KB ELF Page-Aligned",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = TextTertiary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        ),
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun HardwarePill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF14141C),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222230)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HatchPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun CreditRow(
    role: String,
    author: String,
    description: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = role,
                style = MaterialTheme.typography.labelSmall,
                color = HatchPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = author,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
