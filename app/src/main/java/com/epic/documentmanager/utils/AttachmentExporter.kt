@file:Suppress("DEPRECATION")

package com.epic.documentmanager.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

data class Attachment(val url: String, val name: String? = null)

object AttachmentExporter {

    // ========= PUBLIC API =========

    suspend fun downloadOriginals(
        context: Context,
        baseName: String,
        attachments: List<Attachment>
    ) = withContext(Dispatchers.IO) {
        attachments.forEachIndexed { idx, att ->
            val ext = guessExt(att)
            val fileName = "${baseName}-${index2(idx + 1)}.$ext"
            context.openDownloadOutput(fileName, guessMimeFromExt(ext)).use { out ->
                context.httpStream(att.url).use { input -> input.copyTo(out) }
            }
        }
    }

    suspend fun exportAsPdf(
        context: Context,
        baseName: String,
        attachments: List<Attachment>
    ) {
        attachments.forEachIndexed { idx, att ->
            val targetName = "${baseName}-${index2(idx + 1)}.pdf"
            val ext = guessExt(att).lowercase(Locale.ROOT)

            when {
                ext == "pdf" -> withContext(Dispatchers.IO) {
                    context.openDownloadOutput(targetName, "application/pdf").use { out ->
                        context.httpStream(att.url).use { input -> input.copyTo(out) }
                    }
                }

                isImageExt(ext) -> {
                    val bmp = withContext(Dispatchers.IO) {
                        context.httpStream(att.url).use { BitmapFactory.decodeStream(it) }
                    } ?: return@forEachIndexed
                    withContext(Dispatchers.IO) {
                        context.openDownloadOutput(targetName, "application/pdf").use { out ->
                            bitmapToPdfA4(bmp, out)
                        }
                    }
                }

                else -> {
                    // Headless WebView render to PDF (no Print framework callbacks)
                    renderUrlToPdf(context, att.url, targetName, ext)
                }
            }
        }
    }

    // ========= IMPLEMENTATION =========

    private fun index2(n: Int) = String.format(Locale.US, "%02d", n)

    private fun guessExt(att: Attachment): String {
        att.name?.let { n ->
            val ix = n.lastIndexOf('.')
            if (ix in 1 until n.lastIndex) return n.substring(ix + 1)
        }
        val url = att.url.substringBefore('?')
        val ix = url.lastIndexOf('.')
        return if (ix in 1 until url.lastIndex) url.substring(ix + 1) else "bin"
    }

