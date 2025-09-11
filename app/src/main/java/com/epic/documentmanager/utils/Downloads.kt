package com.epic.documentmanager.utils

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object Downloads {

    /** Tebak MIME dari nama file. Fallback ke octet-stream. */
    fun mimeFromName(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }

    /** Dapatkan ekstensi dari MIME. */
    private fun extFromMime(mime: String?): String? {
        if (mime.isNullOrBlank()) return null
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
    }

    /** Pastikan nama file ber-ekstensi sesuai MIME. */
    fun ensureExt(fileName: String, mime: String?): String {
        val hasExt = fileName.contains('.')
        if (hasExt) return fileName
        val ext = extFromMime(mime) ?: return fileName
        return "$fileName.$ext"
    }

    /** Simpan bytes ke folder Downloads. Return Uri file. */
    fun saveBytesToDownloads(
        context: Context,
        fileNameRaw: String,
        mimeTypeRaw: String?,
        bytes: ByteArray
    ): Uri? {
        val mimeType = mimeTypeRaw ?: "application/octet-stream"
        val fileName = ensureExt(fileNameRaw, mimeType)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return null
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            // Legacy
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            Uri.fromFile(file)
        }
    }

    /** Download format asli via DownloadManager (otomatis ke Downloads). */
    fun enqueueDownloadOriginal(context: Context, url: Uri, fileNameRaw: String, mimeTypeRaw: String?) {
        val mime = mimeTypeRaw ?: mimeFromName(fileNameRaw)
        val fileName = ensureExt(fileNameRaw, mime)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(url)
            .setTitle(fileName)
            .setDescription("Mengunduh…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        // Memberi tahu sistem tipe file → tidak jadi .bin
        req.setMimeType(mime)
        dm.enqueue(req)
    }
}
