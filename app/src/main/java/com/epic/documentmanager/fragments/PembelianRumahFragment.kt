// com/epic/documentmanager/fragments/PembelianRumahFragment.kt
package com.epic.documentmanager.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.epic.documentmanager.R
import com.epic.documentmanager.adapters.DocumentImage
import com.epic.documentmanager.adapters.DocumentImageAdapter
import com.epic.documentmanager.models.PembelianRumah
import com.epic.documentmanager.utils.CodeGenerator
import com.epic.documentmanager.utils.Constants
import com.epic.documentmanager.utils.ValidationUtils
import com.epic.documentmanager.viewmodels.DocumentViewModel

class PembelianRumahFragment : Fragment() {

    private val documentViewModel: DocumentViewModel by activityViewModels()
    private lateinit var imageAdapter: DocumentImageAdapter
    private val selectedImages = mutableListOf<DocumentImage>()
    private var editingDocument: PembelianRumah? = null

    // === Guard agar event dari fragment lain diabaikan ===
    private var awaitingUpload = false
    private var awaitingSave = false

    // Views (nullable supaya fleksibel dgn ID)
    private var etNama: EditText? = null
    private var etAlamatKTP: EditText? = null
    private var etNIK: EditText? = null
    private var etNPWP: EditText? = null
    private var etNoTelepon: EditText? = null
    private var spinnerStatusPernikahan: Spinner? = null
    private var etNamaPasangan: EditText? = null
    private var etPekerjaan: EditText? = null
    private var etGaji: EditText? = null
    private var etKontakDarurat: EditText? = null
    private var etTempatKerja: EditText? = null
    private var etNamaPerumahan: EditText? = null
    private var etTipeRumah: EditText? = null
    private var spinnerJenisPembayaran: Spinner? = null
    private var spinnerTipeRumahKategori: Spinner? = null

    private lateinit var recyclerViewImages: RecyclerView
    private lateinit var btnAddImage: View
    private lateinit var btnSave: View
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
        private const val ARG_EDIT_DOCUMENT = "arg_edit_document"

        fun newInstanceForEdit(document: PembelianRumah): PembelianRumahFragment {
            val f = PembelianRumahFragment()
            f.arguments = Bundle().apply { putSerializable(ARG_EDIT_DOCUMENT, document) }
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layoutId = resources.getIdentifier("fragment_pembelian_rumah", "layout", requireContext().packageName)
        val view = inflater.inflate(if (layoutId != 0) layoutId else R.layout.fragment_pembelian_rumah, container, false)
        initViews(view)
        setupRecyclerView()
        setupSpinners()
        setupObservers()
        setupClickListeners()

        arguments?.getSerializable(ARG_EDIT_DOCUMENT)?.let { doc ->
            editingDocument = doc as PembelianRumah
            populateFields(editingDocument!!)
        }

        return view
    }

    private inline fun <reified V : View> View.findAny(vararg names: String): V? {
        for (name in names) {
            val id = resources.getIdentifier(name, "id", context.packageName)
            if (id != 0) return findViewById(id)
        }
        return null
    }

    private fun initViews(view: View) {
        etNama               = view.findAny("etNama")
        etAlamatKTP          = view.findAny("etAlamatKTP", "etAlamat")
        etNIK                = view.findAny("etNIK")
        etNPWP               = view.findAny("etNPWP")
        etNoTelepon          = view.findAny("etNoTelepon", "etPhone")
        spinnerStatusPernikahan = view.findAny("spinnerStatusPernikahan", "spStatusPernikahan")
        etNamaPasangan       = view.findAny("etNamaPasangan")
        etPekerjaan          = view.findAny("etPekerjaan")
        etGaji               = view.findAny("etGaji")
        etKontakDarurat      = view.findAny("etKontakDarurat")
        etTempatKerja        = view.findAny("etTempatKerja")
        etNamaPerumahan      = view.findAny("etNamaPerumahan")
        etTipeRumah          = view.findAny("etTipeRumah")
        spinnerJenisPembayaran   = view.findAny("spinnerJenisPembayaran", "spJenisPembayaran")
        spinnerTipeRumahKategori = view.findAny("spinnerTipeRumahKategori", "spTipeRumahKategori")

        recyclerViewImages = view.findAny("recyclerViewImages", "rvImages")!!
        btnAddImage = view.findAny("btnAddImage", "btnTambahDokumen", "btnAddAttachment")!!
        btnSave = view.findAny("btnSave")!!
        progressBar = view.findAny("progressBar") ?: ProgressBar(requireContext()).apply { visibility = View.GONE }
    }

