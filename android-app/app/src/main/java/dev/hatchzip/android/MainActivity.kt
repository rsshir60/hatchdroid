package dev.hatchzip.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.hatchzip.android.ui.screens.*
import dev.hatchzip.android.ui.theme.HatchZipTheme
import dev.hatchzip.android.util.ContentUriPathResolver
import dev.hatchzip.android.util.PermissionUtil
import dev.hatchzip.android.viewmodel.ExtractionUiPhase
import dev.hatchzip.android.viewmodel.ExtractionViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: ExtractionViewModel by viewModels()

    // Hoisted to class level so onResume() can write it and trigger Compose recomposition.
    private val hasPermission = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15 mandatory edge-to-edge layout
        enableEdgeToEdge()

        // Initialise before setContent so the first frame is correct.
        hasPermission.value = PermissionUtil.hasAllFilesAccess()

        handleIncomingArchiveIntent(intent)

        setContent {
            HatchZipTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val permissionGranted by hasPermission
                    var currentScreen by remember { mutableStateOf("home") }

                    val uiState by viewModel.uiState.collectAsState()

                    if (!permissionGranted) {
                        PermissionScreen(
                            onPermissionGranted = {
                                hasPermission.value = true
                                viewModel.scanStorage()
                                viewModel.loadDirectory(File("/storage/emulated/0"))
                            }
                        )
                    } else {
                        // If an extraction is actively running, show ExtractionScreen
                        val isExtracting = uiState.phase is ExtractionUiPhase.Extracting ||
                                uiState.phase is ExtractionUiPhase.Paused ||
                                uiState.phase is ExtractionUiPhase.Finished ||
                                uiState.phase is ExtractionUiPhase.Failed

                        when {
                            isExtracting -> {
                                ExtractionScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        viewModel.dismissPreFlight()
                                    }
                                )
                            }
                            currentScreen == "settings" -> {
                                SettingsAboutScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = "home" }
                                )
                            }
                            else -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { currentScreen = "settings" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check every time the user returns to the app (e.g. from Settings grant screen).
        val granted = PermissionUtil.hasAllFilesAccess()
        if (granted && !hasPermission.value) {
            // Permission was just granted — update state so Compose navigates away from
            // PermissionScreen on the next recomposition.
            hasPermission.value = true
            viewModel.loadDirectory(File("/storage/emulated/0"))
        }
        if (granted) {
            viewModel.scanStorage()
            viewModel.detectInterruptedArchives()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingArchiveIntent(intent)
    }

    private fun handleIncomingArchiveIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null) {
                val path = ContentUriPathResolver.resolvePosixPath(this, uri)
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        viewModel.selectArchive(file)
                    }
                } else {
                    android.util.Log.w("HatchDroid", "Unable to resolve direct POSIX path for URI: $uri")
                }
            }
        }
    }
}
