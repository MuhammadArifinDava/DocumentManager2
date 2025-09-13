package com.epic.documentmanager.activities

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.epic.documentmanager.R
import com.epic.documentmanager.adapters.DocumentImage
import com.epic.documentmanager.adapters.DocumentImageAdapter
import com.epic.documentmanager.models.PemasanganAC
import com.epic.documentmanager.models.PemasanganCCTV
import com.epic.documentmanager.models.PembelianRumah
import com.epic.documentmanager.models.RenovasiRumah
import com.epic.documentmanager.utils.Attachment
import com.epic.documentmanager.utils.AttachmentExporter
import com.epic.documentmanager.utils.Constants
import com.epic.documentmanager.utils.DateUtils
import com.epic.documentmanager.viewmodels.AuthViewModel
import com.epic.documentmanager.viewmodels.DocumentViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.Serializable
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class DocumentDetailActivity : AppCompatActivity() {

    companion object {
        private const val REQ_EDIT = 9901
    }

    private lateinit var documentViewModel: DocumentViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var imageAdapter: DocumentImageAdapter

    // Views
    private lateinit var tvUniqueCode: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var tvNama: TextView
    private lateinit var tvAlamat: TextView
    private lateinit var tvNoTelepon: TextView
    private lateinit var rvImages: RecyclerView
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var btnPrint: MaterialButton
    private lateinit var btnDownload: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var document: Any? = null
    private var documentType: String = Constants.DOC_TYPE_PEMBELIAN_RUMAH
    private var userRole: String = Constants.ROLE_STAFF

    // ==== permission (pre-Android 10) ====
    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) doCurrentActionAfterPermission?.invoke()
            else toast("Izin penyimpanan ditolak")
            doCurrentActionAfterPermission = null
        }
    private var doCurrentActionAfterPermission: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_detail)

        // terima data dari list
        document = intent.getSerializableExtra("document")
        documentType = intent.getStringExtra("documentType") ?: Constants.DOC_TYPE_PEMBELIAN_RUMAH

        setupToolbar()
        bindViews()
        setupViewModel()
        setupImageRecyclerView()
        setupButtons()

        // apply spacing ke text detail
        applyNiceLineSpacing(tvNama, tvAlamat, tvNoTelepon)

        displayDocumentDetails()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun bindViews() {
        tvUniqueCode = findViewById(R.id.tvUniqueCode)
        tvCategory = findViewById(R.id.tvCategory)
        tvCreatedAt = findViewById(R.id.tvCreatedAt)
        tvNama = findViewById(R.id.tvNama)
        tvAlamat = findViewById(R.id.tvAlamat)
        tvNoTelepon = findViewById(R.id.tvNoTelepon)
        rvImages = findViewById(R.id.rvImages)
        btnEdit = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
        btnPrint = findViewById(R.id.btnPrint)         // teks tombol di layout sebaiknya “EKSPOR PDF”
        btnDownload = findViewById(R.id.btnDownload)   // teks tombol “UNDUH”
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupViewModel() {
        documentViewModel = ViewModelProvider(this)[DocumentViewModel::class.java]
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        authViewModel.getCurrentUser()
        authViewModel.currentUser.observe(this) { user ->
            user?.let {
                userRole = it.role
                btnDelete.visibility = if (userRole == Constants.ROLE_ADMIN) View.VISIBLE else View.GONE
            }
        }

        documentViewModel.deleteResult.observe(this) { result ->
            if (result.isSuccess) {
                toast("Dokumen berhasil dihapus")
                finish()
            } else {
                toast("Gagal menghapus dokumen")
            }
        }
    }

    private fun setupImageRecyclerView() {
        imageAdapter = DocumentImageAdapter(
            images = emptyList(),
            onImageClick = { _, _ -> },
            onDeleteClick = { _, _ -> },
            canDelete = false
        )
        rvImages.layoutManager = GridLayoutManager(this, 2)
        rvImages.adapter = imageAdapter
    }

    private fun setupButtons() {
        btnEdit.setOnClickListener { editDocument() }
        btnDelete.setOnClickListener { showDeleteConfirmation() }

        // === EKSPOR PDF: sekarang jadi 2 halaman (detail + ringkasan lampiran)
        btnPrint.setOnClickListener {
            requireStorageIfNeeded {
                exportDetailAsTwoPagePdf()
            }
        }

        // UNDUH (file asli lampiran, tanpa PDF)
        btnDownload.setOnClickListener {
            requireStorageIfNeeded {
                downloadAllAttachmentsOriginal()
            }
        }
    }

    private fun displayDocumentDetails() {
        document?.let { doc ->
            try {
                when (documentType) {
                    Constants.DOC_TYPE_PEMBELIAN_RUMAH -> bindPembelianRumah(doc as PembelianRumah)
                    Constants.DOC_TYPE_RENOVASI_RUMAH -> bindRenovasiRumah(doc as RenovasiRumah)
                    Constants.DOC_TYPE_PEMASANGAN_AC -> bindPemasanganAC(doc as PemasanganAC)
                    Constants.DOC_TYPE_PEMASANGAN_CCTV -> bindPemasanganCCTV(doc as PemasanganCCTV)
                }
            } catch (_: Exception) {
                toast("Gagal menampilkan detail")
                finish()
            }
        }
    }

    // helper: fallback string & line spacing
    private fun v(s: String?): String = if (s.isNullOrBlank()) "-" else s.trim()

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun applyNiceLineSpacing(vararg tvs: TextView) {
        val extra = dp(4f)
        tvs.forEach { it.setLineSpacing(extra, 1.15f) }
    }

    // === Binder per tipe ===
    private fun bindPembelianRumah(data: PembelianRumah) {
        tvUniqueCode.text = data.uniqueCode
        tvCategory.text = "Pembelian Rumah"
        tvCreatedAt.text = "Dibuat: ${DateUtils.formatDateTime(data.createdAt)}"

        tvNama.text = "Nama : ${v(data.nama)}"
        tvAlamat.text = "Alamat : ${v(data.alamatKTP)}"
        tvNoTelepon.text = buildString {
            appendLine("Nomor Telepon : ${v(data.noTelepon)}")
            appendLine("NIK : ${v(data.nik)}")
            appendLine("NPWP : ${v(data.npwp)}")
            appendLine("Status Pernikahan : ${v(data.statusPernikahan)}")
            appendLine("Nama Pasangan : ${v(data.namaPasangan)}")
            appendLine("Pekerjaan : ${v(data.pekerjaan)}")
            appendLine("Gaji : ${v(data.gaji)}")
            appendLine("Kontak Darurat : ${v(data.kontakDarurat)}")
            appendLine("Tempat Kerja : ${v(data.tempatKerja)}")
            appendLine("Nama Perumahan : ${v(data.namaPerumahan)}")
            appendLine("Tipe Rumah : ${v(data.tipeRumah)}")
            appendLine("Jenis Pembayaran : ${v(data.jenisPembayaran)}")
            append("Kategori Tipe Rumah : ${v(data.tipeRumahKategori)}")
        }

        imageAdapter.updateImages(data.attachments.map { (n, u) -> DocumentImage(n, null, u) })
    }

    private fun bindRenovasiRumah(data: RenovasiRumah) {
        tvUniqueCode.text = data.uniqueCode
        tvCategory.text = "Renovasi Rumah"
        tvCreatedAt.text = "Dibuat: ${DateUtils.formatDateTime(data.createdAt)}"

        tvNama.text = "Nama : ${v(data.nama)}"
        tvAlamat.text = "Alamat : ${v(data.alamat)}"
        tvNoTelepon.text = buildString {
            appendLine("Nomor Telepon : ${v(data.noTelepon)}")
            append("Deskripsi Renovasi : ${v(data.deskripsiRenovasi)}")
        }

        imageAdapter.updateImages(data.attachments.map { (n, u) -> DocumentImage(n, null, u) })
    }

    private fun bindPemasanganAC(data: PemasanganAC) {
        tvUniqueCode.text = data.uniqueCode
        tvCategory.text = "Pemasangan AC"
        tvCreatedAt.text = "Dibuat: ${DateUtils.formatDateTime(data.createdAt)}"

        tvNama.text = "Nama : ${v(data.nama)}"
        tvAlamat.text = "Alamat : ${v(data.alamat)}"
        tvNoTelepon.text = buildString {
            appendLine("Nomor Telepon : ${v(data.noTelepon)}")
            appendLine("Jenis/Tipe AC : ${v(data.jenisTipeAC)}")
            append("Jumlah Unit : ${if (data.jumlahUnit <= 0) "-" else data.jumlahUnit}")
        }

        imageAdapter.updateImages(data.attachments.map { (n, u) -> DocumentImage(n, null, u) })
    }

    private fun bindPemasanganCCTV(data: PemasanganCCTV) {
        tvUniqueCode.text = data.uniqueCode
        tvCategory.text = "Pemasangan CCTV"
        tvCreatedAt.text = "Dibuat: ${DateUtils.formatDateTime(data.createdAt)}"

        tvNama.text = "Nama : ${v(data.nama)}"
        tvAlamat.text = "Alamat : ${v(data.alamat)}"
        tvNoTelepon.text = buildString {
            appendLine("Nomor Telepon : ${v(data.noTelepon)}")
            append("Jumlah Unit : ${if (data.jumlahUnit <= 0) "-" else data.jumlahUnit}")
        }

        imageAdapter.updateImages(data.attachments.map { (n, u) -> DocumentImage(n, null, u) })
    }

    // === Actions ===

    private fun editDocument() {
        val intent = Intent(this, EditDocumentActivity::class.java).apply {
            putExtra(EditDocumentActivity.EXTRA_DOCUMENT, document as Serializable)
            putExtra(EditDocumentActivity.EXTRA_DOC_TYPE, documentType)
        }
        startActivityForResult(intent, REQ_EDIT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_EDIT && resultCode == Activity.RESULT_OK) {
            finish() // selesai edit → balik ke list
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus dokumen ini?")
            .setPositiveButton("Hapus") { _, _ -> deleteDocument() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteDocument() {
        val doc = document ?: return
        try {
            val idField = doc::class.java.getDeclaredField("id")
            idField.isAccessible = true
            val id = idField.get(doc) as String

            when (documentType) {
                Constants.DOC_TYPE_PEMBELIAN_RUMAH -> documentViewModel.deletePembelianRumah(id)
                Constants.DOC_TYPE_RENOVASI_RUMAH -> documentViewModel.deleteRenovasiRumah(id)
                Constants.DOC_TYPE_PEMASANGAN_AC -> documentViewModel.deletePemasanganAC(id)
                Constants.DOC_TYPE_PEMASANGAN_CCTV -> documentViewModel.deletePemasanganCCTV(id)
            }
        } catch (_: Exception) {
            toast("Gagal menghapus dokumen")
        }
    }

    // ===== CETAK (EKSPOR PDF) 2 HALAMAN =====

    /** Pastikan permission (Android 9 kebawah) sebelum menulis ke Downloads. */
    private fun requireStorageIfNeeded(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            action(); return
        }
        val granted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) action()
        else {
            doCurrentActionAfterPermission = action
            requestStoragePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /**
     * Halaman 1: semua field (dinamis per tipe).
     * Halaman 2: ringkasan lampiran (nama + url singkat).
     */
    private fun exportDetailAsTwoPagePdf() {
        val unit = uniqueCode()
        val atts = collectAttachments()

        progress(true)
        CoroutineScope(Dispatchers.IO).launch {
            val doc = PdfDocument()
            try {
                // ====== Page metrics (A4 72dpi) ======
                val pageW = 595
                val pageH = 842
                val margin = 36

                // ====== Paints ======
                val pTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                    textSize = 16f
                }
                val pSub = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
                val pText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
                val pLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 0.7f }

                // =================================================================
                // Page 1 — DETAIL (jarak antar baris diperlebar)
                // =================================================================
                val info1 = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
                var page = doc.startPage(info1)
                var c = page.canvas

                var y = margin
                c.drawText("Laporan Detail Dokumen", margin.toFloat(), y.toFloat(), pTitle)
                y += (pTitle.textSize + 6).toInt()
                c.drawText(
                    "Kode: $unit   •   Kategori: ${tvCategory.text}   •   Dibuat: ${
                        tvCreatedAt.text.removePrefix(
                            "Dibuat: "
                        )
                    }",
                    margin.toFloat(), y.toFloat(), pSub
                )
                y += (pSub.textSize + 12).toInt() // <— spasi header dibesarkan

                // siapkan pasangan label–value sesuai tipe
                val rows: List<Pair<String, String>> = when (val d = document) {
                    is PembelianRumah -> listOf(
                        "Nama" to v(d.nama),
                        "Alamat KTP" to v(d.alamatKTP),
                        "Nomor Telepon" to v(d.noTelepon),
                        "NIK" to v(d.nik),
                        "NPWP" to v(d.npwp),
                        "Status Pernikahan" to v(d.statusPernikahan),
                        "Nama Pasangan" to v(d.namaPasangan),
                        "Pekerjaan" to v(d.pekerjaan),
                        "Gaji" to v(d.gaji),
                        "Kontak Darurat" to v(d.kontakDarurat),
                        "Tempat Kerja" to v(d.tempatKerja),
                        "Nama Perumahan" to v(d.namaPerumahan),
                        "Tipe Rumah" to v(d.tipeRumah),
                        "Jenis Pembayaran" to v(d.jenisPembayaran),
                        "Kategori Tipe Rumah" to v(d.tipeRumahKategori)
                    )

                    is RenovasiRumah -> listOf(
                        "Nama" to v(d.nama),
                        "Alamat" to v(d.alamat),
                        "Nomor Telepon" to v(d.noTelepon),
                        "Deskripsi Renovasi" to v(d.deskripsiRenovasi)
                    )

                    is PemasanganAC -> listOf(
                        "Nama" to v(d.nama),
                        "Alamat" to v(d.alamat),
                        "Nomor Telepon" to v(d.noTelepon),
                        "Jenis/Tipe AC" to v(d.jenisTipeAC),
                        "Jumlah Unit" to (if (d.jumlahUnit <= 0) "-" else d.jumlahUnit.toString())
                    )

                    is PemasanganCCTV -> listOf(
                        "Nama" to v(d.nama),
                        "Alamat" to v(d.alamat),
                        "Nomor Telepon" to v(d.noTelepon),
                        "Jumlah Unit" to (if (d.jumlahUnit <= 0) "-" else d.jumlahUnit.toString())
                    )

                    else -> emptyList()
                }

                // gambar 2 kolom; JARAK antar entry diperbesar (lineGap = 14)
                y = drawKeyValueColumn(
                    canvas = c,
                    startY = y,
                    pageW = pageW,
                    margin = margin,
                    lineGap = 14,            // <— sebelumnya 8
                    labelPaint = pText,
                    valuePaint = pText,
                    rows = rows
                )

                // footer line
                c.drawLine(
                    margin.toFloat(), (pageH - margin).toFloat(),
                    (pageW - margin).toFloat(), (pageH - margin).toFloat(), pLine
                )
                doc.finishPage(page)

                // =================================================================
                // Page 2 — LAMPIRAN: render isi gambar. Non-gambar -> judul + link
                // =================================================================
                val info2 = PdfDocument.PageInfo.Builder(pageW, pageH, 2).create()
                page = doc.startPage(info2)
                c = page.canvas

                var y2 = margin
                c.drawText("Lampiran", margin.toFloat(), y2.toFloat(), pTitle)
                y2 += (pTitle.textSize + 10).toInt()

                if (atts.isEmpty()) {
                    c.drawText("Tidak ada lampiran.", margin.toFloat(), y2.toFloat(), pText)
                } else {
                    val usableW = pageW - margin * 2
                    val maxImageH = 180 // tinggi maksimum per gambar biar muat banyak

                    for ((i, a) in atts.withIndex()) {
                        // buat judul kecil setiap attachment
                        val name = a.name ?: a.url.substringAfterLast('/').ifBlank { "lampiran_${i + 1}" }
                        c.drawText("• $name", margin.toFloat(), y2.toFloat(), pSub)
                        y2 += (pSub.textSize + 6).toInt()

                        // coba ambil bitmap dari URL (jpeg/png/webp). Jika gagal → tulis linknya
                        val bmp = loadBitmapFromUrl(a.url)
                        if (bmp != null) {
                            val (wScaled, hScaled) = scaleToFit(
                                bmp.width,
                                bmp.height,
                                usableW,
                                maxImageH
                            )
                            if (y2 + hScaled > pageH - margin) {
                                // halaman lampiran berikutnya
                                doc.finishPage(page)
                                val next =
                                    PdfDocument.PageInfo.Builder(pageW, pageH, doc.pages.size + 1)
                                        .create()
                                page = doc.startPage(next)
                                c = page.canvas
                                y2 = margin
                                c.drawText(
                                    "Lampiran (lanjutan)",
                                    margin.toFloat(),
                                    y2.toFloat(),
                                    pTitle
                                )
                                y2 += (pTitle.textSize + 10).toInt()
                            }
                            val left = margin + (usableW - wScaled) / 2
                            c.drawBitmap(
                                Bitmap.createScaledBitmap(bmp, wScaled, hScaled, true),
                                left.toFloat(), y2.toFloat(), null
                            )
                            y2 += hScaled + 14 // spasi ekstra antar gambar
                        } else {
                            // fallback non-gambar: tampilkan URL singkat
                            val shortUrl =
                                a.url.let { if (it.length > 110) it.take(110) + "…" else it }
                            drawMultiline(c, shortUrl, margin, y2, usableW, pText)
                            y2 += (pText.textSize + 12).toInt()
                        }

                        // garis pemisah tipis
                        c.drawLine(
                            margin.toFloat(),
                            y2.toFloat(),
                            (pageW - margin).toFloat(),
                            y2.toFloat(),
                            pLine
                        )
                        y2 += 10
                    }
                }

                doc.finishPage(page)

                // ===== Save to Downloads =====
                savePdfToDownloads(doc, "Detail_${unit}.pdf")

                runOnUiThread {
                    progress(false)
                    toast("Berhasil menyimpan PDF detail ke folder Download.")
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    progress(false)
                    toast(e.message ?: "Gagal membuat PDF")
                }
            } finally {
                try {
                    doc.close()
                } catch (_: Throwable) {
                }
            }
        }
    }

    // Ambil bitmap dari URL (hanya untuk image mime: jpg/png/webp). IO thread OK.
    private fun loadBitmapFromUrl(url: String): Bitmap? = try {
        val uri = android.net.Uri.parse(url)
        // Jika file ada di Firebase Storage (https/http) kita pakai URL langsung.
        val conn = java.net.URL(uri.toString()).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.instanceFollowRedirects = true
        conn.inputStream.use { BitmapFactory.decodeStream(it) }
    } catch (_: Throwable) { null }

    // hitung skala agar muat ke kotak WxH (menjaga aspect ratio)
    private fun scaleToFit(srcW: Int, srcH: Int, boxW: Int, boxH: Int): Pair<Int, Int> {
        if (srcW <= 0 || srcH <= 0) return boxW to boxH
        val ratio = minOf(boxW.toFloat() / srcW, boxH.toFloat() / srcH)
        return (srcW * ratio).toInt().coerceAtLeast(1) to (srcH * ratio).toInt().coerceAtLeast(1)
    }


    // Util gambar list pasangan label–value dua kolom, auto-wrap
    private fun drawKeyValueColumn(
        canvas: Canvas,
        startY: Int,
        pageW: Int,
        margin: Int,
        lineGap: Int,
        labelPaint: Paint,
        valuePaint: Paint,
        rows: List<Pair<String, String>>
    ): Int {
        var y = startY
        val colGap = dp(14f).toInt()
        val colW = (pageW - margin * 2 - colGap) / 2
        val labelBold = Paint(labelPaint).apply { typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD) }

        // belah jadi dua kolom kurang-lebih seimbang
        val half = (rows.size + 1) / 2
        val left = rows.take(half)
        val right = rows.drop(half)

        fun drawCol(list: List<Pair<String, String>>, x: Int): Int {
            var yy = y
            for ((label, value) in list) {
                // label
                drawMultiline(canvas, "$label:", x, yy, colW, labelBold)
                yy += (labelBold.textSize + 2).toInt()
                // value
                drawMultiline(canvas, value.ifBlank { "-" }, x, yy, colW, valuePaint)
                yy += (valuePaint.textSize + lineGap).toInt()
            }
            return yy
        }

        val yLeft = drawCol(left, margin)
        val yRight = drawCol(right, margin + colW + colGap)
        return max(yLeft, yRight)
    }

    private fun drawMultiline(
        canvas: Canvas,
        text: String,
        x: Int,
        startY: Int,
        maxWidth: Int,
        paint: Paint
    ): Int {
        val words = text.split(' ')
        var line = StringBuilder()
        var y = startY
        for (w in words) {
            val trial = if (line.isEmpty()) w else "${line} $w"
            if (paint.measureText(trial) <= maxWidth) {
                line = StringBuilder(trial)
            } else {
                canvas.drawText(line.toString(), x.toFloat(), y.toFloat(), paint)
                y += (paint.textSize + 2).toInt()
                line = StringBuilder(w)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x.toFloat(), y.toFloat(), paint)
            y += (paint.textSize + 2).toInt()
        }
        return y
    }

    /** Simpan PdfDocument ke folder Download dengan MediaStore (Q+) atau path publik (pre-Q). */
    @Throws(IOException::class)
    private fun savePdfToDownloads(doc: PdfDocument, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Gagal membuat file")
            contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
        } else {
            val outDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!outDir.exists()) outDir.mkdirs()
            val outFile = File(outDir, fileName)
            FileOutputStream(outFile).use { doc.writeTo(it) }
        }
    }

    // ====== UNDUH LAMPIRAN ASLI (tetap seperti sebelumnya) ======
    private fun downloadAllAttachmentsOriginal() {
        val unit = uniqueCode()
        val atts = collectAttachments()
        if (atts.isEmpty()) {
            toast("Tidak ada lampiran.")
            return
        }
        progress(true)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                AttachmentExporter.downloadOriginals(this@DocumentDetailActivity, unit, atts)
            }.onSuccess {
                runOnUiThread {
                    progress(false)
                    toast("Selesai mengunduh ke Download/")
                }
            }.onFailure {
                runOnUiThread {
                    progress(false)
                    toast(it.message ?: "Gagal mengunduh")
                }
            }
        }
    }

    /** Ambil uniqueCode untuk penamaan file. */
    private fun uniqueCode(): String {
        val d = document ?: return "DOC"
        return try {
            val f = d::class.java.getDeclaredField("uniqueCode")
            f.isAccessible = true
            (f.get(d) as? String)?.ifBlank { "DOC" } ?: "DOC"
        } catch (_: Throwable) { "DOC" }
    }

    /** Kumpulkan daftar lampiran jadi List<Attachment> dari model apapun. */
    private fun collectAttachments(): List<Attachment> {
        val d = document ?: return emptyList()
        return try {
            val f = d::class.java.getDeclaredField("attachments")
            f.isAccessible = true
            when (val raw = f.get(d)) {
                is Map<*, *> -> raw.entries.mapNotNull { e ->
                    val name = e.key?.toString()
                    val url = e.value?.toString()
                    if (url.isNullOrBlank()) null else Attachment(url, name)
                }
                is List<*>   -> raw.mapNotNull { it?.toString() }.map { Attachment(it) }
                else -> emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    // ====== Legacy share (masih disimpan kalau butuh) ======
    private fun sharePDF(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share PDF"))
    }

    private fun progress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnPrint.isEnabled = !show
        btnDownload.isEnabled = !show
        btnEdit.isEnabled = !show
        btnDelete.isEnabled = !show
    }

    private fun toast(s: String) =
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
