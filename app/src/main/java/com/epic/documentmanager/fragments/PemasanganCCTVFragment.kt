package com.epic.documentmanager.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import com.epic.documentmanager.models.PemasanganCCTV
import com.epic.documentmanager.utils.CodeGenerator
import com.epic.documentmanager.utils.Constants
import com.epic.documentmanager.utils.ValidationUtils
import com.epic.documentmanager.viewmodels.DocumentViewModel

class PemasanganCCTVFragment : Fragment() {

    private val documentViewModel: DocumentViewModel by activityViewModels()
    private lateinit var imageAdapter: DocumentImageAdapter
    private val selectedImages = mutableListOf<DocumentImage>()
    private var editingDocument: PemasanganCCTV? = null

    // === Guard agar event dari fragment lain diabaikan ===
    private var awaitingUpload = false
    private var awaitingSave = false

    private lateinit var etNama: EditText
    private lateinit var etAlamat: EditText
    private lateinit var etNoTelepon: EditText
    private lateinit var etJumlahUnit: EditText
    private lateinit var recyclerViewImages: RecyclerView
    private lateinit var btnAddImage: View
    private lateinit var btnSave: View
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val REQUEST_IMAGE_PICK = 4001
        private const val ARG_EDIT_DOCUMENT = "arg_edit_document"
        fun newInstanceForEdit(document: PemasanganCCTV): PemasanganCCTVFragment {
            val f = PemasanganCCTVFragment()
            f.arguments = Bundle().apply { putSerializable(ARG_EDIT_DOCUMENT, document) }
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setHasOptionsMenu(false) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layoutId = resources.getIdentifier("fragment_pemasangan_cctv", "layout", requireContext().packageName)
        val view = inflater.inflate(if (layoutId != 0) layoutId else R.layout.fragment_pemasangan_cctv, container, false)

        etNama = view.findViewById(resources.getIdentifier("etNama", "id", requireContext().packageName))
        etAlamat = view.findViewById(resources.getIdentifier("etAlamat", "id", requireContext().packageName))
        etNoTelepon = view.findViewById(resources.getIdentifier("etNoTelepon", "id", requireContext().packageName))
        etJumlahUnit = view.findViewById(resources.getIdentifier("etJumlahUnit", "id", requireContext().packageName))
        recyclerViewImages = view.findViewById(resources.getIdentifier("recyclerViewImages", "id", requireContext().packageName))
        btnAddImage = view.findViewById(resources.getIdentifier("btnAddImage", "id", requireContext().packageName))
        btnSave = view.findViewById(resources.getIdentifier("btnSave", "id", requireContext().packageName))
        progressBar = view.findViewById(resources.getIdentifier("progressBar", "id", requireContext().packageName))

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        arguments?.getSerializable(ARG_EDIT_DOCUMENT)?.let { editingDocument = it as PemasanganCCTV; populateFields(editingDocument!!) }
        return view
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

    private fun setupObservers() {
        documentViewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            if (!awaitingUpload) return@observe
            if (result.isSuccess) {
                saveDocumentWithImages(result.getOrNull() ?: emptyList())
            } else {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                awaitingUpload = false
                Toast.makeText(requireContext(),"Upload gagal: ${result.exceptionOrNull()?.message}",Toast.LENGTH_SHORT).show()
            }
        }

        documentViewModel.saveResult.observe(viewLifecycleOwner) { result ->
            if (!awaitingSave) return@observe
            progressBar.visibility = View.GONE; btnSave.isEnabled = true
            awaitingSave = false
            awaitingUpload = false
            if (result.isSuccess) {
                Toast.makeText(requireContext(), "Data tersimpan", Toast.LENGTH_SHORT).show()
                requireActivity().setResult(Activity.RESULT_OK)  // tambahkan
                requireActivity().finish()                       // tambahkan
            } else Toast.makeText(requireContext(),"Gagal simpan: ${result.exceptionOrNull()?.message}",Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        btnAddImage.setOnClickListener { pickImage() }
        btnSave.setOnClickListener { if (validateForm()) saveDocument() }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","image/*"))
        }
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun populateFields(d: PemasanganCCTV) {
        etNama.setText(d.nama); etAlamat.setText(d.alamat); etNoTelepon.setText(d.noTelepon); etJumlahUnit.setText(d.jumlahUnit.toString())
        d.attachments.forEach { (n,u) -> selectedImages.add(DocumentImage(n, url = u)) }; imageAdapter.notifyDataSetChanged()
    }

    private fun validateForm(): Boolean {
        val fields = mapOf("Nama" to etNama.text.toString().trim(), "Alamat" to etAlamat.text.toString().trim(), "No. Telepon" to etNoTelepon.text.toString().trim())
        val errors = ValidationUtils.validateForm(fields)
        if (errors.isNotEmpty()) { Toast.makeText(requireContext(), errors.first(), Toast.LENGTH_SHORT).show(); return false }
        return true
    }

    private fun saveDocument() {
        progressBar.visibility = View.VISIBLE; btnSave.isEnabled = false

        // proses ini milik fragment ini
        awaitingUpload = true
        awaitingSave = false

        val locals = selectedImages.filter { it.uri != null }
        if (locals.isNotEmpty()) documentViewModel.uploadFiles(requireContext(), locals.map { it.uri!! }, Constants.DOC_TYPE_PEMASANGAN_CCTV, locals.map { it.name })
        else saveDocumentWithImages(emptyList())
    }

    private fun saveDocumentWithImages(uploadedUrls: List<String>) {
        // saat menyimpan, aktifkan guard save
        awaitingSave = true

        val all = linkedMapOf<String,String>()
        selectedImages.filter { it.url.isNotEmpty() }.forEach { all[it.name] = it.url }
        val locals = selectedImages.filter { it.uri != null }
        uploadedUrls.forEachIndexed { i,u -> all[locals.getOrNull(i)?.name ?: "file_${i+1}"] = u }

        val d = editingDocument
        val doc = PemasanganCCTV(
            id = d?.id ?: "",
            uniqueCode = d?.uniqueCode ?: CodeGenerator.generateCodeForPemasanganCCTV(),
            nama = etNama.text.toString().trim(),
            alamat = etAlamat.text.toString().trim(),
            noTelepon = etNoTelepon.text.toString().trim(),
            jumlahUnit = etJumlahUnit.text.toString().toIntOrNull() ?: 0,
            createdAt = d?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = d?.createdBy ?: "",
            updatedBy = d?.updatedBy ?: "",
            status = d?.status ?: "active",
            attachments = all
        )
        documentViewModel.savePemasanganCCTV(doc)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            fun addOne(uri: Uri) { selectedImages.add(DocumentImage("Document_${System.currentTimeMillis()}", uri)) }
            val clip = data?.clipData
            if (clip != null && clip.itemCount > 0) { for (i in 0 until clip.itemCount) addOne(clip.getItemAt(i).uri); imageAdapter.notifyDataSetChanged() }
            else data?.data?.let { uri -> addOne(uri); imageAdapter.notifyItemInserted(selectedImages.size - 1) }
        }
    }
}
