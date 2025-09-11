package com.epic.documentmanager.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument

/**
 * Penulis PDF sederhana untuk Laporan Bulanan/Tahunan.
 * Kolom: Nama | Alamat | No Telepon
 * Menangani page-break + header ulang tiap halaman.
 */
class ReportPdfWriter(
    private val context: Context,
    private val title: String,
    private val subtitle: String,
    private val rowsPembelian: List<Row>,
    private val rowsRenovasi: List<Row>,
    private val rowsAC: List<Row>,
    private val rowsCCTV: List<Row>
) {

    data class Row(val nama: String, val alamat: String, val telp: String)

    // Ukuran A4 (point pada PdfDocument ~ pixel "pdf")
    private val pageW = 595
    private val pageH = 842
    private val margin = 32

    private val contentW get() = pageW - (margin * 2)

    // Lebar kolom (tiga kolom)
    private val colNamaW   = (contentW * 0.34f).toInt()
    private val colAlamatW = (contentW * 0.48f).toInt()
    private val colTelpW   = contentW - colNamaW - colAlamatW

    // Paint
    private val pTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val pSubtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 11f
    }
    private val pHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val pText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 10f
    }
    private val pLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 0.6f
    }

    private var pageNo = 0

    fun writeTo(doc: PdfDocument) {
        // mulai halaman pertama
        var page = newPage(doc)
        var canvas = page.canvas
        var y = drawTitle(canvas, margin)

        // header kolom
        y = drawColumnHeader(canvas, y)

        // helper gambar 1 seksi
        fun drawSection(title: String, rows: List<Row>) {
            // judul seksi
            val need = (pHeader.textSize + 8).toInt()
            if (needBreak(y, need)) {
                doc.finishPage(page)
                page = newPage(doc)
                canvas = page.canvas
                y = drawTitle(canvas, margin)
                y = drawColumnHeader(canvas, y)
            }
            canvas.drawText(title, margin.toFloat(), y.toFloat(), pHeader)
            y += (pHeader.textSize + 4).toInt()
            canvas.drawLine(margin.toFloat(), y.toFloat(), (pageW - margin).toFloat(), y.toFloat(), pLine)
            y += 6

            // tiap baris
            rows.forEach { r ->
                val rowHeight = (pText.textSize + 6).toInt()
                if (needBreak(y, rowHeight)) {
                    doc.finishPage(page)
                    page = newPage(doc)
                    canvas = page.canvas
                    y = drawTitle(canvas, margin)
                    y = drawColumnHeader(canvas, y)
                }
                // kolom-kolom
                var x = margin
                canvas.drawText(r.nama,   x.toFloat(), y.toFloat(), pText);   x += colNamaW
                canvas.drawText(r.alamat, x.toFloat(), y.toFloat(), pText);   x += colAlamatW
                canvas.drawText(r.telp,   x.toFloat(), y.toFloat(), pText)
                y += rowHeight
            }

            // total seksi
            val totText = "Total Dokumen: ${rows.size}"
            val totH = (pSubtitle.textSize + 10).toInt()
            if (needBreak(y, totH)) {
                doc.finishPage(page)
                page = newPage(doc)
                canvas = page.canvas
                y = drawTitle(canvas, margin)
                y = drawColumnHeader(canvas, y)
            }
            canvas.drawText(totText, margin.toFloat(), y.toFloat(), pSubtitle)
            y += totH
        }

        // gambar semua seksi
        drawSection("Pembelian Rumah", rowsPembelian)
        drawSection("Renovasi Rumah",  rowsRenovasi)
        drawSection("Pemasangan AC",   rowsAC)
        drawSection("Pemasangan CCTV", rowsCCTV)

        // total keseluruhan
        val grand = rowsPembelian.size + rowsRenovasi.size + rowsAC.size + rowsCCTV.size
        val grandText = "Total Dokumen (Semua): $grand"
        val gNeed = (pHeader.textSize + 6).toInt()
        if (needBreak(y, gNeed)) {
            doc.finishPage(page)
            page = newPage(doc)
            canvas = page.canvas
            y = drawTitle(canvas, margin)
            y = drawColumnHeader(canvas, y)
        }
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageW - margin).toFloat(), y.toFloat(), pLine)
        y += 8
        canvas.drawText(grandText, margin.toFloat(), y.toFloat(), pHeader)

        doc.finishPage(page)
    }

    // ---------- Helpers ----------

    private fun newPage(doc: PdfDocument): PdfDocument.Page {
        pageNo += 1
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create()
        return doc.startPage(info)
    }

    private fun needBreak(currentY: Int, needed: Int): Boolean {
        val bottomLimit = pageH - margin
        return currentY + needed > bottomLimit
    }

    private fun drawTitle(c: Canvas, startY: Int): Int {
        var y = startY
        c.drawText(title, margin.toFloat(), y.toFloat(), pTitle)
        y += (pTitle.textSize + 4).toInt()
        c.drawText(subtitle, margin.toFloat(), y.toFloat(), pSubtitle)
        y += (pSubtitle.textSize + 8).toInt()
        c.drawLine(margin.toFloat(), y.toFloat(), (pageW - margin).toFloat(), y.toFloat(), pLine)
        return y + 10
    }

    private fun drawColumnHeader(c: Canvas, startY: Int): Int {
        var y = startY
        var x = margin
        c.drawText("Nama",      x.toFloat(), y.toFloat(), pHeader); x += colNamaW
        c.drawText("Alamat",    x.toFloat(), y.toFloat(), pHeader); x += colAlamatW
        c.drawText("No Telepon",x.toFloat(), y.toFloat(), pHeader)
        y += (pHeader.textSize + 6).toInt()
        c.drawLine(margin.toFloat(), y.toFloat(), (pageW - margin).toFloat(), y.toFloat(), pLine)
        return y + 8
    }
}
