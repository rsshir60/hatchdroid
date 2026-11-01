package dev.hatchzip.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import dev.hatchzip.android.MainActivity
import dev.hatchzip.android.R
import dev.hatchzip.android.util.StorageUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

class ExtractionForegroundService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastNotificationUpdateMs: Long = 0L

    // Callbacks & state observation
    private val _serviceProgress = MutableStateFlow(ServiceProgressData())
    val serviceProgress = _serviceProgress.asStateFlow()

    private var activeCancellationBlock: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): ExtractionForegroundService = this@ExtractionForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PAUSE) {
            pauseExtraction()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    fun startForegroundExtraction(
        archivePath: String,
        originalArchiveSize: Long,
        totalOutputSize: Long,
        onCancel: () -> Unit
    ) {
        activeCancellationBlock = onCancel

        // Acquire 45-minute scoped partial wake lock preventing battery drain if abandoned
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HatchDroid::InPlaceExtractionWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(45 * 60 * 1000L)
        }

        lastNotificationUpdateMs = 0L

        val notification = buildNotification(
            archiveName = File(archivePath).name,
            progressText = "Preparing in-place extraction...",
            percent = 0,
            reclaimedText = "0 B reclaimed",
            indeterminate = false
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun updateProgress(
        archiveName: String,
        currentFile: String,
        extractedBytes: Long,
        totalOutputBytes: Long,
        originalArchiveSize: Long,
        currentArchiveSize: Long,
        speedBytesPerSec: Double = 0.0,
        phaseDescription: String? = null
    ) {
        val percent = if (totalOutputBytes > 0) {
            ((extractedBytes.toFloat() / totalOutputBytes.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else 0

        val reclaimed = (originalArchiveSize - currentArchiveSize).coerceAtLeast(0L)
        val speedText = if (speedBytesPerSec > 64 * 1024) " • ${StorageUtil.formatBytes(speedBytesPerSec.toLong())}/s" else ""
        val phaseLabel = phaseDescription ?: "Hatching"

        val reclaimedGb = reclaimed / (1024.0 * 1024.0 * 1024.0)
        val hatchedGb = extractedBytes / (1024.0 * 1024.0 * 1024.0)
        val dualStatus = String.format(Locale.US, "🔻 %.2f GB freed  •  🔺 %.2f GB hatched", reclaimedGb, hatchedGb)

        // 1. Immediately emit to 60 Hz StateFlow for smooth Compose UI rendering
        _serviceProgress.value = ServiceProgressData(
            archiveName = archiveName,
            currentFile = currentFile,
            extractedBytes = extractedBytes,
            totalOutputBytes = totalOutputBytes,
            originalArchiveSize = originalArchiveSize,
            currentArchiveSize = currentArchiveSize,
            percent = percent,
            speedBytesPerSec = speedBytesPerSec,
            phaseDescription = phaseLabel
        )

        // 2. Rate-limit NotificationManager.notify to at most once per 500 ms (2 Hz)
        val now = SystemClock.elapsedRealtime()
        val isTerminal = percent == 100 || percent == 0 ||
                phaseDescription?.contains("Complete", ignoreCase = true) == true ||
                phaseDescription?.contains("Paused", ignoreCase = true) == true ||
                phaseDescription?.contains("Failed", ignoreCase = true) == true

        if (isTerminal || (now - lastNotificationUpdateMs) >= NOTIFICATION_INTERVAL_MS) {
            lastNotificationUpdateMs = now

            val isIndeterminate = phaseDescription?.contains("fsync", ignoreCase = true) == true
            val notification = buildNotification(
                archiveName = archiveName,
                progressText = "$phaseLabel $percent%$speedText • $currentFile",
                percent = percent,
                reclaimedText = dualStatus,
                indeterminate = isIndeterminate
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    fun pauseExtraction() {
        activeCancellationBlock?.invoke()
        stopForeground(STOP_FOREGROUND_REMOVE)
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    fun stopExtraction() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        wakeLock?.let { if (it.isHeld) it.release() }
        stopSelf()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        // 1. Signal cancellation to flush .hatchzip-tail safely before OS kill
        activeCancellationBlock?.invoke()

        // 2. Release wake lock
        wakeLock?.let { if (it.isHeld) it.release() }

        // 3. Detach and stop to prevent Android 15 ForegroundServiceDidNotStopInTimeException
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            stopForeground(false)
        }
        stopSelf(startId)
        super.onTimeout(startId, fgsType)
    }

    private fun buildNotification(
        archiveName: String,
        progressText: String,
        percent: Int,
        reclaimedText: String,
        indeterminate: Boolean = false
    ): Notification {
        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ExtractionForegroundService::class.java).apply {
                action = ACTION_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hatching $archiveName ($percent%)")
            .setContentText(progressText)
            .setSubText(reclaimedText)
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(100, percent, indeterminate)
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .addAction(
                R.drawable.ic_notification,
                "Pause Safely",
                pauseIntent
            )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "hatchzip_extractions"
        const val NOTIFICATION_ID = 4242
        const val ACTION_PAUSE = "dev.hatchzip.android.ACTION_PAUSE"
        const val NOTIFICATION_INTERVAL_MS = 500L // 2 Hz max for Android System UI binder stability
    }
}

data class ServiceProgressData(
    val archiveName: String = "",
    val currentFile: String = "",
    val extractedBytes: Long = 0L,
    val totalOutputBytes: Long = 0L,
    val originalArchiveSize: Long = 0L,
    val currentArchiveSize: Long = 0L,
    val percent: Int = 0,
    val speedBytesPerSec: Double = 0.0,
    val phaseDescription: String = ""
)
