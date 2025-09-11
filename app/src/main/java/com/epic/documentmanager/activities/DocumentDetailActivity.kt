package com.epic.documentmanager.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.epic.documentmanager.utils.Constants
import com.epic.documentmanager.utils.DateUtils
import com.epic.documentmanager.utils.Attachment
import com.epic.documentmanager.utils.AttachmentExporter
import com.epic.documentmanager.viewmodels.AuthViewModel
import com.epic.documentmanager.viewmodels.DocumentViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.Serializable

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

        // EKSPOR PDF (konversi semua lampiran jadi PDF)
        btnPrint.setOnClickListener {
            requireStorageIfNeeded {
                exportAllAttachmentsToPdf()
            }
        }

        // UNDUH (file asli)
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

    // ===== CETAK (EKSPOR PDF) & UNDUH =====

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

    private fun exportAllAttachmentsToPdf() {
        val unit = uniqueCode()
        val atts = collectAttachments()
        if (atts.isEmpty()) {
            toast("Tidak ada lampiran.")
            return
        }
        progress(true)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                AttachmentExporter.exportAsPdf(this@DocumentDetailActivity, unit, atts)
            }.onSuccess {
                runOnUiThread {
                    progress(false)
                    toast("Selesai ekspor PDF ke Download/")
                }
            }.onFailure {
                runOnUiThread {
                    progress(false)
                    toast(it.message ?: "Gagal ekspor PDF")
                }
            }
        }
    }

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
                is Map<*, *> -> raw.values.mapNotNull { it?.toString() }.map { Attachment(it) }
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
