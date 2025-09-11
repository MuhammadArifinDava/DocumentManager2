package com.epic.documentmanager.utils

import android.content.Context
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.epic.documentmanager.models.*
import com.epic.documentmanager.utils.DateUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


object PDFGenerator {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun generatePembelianRumahPDF(context: Context, data: PembelianRumah): File {
        val fileName = "PR_${data.uniqueCode}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        val writer = PdfWriter(FileOutputStream(file))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)

        // Header
        val header = Paragraph("DOKUMEN PEMBELIAN RUMAH")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(18f)
            .setBold()
            .setMarginBottom(20f)
        document.add(header)

        // Kode Unik
        val codeHeader = Paragraph("Kode Dokumen: ${data.uniqueCode}")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12f)
            .setBold()
            .setMarginBottom(20f)
        document.add(codeHeader)

        // Data Table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        // Add data rows
        addTableRow(table, "Nama Lengkap", data.nama)
        addTableRow(table, "Alamat KTP", data.alamatKTP)
        addTableRow(table, "NIK", data.nik)
        addTableRow(table, "NPWP", data.npwp)
        addTableRow(table, "No. Telepon", data.noTelepon)
        addTableRow(table, "Status Pernikahan", data.statusPernikahan)
        addTableRow(table, "Nama Pasangan", data.namaPasangan)
        addTableRow(table, "Pekerjaan", data.pekerjaan)
        addTableRow(table, "Gaji", data.gaji)
        addTableRow(table, "Kontak Darurat", data.kontakDarurat)
        addTableRow(table, "Tempat Kerja", data.tempatKerja)
        addTableRow(table, "Nama Perumahan", data.namaPerumahan)
        addTableRow(table, "Tipe Rumah", data.tipeRumah)
        addTableRow(table, "Jenis Pembayaran", data.jenisPembayaran)
        addTableRow(table, "Kategori Rumah", data.tipeRumahKategori)
        addTableRow(table, "Tanggal Dibuat", dateFormat.format(Date(data.createdAt)))

        document.add(table)

