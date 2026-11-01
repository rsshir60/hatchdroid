package dev.hatchzip.android.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Environment
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.hatchzip.android.service.ExtractionForegroundService
import dev.hatchzip.android.util.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.hatchzip_android.*
import java.io.File

class ExtractionViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ExtractionUiState())
    val uiState = _uiState.asStateFlow()

    // Service connection
    private var serviceBinder: ExtractionForegroundService.LocalBinder? = null
    private var isServiceBound = false

    // Native extraction cancellation handle
    private var currentHandle: ExtractionHandle? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            serviceBinder = binder as? ExtractionForegroundService.LocalBinder
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBinder = null
            isServiceBound = false
        }
    }

    init {
        bindForegroundService()
        scanStorage()
        loadDirectory(File("/storage/emulated/0"))
        detectInterruptedArchives()
    }

    private fun bindForegroundService() {
        val intent = Intent(getApplication(), ExtractionForegroundService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun scanStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            val baseInfo = StorageUtil.getDeviceStorage()
            _uiState.update { it.copy(storageInfo = baseInfo, isScanning = true) }

            // Scan primary download and document folders for archives
            val roots = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                File("/storage/emulated/0/Download")
            )
            val reclaimable = StorageUtil.scanReclaimableBytes(roots)

            _uiState.update {
                it.copy(
                    storageInfo = baseInfo.copy(reclaimableArchiveBytes = reclaimable),
                    isScanning = false
                )
            }
        }
    }

    fun detectInterruptedArchives() {
        viewModelScope.launch(Dispatchers.IO) {
            val dirsToScan = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                "/storage/emulated/0/Download",
                "/storage/emulated/0"
            )
            val found = mutableListOf<String>()
            for (dir in dirsToScan) {
                try {
                    found.addAll(findInterruptedJobs(dir))
                } catch (e: Exception) {
                    android.util.Log.e("HatchZip", "Error detecting interrupted jobs in $dir", e)
                }
            }
            _uiState.update { it.copy(interruptedArchives = found.distinct()) }
        }
    }

    fun loadDirectory(dir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val files = if (dir.exists() && dir.isDirectory) {
                val list = dir.listFiles()?.toList() ?: emptyList()
                sortAndFilterFiles(list, _uiState.value.activeFilterChip)
            } else {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    currentDirectory = dir,
                    directoryFiles = files
                )
            }
        }
    }

    fun navigateToDirectory(dir: File) {
        loadDirectory(dir)
    }

    fun navigateUp() {
        val parent = _uiState.value.currentDirectory.parentFile
        if (parent != null && parent.canRead()) {
            loadDirectory(parent)
        }
    }

    fun setFilterChip(chip: FilterChip) {
        _uiState.update { it.copy(activeFilterChip = chip) }
        loadDirectory(_uiState.value.currentDirectory)
    }

    private fun sortAndFilterFiles(files: List<File>, filter: FilterChip): List<File> {
        val filtered = when (filter) {
            FilterChip.ALL_FOLDERS -> files
            FilterChip.LARGE_ARCHIVES -> files.filter { it.isDirectory || (StorageUtil.isArchive(it) && it.length() >= 1024L * 1024 * 1024) }
            FilterChip.SPLIT_VOLUMES -> files.filter {
                it.isDirectory || (it.name.matches(Regex(""".*\.\d{3}$""")) || it.name.contains(".part"))
            }
            FilterChip.DOWNLOADS -> files
        }

        // Directories first, then files sorted alphabetically
        return filtered.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun selectArchive(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(phase = ExtractionUiPhase.Checking) }

            try {
                val path = file.absolutePath
                if (path.startsWith("content://")) {
                    throw IllegalArgumentException(
                        "content:// URIs are not supported. Only direct POSIX file paths (/storage/emulated/0/...) can be manipulated in place."
                    )
                }
                if (!file.exists()) {
                    throw IllegalArgumentException("Archive file does not exist: $path")
                }

                // Call native Rust pre-flight inspection
                val info = checkArchive(path)

                val archiveDetails = ArchiveDetails(
                    path = path,
                    fileName = file.name,
                    format = info.format,
                    itemCount = info.itemCount.toLong(),
                    totalSize = info.totalSize.toLong(),
                    isEncrypted = info.encrypted,
                    isSingleRoot = info.singleRoot,
                    displayName = info.displayName,
                    defaultOutputDir = info.defaultOutputDir,
                    inPlaceRefusal = info.inPlaceRefusal
                )

                val keepFreeBytes = (_uiState.value.keepFreeBufferGb * 1024 * 1024 * 1024).toLong()
                val keepArchive = _uiState.value.extractionMode == ExtractionMode.NORMAL_KEEP
                val outputDir = _uiState.value.outputDirectoryOverride ?: info.defaultOutputDir

                // Call native Rust pre-flight space planner
                val plan = planExtraction(
                    path = path,
                    outputDir = outputDir,
                    keepFreeBytes = keepFreeBytes.toULong(),
                    keepArchive = keepArchive
                )

                val spacePlan = SpacePlanDetails(
                    freeBytes = plan.freeBytes.toLong(),
                    neededBytes = plan.neededBytes.toLong(),
                    keepFreeBytes = plan.keepFreeBytes.toLong(),
                    fits = plan.fits,
                    freeAfterBytes = plan.freeAfterBytes.toLong(),
                    keepArchive = plan.keepArchive,
                    inPlaceNeeds = plan.inPlaceNeeds.toLong(),
                    normalNeeds = plan.normalNeeds.toLong(),
                    reclaims = plan.reclaims
                )

                _uiState.update {
                    it.copy(
                        selectedArchive = archiveDetails,
                        spacePlan = spacePlan,
                        phase = ExtractionUiPhase.Ready
                    )
                }
            } catch (e: HatchZipException) {
                val msg = e.message ?: "Archive check failed"
                _uiState.update {
                    it.copy(
                        phase = ExtractionUiPhase.Failed(msg),
                        errorMessage = msg
                    )
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to open archive"
                _uiState.update {
                    it.copy(
                        phase = ExtractionUiPhase.Failed(msg),
                        errorMessage = msg
                    )
                }
            }
        }
    }

    fun dismissPreFlight() {
        _uiState.update { it.copy(selectedArchive = null, spacePlan = null, phase = ExtractionUiPhase.Idle) }
    }

    fun setExtractionMode(mode: ExtractionMode) {
        _uiState.update { it.copy(extractionMode = mode) }
    }

    fun setConflictResolution(resolution: ConflictResolution) {
        _uiState.update { it.copy(conflictResolution = resolution) }
    }

    fun setKeepFreeBuffer(bufferGb: Float) {
        _uiState.update { it.copy(keepFreeBufferGb = bufferGb) }
    }

    fun setVerifyFirst(verify: Boolean) {
        _uiState.update { it.copy(verifyFirst = verify) }
    }

    fun startExtraction() {
        // Enforce MANAGE_EXTERNAL_STORAGE permission before native disk operations
        if (!Environment.isExternalStorageManager()) {
            _uiState.update {
                it.copy(
                    phase = ExtractionUiPhase.Failed(
                        "MANAGE_EXTERNAL_STORAGE permission is required to manipulate and truncate archives in place. Please grant All Files Access."
                    )
                )
            }
            return
        }

        val archive = _uiState.value.selectedArchive ?: return
        val plan = _uiState.value.spacePlan ?: return

        _uiState.update {
            it.copy(
                phase = ExtractionUiPhase.Extracting,
                originalArchiveSize = archive.totalSize,
                currentArchiveSize = archive.totalSize,
                extractedBytes = 0L,
                totalOutputBytes = archive.totalSize,
                currentFreeSpace = plan.freeBytes,
                freeSpaceFloor = plan.keepFreeBytes
            )
        }

        val service = serviceBinder?.getService()
        service?.startForegroundExtraction(
            archivePath = archive.path,
            originalArchiveSize = archive.totalSize,
            totalOutputSize = archive.totalSize,
            onCancel = { pauseExtraction() }
        )

        viewModelScope.launch(Dispatchers.IO) {
            executeNativeExtraction(archive)
        }
    }

    private suspend fun executeNativeExtraction(archive: ArchiveDetails) {
        withContext(Dispatchers.IO) {
            val service = serviceBinder?.getService()
            val handle = ExtractionHandle()
            currentHandle = handle

            val keepFreeBytes = (_uiState.value.keepFreeBufferGb * 1024 * 1024 * 1024).toLong()
            val keepArchive = _uiState.value.extractionMode == ExtractionMode.NORMAL_KEEP
            val outputDir = _uiState.value.outputDirectoryOverride ?: archive.defaultOutputDir
            val password = _uiState.value.passwordInput

            val callback = object : ProgressCallback {
                override fun onBytesExtracted(bytes: ULong) {
                    val extracted = bytes.toLong()
                    _uiState.update { it.copy(extractedBytes = extracted) }
                    service?.updateProgress(
                        archiveName = archive.fileName,
                        currentFile = _uiState.value.currentFileName,
                        extractedBytes = extracted,
                        totalOutputBytes = archive.totalSize,
                        originalArchiveSize = archive.totalSize,
                        currentArchiveSize = _uiState.value.currentArchiveSize
                    )
                }

                override fun onArchiveShrunk(newArchiveLen: ULong) {
                    val newLen = newArchiveLen.toLong()
                    val currentFree = StorageUtil.getDeviceStorage().freeBytes
                    _uiState.update {
                        it.copy(
                            currentArchiveSize = newLen,
                            currentFreeSpace = currentFree
                        )
                    }
                }

                override fun onPhaseChanged(phase: ExtractionPhase) {
                    val uiPhase = when (phase) {
                        is ExtractionPhase.Checking -> ExtractionUiPhase.Checking
                        is ExtractionPhase.Planning -> ExtractionUiPhase.Planning
                        is ExtractionPhase.Extracting -> ExtractionUiPhase.Extracting
                        is ExtractionPhase.Paused -> ExtractionUiPhase.Paused
                        is ExtractionPhase.Finished -> ExtractionUiPhase.Finished
                        is ExtractionPhase.Failed -> ExtractionUiPhase.Failed(phase.message)
                    }
                    _uiState.update { it.copy(phase = uiPhase) }
                }

                override fun onError(message: String) {
                    android.util.Log.e("HatchZip", "Rust extraction error: $message")
                }
            }

            try {
                extractArchive(
                    handle = handle,
                    path = archive.path,
                    outputDir = outputDir,
                    keepFreeBytes = keepFreeBytes.toULong(),
                    keepArchive = keepArchive,
                    password = password,
                    callback = callback
                )

                // Verify output folder has contents unless paused or failed
                if (_uiState.value.phase !is ExtractionUiPhase.Paused && _uiState.value.phase !is ExtractionUiPhase.Failed) {
                    val outDir = File(outputDir)
                    if (outDir.exists() && (outDir.listFiles()?.isNotEmpty() == true)) {
                        _uiState.update { it.copy(phase = ExtractionUiPhase.Finished) }
                    } else {
                        _uiState.update {
                            it.copy(phase = ExtractionUiPhase.Failed("Extraction completed, but no files were written to $outputDir"))
                        }
                    }
                }
            } catch (e: HatchZipException) {
                android.util.Log.e("HatchZip", "Native extraction failed", e)
                _uiState.update { it.copy(phase = ExtractionUiPhase.Failed(e.message ?: "Extraction failed")) }
            } catch (e: Exception) {
                android.util.Log.e("HatchZip", "Extraction error", e)
                _uiState.update { it.copy(phase = ExtractionUiPhase.Failed(e.message ?: "Extraction error")) }
            } finally {
                service?.stopExtraction()
                currentHandle = null
                scanStorage()
                detectInterruptedArchives()
            }
        }
    }

    fun pauseExtraction() {
        currentHandle?.cancel()
        serviceBinder?.getService()?.pauseExtraction()
    }

    fun resumeExtraction(archivePath: String? = null) {
        val path = archivePath ?: _uiState.value.selectedArchive?.path
        if (path != null) {
            selectArchive(File(path))
            startExtraction()
        }
    }

    fun abortAndRestoreArchive() {
        currentHandle?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            val archive = _uiState.value.selectedArchive
            if (archive != null) {
                try {
                    uniffi.hatchzip_android.restoreArchive(archive.path, null)
                } catch (e: Exception) {
                    android.util.Log.e("HatchZip", "Abort-restore failed", e)
                }
            }
            _uiState.update { it.copy(phase = ExtractionUiPhase.Idle, selectedArchive = null) }
            detectInterruptedArchives()
        }
    }

    fun restoreArchive(archivePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = uniffi.hatchzip_android.restoreArchive(archivePath, null)
                android.util.Log.i("HatchZip", "Restored $count entries for $archivePath")
            } catch (e: Exception) {
                android.util.Log.e("HatchZip", "Restore failed: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Restore failed: ${e.message}") }
            }
            detectInterruptedArchives()
        }
    }

    override fun onCleared() {
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
        }
        super.onCleared()
    }
}