    private fun guessMimeFromExt(ext: String): String = when (ext.lowercase(Locale.ROOT)) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "heic" -> "image/heic"
        "txt" -> "text/plain"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        else -> "application/octet-stream"
    }

    private fun isImageExt(ext: String): Boolean = when (ext.lowercase(Locale.ROOT)) {
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "tif", "tiff" -> true
        else -> false
    }

    // ---- storage helpers (MediaStore Downloads) ----

    private fun Context.openDownloadOutput(displayName: String, mime: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, mime)
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
            ) ?: throw IllegalStateException("Gagal membuat file $displayName")
            PendingOutputStream(this, uri, values)
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, displayName)
            FileOutputStream(file)
        }

    private class PendingOutputStream(
        val ctx: Context,
        val uri: Uri,
        val values: ContentValues
    ) : FileOutputStream(ctx.contentResolver.openFileDescriptor(uri, "w")!!.fileDescriptor) {
        override fun close() {
            super.close()
            values.clear()
            values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
            ctx.contentResolver.update(uri, values, null, null)
        }
    }

    // ---- network ----

    @WorkerThread
    private fun Context.httpStream(urlStr: String): InputStream {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20000
            readTimeout = 20000
            instanceFollowRedirects = true
        }
        val code = conn.responseCode
        if (code in 200..299) {
            return conn.inputStream
        } else {
            conn.disconnect()
            throw IllegalStateException("HTTP $code pada $urlStr")
        }
    }

    // ---- image → PDF A4 ----

    private fun bitmapToPdfA4(bitmap: Bitmap, out: FileOutputStream) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 @72dpi
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val margin = 24
        val availW = pageInfo.pageWidth - margin * 2
        val availH = pageInfo.pageHeight - margin * 2
        val scale = minOf(availW.toFloat() / bitmap.width, availH.toFloat() / bitmap.height)
        val dstW = (bitmap.width * scale).toInt()
        val dstH = (bitmap.height * scale).toInt()
        val left = margin + (availW - dstW) / 2
        val top = margin + (availH - dstH) / 2

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(
            Bitmap.createScaledBitmap(bitmap, dstW, dstH, true),
            left.toFloat(), top.toFloat(), paint
        )

        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }

    // ---- Headless WebView → PDF (multi-page, no Print callbacks) ----

    private suspend fun renderUrlToPdf(
        context: Context,
        originalUrl: String,
        displayName: String,
        extLower: String
    ) {
        // Decide URL to load: use Google Docs viewer for non-HTML types
        val urlToLoad = if (
            extLower in listOf(
                "html", "htm", "txt", "md", "csv", "json", "xml"
            )
        ) {
            originalUrl
        } else {
            // Wrap with Google Docs viewer
            val enc = URLEncoder.encode(originalUrl, "UTF-8")
            "https://drive.google.com/viewerng/viewer?embedded=1&url=$enc"
        }

        // Prepare output
        val output = context.openDownloadOutput(displayName, "application/pdf")

        try {
            withContext(Dispatchers.Main) {
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    // disable scrollbars to avoid drawing them
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                }

                // Wait until fully loaded
                suspendCancellableCoroutine<Unit> { cont ->
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (!cont.isCompleted) cont.resume(Unit)
                        }
                    }
                    webView.loadUrl(urlToLoad)

                    cont.invokeOnCancellation { try { webView.destroy() } catch (_: Throwable) {} }
                }

                // Give viewer a short extra time to render (images/previews)
                delay(250L)

                // Measure to A4 width at device density
                val dpi = 72        // PdfDocument canvas unit
                val a4w = 595       // points
                val a4h = 842       // points
                val scale = context.resources.displayMetrics.density

                val contentWidthPx = (a4w * scale).toInt()
                val measureSpecW = ViewMeasure.exact(contentWidthPx)
                val measureSpecH = ViewMeasure.unspecified()

                webView.measure(measureSpecW, measureSpecH)
                webView.layout(0, 0, contentWidthPx, webView.measuredHeight)

                // Total content height in px
                val contentHeightPx = (webView.contentHeight * scale).toInt().coerceAtLeast(webView.measuredHeight)

                // Convert to pages
                val pageHeightPx = (a4h * scale).toInt()
                val pageCount = ((contentHeightPx + pageHeightPx - 1) / pageHeightPx).coerceAtLeast(1)

                val doc = PdfDocument()
                for (i in 0 until pageCount) {
                    val pageInfo = PdfDocument.PageInfo.Builder(a4w, a4h, i + 1).create()
                    val page = doc.startPage(pageInfo)
                    val canvas = page.canvas

                    // Translate canvas to draw slice of the long page
                    val topPx = i * pageHeightPx
                    canvas.translate(0f, -(topPx / scale))

                    // Scale WebView drawing to PDF points
                    canvas.scale(1 / scale, 1 / scale)
                    webView.draw(canvas)
                    doc.finishPage(page)
                }

                withContext(Dispatchers.IO) { output.use { doc.writeTo(it) } }
                doc.close()

                try { webView.destroy() } catch (_: Throwable) {}
            }
        } finally {
            // PendingOutputStream will clear IS_PENDING on close()
            output.close()
        }
    }

    /** Small helpers to avoid bringing in View import just for MeasureSpec */
    private object ViewMeasure {
        fun exact(px: Int): Int {
            val mode = 0x40000000  // MeasureSpec.EXACTLY
            return (px and 0x00ffffff) or mode
        }
        fun unspecified(): Int {
            val mode = 0x00000000  // MeasureSpec.UNSPECIFIED
            return mode
        }
    }
}
