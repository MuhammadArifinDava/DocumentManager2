package com.epic.documentmanager.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument

/**
 * Writer PDF untuk 1 dokumen:
 * - Page 1: semua field (key-value)
 * - Page 2: lampiran (attachments) & ringkasan meta
 */
class FormPdfWriter(
    private val context: Context,
    private val title: String,                       // mis. "Pembelian Rumah"
    private val subtitle: String,                    // mis. "PR-202509-5755"
    private val fields: List<Pair<String, String>>,  // pasangan label → nilai
    private val attachments: List<String> = emptyList(),
    private val footerNote: String? = null
) {

    private val pageW = 595  // A4 72dpi: 595x842
    private val pageH = 842
    private val margin = 36

    private val pTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    private val pSubtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; color = Color.DKGRAY }
    private val pHeader = Paint().apply { color = Color.LTGRAY; strokeWidth = 1.2f }
    private val pKVLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    private val pKVValue = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
    private val pLine    = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f }
    private val pSmall   = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = Color.GRAY }

    private var pageNo = 0 // counter internal (karena PdfDocument tidak expose pageCount)

    fun writeTo(doc: PdfDocument) {
        // === PAGE 1 — DATA ===
        var page = newPage(doc)
        var y = drawTitle(page.canvas, "Data $title", subtitle, marginTop = 20)
        y += 6

        // tulis pasangan label/value (wrap sederhana)
        for ((k, v) in fields) {
            val need = 34
            val (pg, newY) = ensureSpace(doc, page, y, need)
            page = pg; y = newY

            page.canvas.drawText(k, margin.toFloat(), y.toFloat(), pKVLabel)
            page.canvas.drawText(":", (margin + (pageW - 2 * margin) * 0.32f).toFloat(), y.toFloat(), pKVValue)

            y = drawMultiline(
                page.canvas,
                v.ifBlank { "-" },
                startX = margin + ((pageW - 2 * margin) * 0.36f).toInt(),
                startY = y,
                paint = pKVValue,
                maxW = pageW - margin - (margin + ((pageW - 2 * margin) * 0.36f).toInt())
            )
            y += 10
            page.canvas.drawLine(margin.toFloat(), y.toFloat(), (pageW - margin).toFloat(), y.toFloat(), pLine)
            y += 6
        }

        footerNote?.let {
            val (pg, newY) = ensureSpace(doc, page, y, 40)
            page = pg; y = newY
            page.canvas.drawText(it, margin.toFloat(), (y + 12).toFloat(), pSmall)
            y += 22
        }

        finishPage(doc, page)

        // === PAGE 2 — LAMPIRAN ===
        page = newPage(doc)
        y = drawTitle(page.canvas, "Lampiran $title", subtitle, marginTop = 20)
        y += 6

        if (attachments.isEmpty()) {
            page.canvas.drawText("Tidak ada lampiran.", margin.toFloat(), (y + 12).toFloat(), pKVValue)
        } else {
            attachments.forEachIndexed { i, name ->
                val (pg, newY) = ensureSpace(doc, page, y, 24)
                page = pg; y = newY

                page.canvas.drawText("${i + 1}. $name", margin.toFloat(), (y + 12).toFloat(), pKVValue)
                y += 18
                page.canvas.drawLine(margin.toFloat(), y.toFloat(), (pageW - margin).toFloat(), y.toFloat(), pLine)
                y += 4
            }
            y += 8
            page.canvas.drawText("Total lampiran: ${attachments.size}", margin.toFloat(), y.toFloat(), pSmall)
        }

        finishPage(doc, page)
    }

    // ===== Helpers =====

    private fun newPage(doc: PdfDocument): PdfDocument.Page {
        pageNo += 1
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create()
        val page = doc.startPage(info)
        // header garis tipis untuk setiap halaman
        page.canvas.drawLine(
            margin.toFloat(),
            (margin - 6).toFloat(),
            (pageW - margin).toFloat(),
            (margin - 6).toFloat(),
            pHeader
        )
        return page
    }

    private fun finishPage(doc: PdfDocument, page: PdfDocument.Page) {
        doc.finishPage(page)
    }

    /** Pastikan ruang cukup; jika tidak, tutup halaman & buka halaman baru. */
    private fun ensureSpace(
        doc: PdfDocument,
        currentPage: PdfDocument.Page,
        currentY: Int,
        need: Int
    ): Pair<PdfDocument.Page, Int> {
        return if (currentY + need <= pageH - margin) {
            currentPage to currentY
        } else {
            finishPage(doc, currentPage)
            val newP = newPage(doc)
            newP to margin
        }
    }

    private fun drawTitle(c: Canvas, title: String, sub: String, marginTop: Int): Int {
        var y = margin + marginTop
        c.drawText(title, margin.toFloat(), y.toFloat(), pTitle)
        y += (pTitle.textSize + 6).toInt()
        c.drawText(sub, margin.toFloat(), y.toFloat(), pSubtitle)
        y += (pSubtitle.textSize + 10).toInt()
        c.drawLine(margin.toFloat(), y.toFloat(), (pageW - margin).toFloat(), y.toFloat(), pHeader)
        return y + 10
    }

    private fun drawMultiline(
        c: Canvas,
        text: String,
        startX: Int,
        startY: Int,
        paint: Paint,
        maxW: Int
    ): Int {
        val words = text.split(Regex("\\s+"))
        var line = StringBuilder()
        var y = startY
        val fm = paint.fontMetrics
        val lineH = (fm.bottom - fm.top + 2).toInt()

        fun flush() {
            if (line.isNotEmpty()) {
                c.drawText(line.toString(), startX.toFloat(), y.toFloat(), paint)
                y += lineH
                line = StringBuilder()
            }
        }

        for (w in words) {
            val tryLine = if (line.isEmpty()) w else "${line} $w"
            if (paint.measureText(tryLine) > maxW) {
                flush()
                line.append(w)
            } else {
                line.clear(); line.append(tryLine)
            }
        }
        flush()
        return y
    }
}
