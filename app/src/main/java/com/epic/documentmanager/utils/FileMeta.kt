package com.epic.documentmanager.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap

object FileMeta {

    data class Meta(
        val displayName: String,
        val mimeType: String
    )

    /** Ambil displayName & mimeType dari Uri; fallback dari ekstensi. */
    fun fromUri(context: Context, uri: Uri): Meta {
        val cr: ContentResolver = context.contentResolver

        // MIME dari ContentResolver
        var mime = cr.getType(uri)
        // Display name dari cursor
        var name = queryDisplayName(cr, uri)

        // Fallback MIME dari ekstensi nama atau Uri
        if (mime.isNullOrBlank()) {
            val guess = guessMimeFromName(name ?: uri.toString())
            if (!guess.isNullOrBlank()) mime = guess
        }

        // Fallback name
        if (name.isNullOrBlank()) {
            // ambil nama dari path terakhir
            name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
        }

        // Pastikan ada ekstensi sesuai MIME
        val finalName = ensureExt(name!!, mime)

        return Meta(finalName, mime ?: "application/octet-stream")
    }

    private fun queryDisplayName(cr: ContentResolver, uri: Uri): String? {
        return try {
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c: Cursor ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun guessMimeFromName(name: String): String? {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext.isBlank()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    fun extFromMime(mime: String?): String? {
        if (mime.isNullOrBlank()) return null
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
    }

    /** Tambah ekstensi dari MIME kalau belum ada. */
    fun ensureExt(name: String, mime: String?): String {
        if (name.contains('.')) return name
        val ext = extFromMime(mime) ?: return name
        return "$name.$ext"
    }
}
