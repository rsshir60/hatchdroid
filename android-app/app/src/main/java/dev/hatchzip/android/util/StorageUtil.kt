package dev.hatchzip.android.util

import android.os.Environment
import android.os.StatFs
import java.io.File
import java.util.Locale

data class DeviceStorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val reclaimableArchiveBytes: Long,
)

object StorageUtil {

    private val ARCHIVE_EXTENSIONS = setOf(
        "zip", "7z", "rar", "tar", "gz", "tgz"
    )

    fun formatBytes(bytes: Long): String {
        if (bytes < 1000) return "$bytes B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1000.0 && unitIndex < units.size - 1) {
            value /= 1000.0
            unitIndex++
        }
        return String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }

    fun isArchive(file: File): Boolean {
        if (!file.isFile) return false
        val name = file.name.lowercase(Locale.ROOT)
        if (ARCHIVE_EXTENSIONS.any { name.endsWith(".$it") }) return true
        if (name.endsWith(".tar.gz")) return true
        // Split archive formats: .001, .part1.rar, .part01.rar, .z01
        if (Regex(""".*\.\d{3}$""").matches(name)) return true
        if (Regex(""".*\.part\d+\.rar$""").matches(name)) return true
        if (Regex(""".*\.z\d{2}$""").matches(name)) return true
        return false
    }

    fun getDeviceStorage(): DeviceStorageInfo {
        return try {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val total = totalBlocks * blockSize
            val free = availableBlocks * blockSize
            val used = (total - free).coerceAtLeast(0)

            DeviceStorageInfo(
                totalBytes = total,
                freeBytes = free,
                usedBytes = used,
                reclaimableArchiveBytes = 0L // Populated asynchronously by scanner
            )
        } catch (e: Exception) {
            DeviceStorageInfo(0L, 0L, 0L, 0L)
        }
    }

    fun scanReclaimableBytes(roots: List<File>): Long {
        var reclaimable = 0L
        for (root in roots) {
            if (root.exists() && root.isDirectory) {
                root.walkTopDown()
                    .maxDepth(3)
                    .filter { it.isFile && isArchive(it) }
                    .forEach { reclaimable += it.length() }
            }
        }
        return reclaimable
    }
}
