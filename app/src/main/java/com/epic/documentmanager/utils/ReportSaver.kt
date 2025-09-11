package com.epic.documentmanager.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

object ReportSaver {

    /**
     * Simpan PdfDocument ke folder Downloads dengan nama [fileName].
     * Mengembalikan Uri (API29+) atau file:// path (pre-29) jika sukses.
     */
    fun saveToDownloads(context: Context, fileName: String, doc: PdfDocument): Uri? {
        return try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val outUri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                )
                context.contentResolver.openOutputStream(outUri!!)?.use { os ->
                    doc.writeTo(os)
                }
                outUri
            } else {
                val outDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!outDir.exists()) outDir.mkdirs()
                val outFile = File(outDir, fileName)
                FileOutputStream(outFile).use { os ->
                    doc.writeTo(os)
                }
                // Pindai supaya muncul di Download app
                @Suppress("DEPRECATION")
                context.sendBroadcast(
                    android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        .setData(android.net.Uri.fromFile(outFile))
                )
                android.net.Uri.fromFile(outFile)
            }

            Toast.makeText(context, "Berhasil diunduh ke Download: $fileName", Toast.LENGTH_LONG).show()
            uri
        } catch (e: Throwable) {
            Toast.makeText(context, e.message ?: "Gagal menyimpan PDF", Toast.LENGTH_LONG).show()
            null
        } finally {
            doc.close()
        }
    }
}