    private fun setupRecyclerView() {
        imageAdapter = DocumentImageAdapter(
            images = selectedImages,
            onImageClick = { _, _ -> },
            onDeleteClick = { _, pos -> selectedImages.removeAt(pos).also { imageAdapter.notifyItemRemoved(pos) } }
        )
        recyclerViewImages.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerViewImages.adapter = imageAdapter
    }

    private fun setupSpinners() {
        spinnerStatusPernikahan?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, arrayOf("Belum Menikah","Menikah","Cerai")).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerJenisPembayaran?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, arrayOf("KPR","Cash","Inhouse")).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerTipeRumahKategori?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, arrayOf("Subsidi","Cluster","Secondary")).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun setupObservers() {
        documentViewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            if (!awaitingUpload) return@observe
            if (result.isSuccess) {
                saveDocumentWithImages(result.getOrNull() ?: emptyList())
            } else {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                awaitingUpload = false
                Toast.makeText(requireContext(), "Upload gagal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        documentViewModel.saveResult.observe(viewLifecycleOwner) { result ->
            if (!awaitingSave) return@observe
            progressBar.visibility = View.GONE
            btnSave.isEnabled = true
            awaitingSave = false
            awaitingUpload = false
            if (result.isSuccess) {
                Toast.makeText(requireContext(), "Data tersimpan", Toast.LENGTH_SHORT).show()
                requireActivity().setResult(Activity.RESULT_OK)   // tambahkan
                requireActivity().finish()                        // tambahkan
            } else {
                Toast.makeText(requireContext(), "Gagal simpan: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        btnAddImage.setOnClickListener { pickImage() }
        btnSave.setOnClickListener { if (validateForm()) saveDocument() }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","image/*"))
        }
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun populateFields(d: PembelianRumah) {
        etNama?.setText(d.nama)
        etAlamatKTP?.setText(d.alamatKTP)
        etNIK?.setText(d.nik)
        etNPWP?.setText(d.npwp)
        etNoTelepon?.setText(d.noTelepon)
        etNamaPasangan?.setText(d.namaPasangan)
        etPekerjaan?.setText(d.pekerjaan)
        etGaji?.setText(d.gaji)
        etKontakDarurat?.setText(d.kontakDarurat)
        etTempatKerja?.setText(d.tempatKerja)
        etNamaPerumahan?.setText(d.namaPerumahan)
        etTipeRumah?.setText(d.tipeRumah)
        spinnerStatusPernikahan?.setSelection((spinnerStatusPernikahan?.adapter as? ArrayAdapter<String>)?.getPosition(d.statusPernikahan) ?: 0)
        spinnerJenisPembayaran?.setSelection((spinnerJenisPembayaran?.adapter as? ArrayAdapter<String>)?.getPosition(d.jenisPembayaran) ?: 0)
        spinnerTipeRumahKategori?.setSelection((spinnerTipeRumahKategori?.adapter as? ArrayAdapter<String>)?.getPosition(d.tipeRumahKategori) ?: 0)

        d.attachments.forEach { (name, url) -> selectedImages.add(DocumentImage(name = name, url = url)) }
        imageAdapter.notifyDataSetChanged()
    }

    private fun validateForm(): Boolean {
        val fields = mapOf(
            "Nama" to (etNama?.text?.toString()?.trim() ?: ""),
            "Alamat KTP/Alamat" to (etAlamatKTP?.text?.toString()?.trim() ?: ""),
            "No. Telepon" to (etNoTelepon?.text?.toString()?.trim() ?: "")
        )
        val errors = ValidationUtils.validateForm(fields)
        if (errors.isNotEmpty()) {
            Toast.makeText(requireContext(), errors.first(), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveDocument() {
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        // proses ini milik fragment ini
        awaitingUpload = true
        awaitingSave = false

        val localImages = selectedImages.filter { it.uri != null }
        if (localImages.isNotEmpty()) {
            // >>> perbaikan: gunakan 'localImages', bukan 'locals'
            documentViewModel.uploadFiles(
                requireContext(),
                localImages.map { it.uri!! },
                Constants.DOC_TYPE_PEMBELIAN_RUMAH,
                localImages.map { it.name }
            )
        } else {
            saveDocumentWithImages(emptyList())
        }
    }

    private fun saveDocumentWithImages(uploadedUrls: List<String>) {
        // saat menyimpan, aktifkan guard save
        awaitingSave = true

        val allAttachments = linkedMapOf<String, String>()
        selectedImages.filter { it.url.isNotEmpty() }.forEach { allAttachments[it.name] = it.url }
        val locals = selectedImages.filter { it.uri != null }
        uploadedUrls.forEachIndexed { i, url -> allAttachments[locals.getOrNull(i)?.name ?: "file_${i+1}"] = url }

        val d = editingDocument
        val doc = PembelianRumah(
            id = d?.id ?: "",
            uniqueCode = d?.uniqueCode ?: CodeGenerator.generateCodeForPembelianRumah(),
            nama = etNama?.text?.toString()?.trim() ?: "",
            alamatKTP = etAlamatKTP?.text?.toString()?.trim() ?: "",
            nik = etNIK?.text?.toString()?.trim() ?: "",
            npwp = etNPWP?.text?.toString()?.trim() ?: "",
            noTelepon = etNoTelepon?.text?.toString()?.trim() ?: "",
            statusPernikahan = spinnerStatusPernikahan?.selectedItem?.toString() ?: "",
            namaPasangan = etNamaPasangan?.text?.toString()?.trim() ?: "",
            pekerjaan = etPekerjaan?.text?.toString()?.trim() ?: "",
            gaji = etGaji?.text?.toString()?.trim() ?: "",
            kontakDarurat = etKontakDarurat?.text?.toString()?.trim() ?: "",
            tempatKerja = etTempatKerja?.text?.toString()?.trim() ?: "",
            namaPerumahan = etNamaPerumahan?.text?.toString()?.trim() ?: "",
            tipeRumah = etTipeRumah?.text?.toString()?.trim() ?: "",
            jenisPembayaran = spinnerJenisPembayaran?.selectedItem?.toString() ?: "",
            tipeRumahKategori = spinnerTipeRumahKategori?.selectedItem?.toString() ?: "",
            createdAt = d?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = d?.createdBy ?: "",
            updatedBy = d?.updatedBy ?: "",
            status = d?.status ?: "active",
            attachments = allAttachments
        )
        documentViewModel.savePembelianRumah(doc)
    }

    private fun clearForm() { /* tak dipakai saat edit, biarkan kosong */ }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            fun addOne(uri: android.net.Uri) { selectedImages.add(DocumentImage("Document_${System.currentTimeMillis()}", uri)) }
            val clip = data?.clipData
            if (clip != null && clip.itemCount > 0) {
                for (i in 0 until clip.itemCount) addOne(clip.getItemAt(i).uri)
                imageAdapter.notifyDataSetChanged()
            } else data?.data?.let { uri -> addOne(uri); imageAdapter.notifyItemInserted(selectedImages.size - 1) }
        }
    }
}