        // Footer
        val footer = Paragraph("\nDokumen ini digenerate otomatis oleh sistem pada ${dateFormat.format(Date())}")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10f)
            .setItalic()
            .setMarginTop(30f)
        document.add(footer)

        document.close()
        return file
    }

    fun generateRenovasiRumahPDF(context: Context, data: RenovasiRumah): File {
        val fileName = "RR_${data.uniqueCode}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        val writer = PdfWriter(FileOutputStream(file))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)

        // Header
        val header = Paragraph("DOKUMEN RENOVASI RUMAH")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(18f)
            .setBold()
            .setMarginBottom(20f)
        document.add(header)

        // Kode Unik
        val codeHeader = Paragraph("Kode Dokumen: ${data.uniqueCode}")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12f)
            .setBold()
            .setMarginBottom(20f)
        document.add(codeHeader)

        // Data Table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        addTableRow(table, "Nama", data.nama)
        addTableRow(table, "Alamat", data.alamat)
        addTableRow(table, "No. Telepon", data.noTelepon)
        addTableRow(table, "Deskripsi Renovasi", data.deskripsiRenovasi)
        addTableRow(table, "Tanggal Dibuat", dateFormat.format(Date(data.createdAt)))

        document.add(table)

        // Footer
        val footer = Paragraph("\nDokumen ini digenerate otomatis oleh sistem pada ${dateFormat.format(Date())}")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10f)
            .setItalic()
            .setMarginTop(30f)
        document.add(footer)

        document.close()
        return file
    }

    fun generatePemasanganACPDF(context: Context, data: PemasanganAC): File {
        val fileName = "AC_${data.uniqueCode}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        val writer = PdfWriter(FileOutputStream(file))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)

        // Header
        val header = Paragraph("DOKUMEN PEMASANGAN AC")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(18f)
            .setBold()
            .setMarginBottom(20f)
        document.add(header)

        // Kode Unik
        val codeHeader = Paragraph("Kode Dokumen: ${data.uniqueCode}")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12f)
            .setBold()
            .setMarginBottom(20f)
        document.add(codeHeader)

        // Data Table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        addTableRow(table, "Nama", data.nama)
        addTableRow(table, "Alamat", data.alamat)
        addTableRow(table, "No. Telepon", data.noTelepon)
        addTableRow(table, "Jenis AC", data.jenisTipeAC)
        addTableRow(table, "Jumlah Unit", data.jumlahUnit.toString())
        addTableRow(table, "Tanggal Dibuat", dateFormat.format(Date(data.createdAt)))

        document.add(table)

        // Footer
        val footer = Paragraph("\nDokumen ini digenerate otomatis oleh sistem pada ${dateFormat.format(Date())}")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10f)
            .setItalic()
            .setMarginTop(30f)
        document.add(footer)

        document.close()
        return file
    }

    fun generatePemasanganCCTVPDF(context: Context, data: PemasanganCCTV): File {
        val fileName = "CC_${data.uniqueCode}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        val writer = PdfWriter(FileOutputStream(file))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)

        // Header
        val header = Paragraph("DOKUMEN PEMASANGAN CCTV")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(18f)
            .setBold()
            .setMarginBottom(20f)
        document.add(header)

        // Kode Unik
        val codeHeader = Paragraph("Kode Dokumen: ${data.uniqueCode}")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12f)
            .setBold()
            .setMarginBottom(20f)
        document.add(codeHeader)

        // Data Table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        addTableRow(table, "Nama", data.nama)
        addTableRow(table, "Alamat", data.alamat)
        addTableRow(table, "No. Telepon", data.noTelepon)
        addTableRow(table, "Jumlah Unit", data.jumlahUnit.toString())
        addTableRow(table, "Tanggal Dibuat", dateFormat.format(Date(data.createdAt)))

        document.add(table)

        // Footer
        val footer = Paragraph("\nDokumen ini digenerate otomatis oleh sistem pada ${dateFormat.format(Date())}")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10f)
            .setItalic()
            .setMarginTop(30f)
        document.add(footer)

        document.close()
        return file
    }

    fun generateMonthlyReportPDF(context: Context, report: MonthlyReport): File {
        val fileName = "Report_${report.month}_${report.year}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        val writer = PdfWriter(FileOutputStream(file))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)

        // Header
        val header = Paragraph("LAPORAN BULANAN DOKUMEN")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(18f)
            .setBold()
            .setMarginBottom(20f)
        document.add(header)

        // Month and Year
        val monthYearHeader = Paragraph("${DateUtils.getMonthName(report.month)} ${report.year}")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(14f)
            .setBold()
            .setMarginBottom(30f)
        document.add(monthYearHeader)

        // Summary Table
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
        summaryTable.setWidth(UnitValue.createPercentValue(100f))

        // Add summary header
        val summaryHeaderCell = Cell(1, 2)
            .add(Paragraph("RINGKASAN").setBold().setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
        summaryTable.addHeaderCell(summaryHeaderCell)

        addTableRow(summaryTable, "Total Dokumen", report.totalDocuments.toString())
        addTableRow(summaryTable, "Pembelian Rumah", report.pembelianRumahCount.toString())
        addTableRow(summaryTable, "Renovasi Rumah", report.renovasiRumahCount.toString())
        addTableRow(summaryTable, "Pemasangan AC", report.pemasanganACCount.toString())
        addTableRow(summaryTable, "Pemasangan CCTV", report.pemasanganCCTVCount.toString())
        addTableRow(summaryTable, "Tanggal Generate", dateFormat.format(Date(report.generatedAt)))

        document.add(summaryTable)

        // Chart representation (text-based)
        document.add(Paragraph("\nGRAFIK DISTRIBUSI DOKUMEN").setBold().setMarginTop(30f))

        val chartTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 20f, 40f)))
        chartTable.setWidth(UnitValue.createPercentValue(100f))

        chartTable.addHeaderCell(createHeaderCell("Jenis Dokumen"))
        chartTable.addHeaderCell(createHeaderCell("Jumlah"))
        chartTable.addHeaderCell(createHeaderCell("Persentase"))

        val total = report.totalDocuments.toDouble()
        if (total > 0) {
            addChartRow(chartTable, "Pembelian Rumah", report.pembelianRumahCount, total)
            addChartRow(chartTable, "Renovasi Rumah", report.renovasiRumahCount, total)
            addChartRow(chartTable, "Pemasangan AC", report.pemasanganACCount, total)
            addChartRow(chartTable, "Pemasangan CCTV", report.pemasanganCCTVCount, total)
        }

        document.add(chartTable)

        // Footer
        val footer = Paragraph("\n\nLaporan ini digenerate otomatis oleh sistem pada ${dateFormat.format(Date())}")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10f)
            .setItalic()
            .setMarginTop(30f)
        document.add(footer)

        document.close()
        return file
    }

    private fun addTableRow(table: Table, label: String, value: String) {
        table.addCell(Cell().add(Paragraph(label).setBold()))
        table.addCell(Cell().add(Paragraph(value)))
    }

    private fun createHeaderCell(text: String): Cell {
        return Cell().add(Paragraph(text).setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setTextAlignment(TextAlignment.CENTER)
    }

    private fun addChartRow(table: Table, label: String, count: Int, total: Double) {
        val percentage = if (total > 0) (count / total * 100).toInt() else 0
        val bar = "█".repeat(percentage / 5) // Simple text-based bar

        table.addCell(Cell().add(Paragraph(label)))
        table.addCell(Cell().add(Paragraph(count.toString()).setTextAlignment(TextAlignment.CENTER)))
        table.addCell(Cell().add(Paragraph("$percentage% $bar")))
    }
}