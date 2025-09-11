package com.epic.documentmanager.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.util.Locale

object FileUtils {

    fun getMimeType(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver
        var type = resolver.getType(uri)
        if (type.isNullOrEmpty()) {
            val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (!ext.isNullOrEmpty()) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase(Locale.ROOT))
            }
        }
        return type
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (it != null && it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) result = it.getString(idx)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }

    /**
     * Menentukan ekstensi file dari mime-type atau nama file.
     * Default "bin" jika tidak diketahui.
     */
    fun guessExtension(mime: String?, fileName: String?): String {
        // Prioritas dari mime
        when (mime?.lowercase(Locale.ROOT)) {
            "application/pdf" -> return "pdf"
            "image/jpeg" -> return "jpg"
            "image/jpg" -> return "jpg"
            "image/png" -> return "png"
            "image/heic" -> return "heic"
            "application/msword" -> return "doc"
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> return "docx"
            "application/vnd.ms-excel" -> return "xls"
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> return "xlsx"
        }

        // Coba dari nama file
        val name = fileName?.lowercase(Locale.ROOT) ?: return "bin"
        val dot = name.lastIndexOf('.')
        if (dot != -1 && dot < name.length - 1) {
            return name.substring(dot + 1)
        }

        return "bin"
    }
}
