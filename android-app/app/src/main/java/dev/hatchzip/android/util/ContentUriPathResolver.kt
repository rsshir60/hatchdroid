package dev.hatchzip.android.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

object ContentUriPathResolver {

    /**
     * Resolves an Android `file://` or `content://` URI into a direct POSIX path
     * suitable for Rust's `std::fs::OpenOptions` and in-place `ftruncate()`.
     * Returns null if the file lives inside a virtual cloud provider (e.g. Google Drive).
     */
    fun resolvePosixPath(context: Context, uri: Uri): String? {
        // 1. Direct file:// URI
        if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
            val path = uri.path
            if (path != null && File(path).exists()) {
                return path
            }
        }

        if (!ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true)) {
            return null
        }

        // 2. ExternalStorageProvider (content://com.android.externalstorage.documents/document/primary:Download/archive.zip)
        try {
            if (DocumentsContract.isDocumentUri(context, uri) &&
                uri.authority == "com.android.externalstorage.documents"
            ) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                if (split.size >= 2) {
                    val volumeId = split[0]
                    val relativePath = split[1]
                    if ("primary".equals(volumeId, ignoreCase = true)) {
                        val candidate = "${Environment.getExternalStorageDirectory().absolutePath}/$relativePath"
                        if (File(candidate).exists()) return candidate
                    } else {
                        // Removable SD card mount (e.g., /storage/1A2B-3C4D/...)
                        val candidate = "/storage/$volumeId/$relativePath"
                        if (File(candidate).exists()) return candidate
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore DocumentsContract parsing errors and fallback to MediaStore query
        }

        // 3. MediaStore / Downloads / 3rd-party file managers exposing DATA column
        try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )?.use { cursor ->
                val colIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                if (colIndex != -1 && cursor.moveToFirst()) {
                    val path = cursor.getString(colIndex)
                    if (!path.isNullOrBlank() && File(path).exists()) {
                        return path
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore MediaStore query failure
        }

        // 4. Fallback heuristic for raw /storage/emulated/ in FileProvider or encoded paths
        val rawPath = uri.path ?: return null
        val emulatedIdx = rawPath.indexOf("/storage/emulated/")
        if (emulatedIdx != -1) {
            val extracted = rawPath.substring(emulatedIdx)
            if (File(extracted).exists()) return extracted
        }

        // Also check decoded URI string
        val decodedUri = Uri.decode(uri.toString())
        val decodedEmulatedIdx = decodedUri.indexOf("/storage/emulated/")
        if (decodedEmulatedIdx != -1) {
            val extracted = decodedUri.substring(decodedEmulatedIdx)
            if (File(extracted).exists()) return extracted
        }

        return null
    }
}
