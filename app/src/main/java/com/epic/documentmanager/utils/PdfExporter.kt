package com.epic.documentmanager.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

object PdfExporter {

    /**
     * Konversi hanya **image** → PDF.
     * Jika sumber aslinya sudah PDF / dokumen lain, akan diunduh apa adanya (tidak dikonversi).
     */
    fun exportToPdf(context: Context, storagePath: String, outputPdfName: String) {
        val ref = FirebaseStorage.getInstance().reference.child(storagePath)

        ref.metadata
            .addOnSuccessListener { md ->
                val ct = md.contentType ?: ""

                // Kalau sudah PDF → unduh original saja (ini yang sebelumnya bikin "PDF kosong")
                if (ct.equals("application/pdf", ignoreCase = true)) {
                    OriginalDownloader.download(
                        context = context,
                        storagePath = storagePath,
                        fileName = Downloads.ensureExt(outputPdfName, "application/pdf"),
                        mimeType = "application/pdf"
                    )
                    return@addOnSuccessListener
                }

                // Hanya dukung image/* untuk dikonversi
                if (!ct.startsWith("image/")) {
                    OriginalDownloader.download(
                        context = context,
                        storagePath = storagePath,
                        fileName = outputPdfName, // akan diberi ekstensi sesuai metadata
                        mimeType = ct
                    )
                    Toast.makeText(context, "Berkas bukan gambar—diunduh dalam format asli.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Unduh bytes gambar lalu render ke PDF
                ref.getBytes(10L * 1024 * 1024) // batas 10MB agar aman
                    .addOnSuccessListener { bytes ->
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp == null) {
                            Toast.makeText(context, "Gagal membaca gambar", Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }

                        val pdfBytes = imageToPdfBytes(bmp)
                        val saved = Downloads.saveBytesToDownloads(
                            context = context,
                            fileNameRaw = Downloads.ensureExt(outputPdfName, "application/pdf"),
                            mimeTypeRaw = "application/pdf",
                            bytes = pdfBytes
                        )
                        Toast.makeText(
                            context,
                            if (saved != null) "PDF berhasil dibuat di Downloads." else "Gagal menyimpan PDF.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, it.localizedMessage ?: "Gagal mengunduh berkas", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(context, it.localizedMessage ?: "Gagal mengambil metadata berkas", Toast.LENGTH_LONG).show()
            }
    }

    /** Render 1 bitmap ke 1 halaman A4 (portrait) dan kembalikan sebagai byte[]. */
    private fun imageToPdfBytes(bitmap: Bitmap): ByteArray {
        val a4w = 595  // pt
        val a4h = 842  // pt
        val pageInfo = PdfDocument.PageInfo.Builder(a4w, a4h, 1).create()
        val doc = PdfDocument()
        val page = doc.startPage(pageInfo)

        // Scale bitmap agar fit di halaman (tanpa distorsi)
        val canvas: Canvas = page.canvas
        val scale = minOf(a4w.toFloat() / bitmap.width, a4h.toFloat() / bitmap.height)
        val w = (bitmap.width * scale).toInt()
        val h = (bitmap.height * scale).toInt()
        val left = (a4w - w) / 2f
        val top = (a4h - h) / 2f
        canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + w, top + h), null)

        doc.finishPage(page)

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
