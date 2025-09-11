package com.epic.documentmanager.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.epic.documentmanager.R
import com.epic.documentmanager.utils.DateUtils
import com.epic.documentmanager.utils.Downloads
import com.epic.documentmanager.utils.OriginalDownloader
import com.epic.documentmanager.utils.PdfExporter
import java.lang.reflect.Field

class DocumentAdapter<T : Any>(
    private var documents: List<T>,
    private val onItemClick: (T) -> Unit,
    private val onEditClick: (T) -> Unit,
    private val onDeleteClick: (T) -> Unit,
    private var canDelete: Boolean = true
) : RecyclerView.Adapter<DocumentAdapter<T>.DocumentViewHolder>() {

    inner class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)

        val btnEdit: View = itemView.findViewById(R.id.btnEdit)
        val btnDelete: View = itemView.findViewById(R.id.btnDelete)

        // Tambahan tombol
        val btnDownload: View? = itemView.findViewById(R.id.btnDownload) // optional
        val btnPrint: View? = itemView.findViewById(R.id.btnPrint)       // optional

        init {
            itemView.setOnClickListener { onItemClick(documents[adapterPosition]) }
            btnEdit.setOnClickListener { onEditClick(documents[adapterPosition]) }
            btnDelete.setOnClickListener { onDeleteClick(documents[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val doc = documents[position]

        // helper refleksi aman
        fun getString(field: String, alt: String? = null): String? {
            return try {
                val f: Field = doc::class.java.getDeclaredField(field).apply { isAccessible = true }
                (f.get(doc) as? String)?.takeIf { it.isNotBlank() } ?: alt
            } catch (_: Throwable) { alt }
        }
        fun getLong(field: String, alt: Long = 0L): Long {
            return try {
                val f: Field = doc::class.java.getDeclaredField(field).apply { isAccessible = true }
                (f.get(doc) as? Long) ?: alt
            } catch (_: Throwable) { alt }
        }

        // tampilkan teks utama
        val name  = getString("nama") ?: "Unknown"
        val code  = getString("uniqueCode") ?: "-"
        val phone = getString("noTelepon") ?: "-"
        val ts    = getLong("createdAt", 0L)

        holder.tvName.text = name
        holder.tvCode.text = code
        holder.tvPhone.text = phone
        holder.tvDate.text = DateUtils.formatDateTime(ts)

        // Show/hide delete button
        holder.btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE

        // ==== Unduh & Cetak ====
        // Cari informasi file:
        // - storagePath (path di Firebase Storage) → "documents/xxx.ext"
        // - originalName/fileName → nama file waktu upload (kalau tidak ada, fallback dari code)
        // - mimeType/ext opsional
        val storagePath = getString("storagePath")
            ?: getString("filePath")
            ?: getString("path")
            ?: getString("storage") // fallback lain jika ada
        val originalName = getString("originalName")
            ?: getString("fileName")
            ?: (if (!code.isNullOrBlank()) "$code" else null)
        val ext = getString("extension") ?: getString("ext")
        val mime = getString("mimeType")

        // Tombol Unduh Asli
        if (holder.btnDownload != null) {
            if (!storagePath.isNullOrBlank() && !originalName.isNullOrBlank()) {
                val fileName = if (!ext.isNullOrBlank() && !originalName.contains('.')) {
                    "$originalName.$ext"
                } else originalName

                holder.btnDownload.visibility = View.VISIBLE
                holder.btnDownload.setOnClickListener {
                    // NOTE: untuk Android < 10 pastikan sudah minta izin legacy WRITE_EXTERNAL_STORAGE
                    OriginalDownloader.download(
                        context = holder.itemView.context,
                        storagePath = storagePath,
                        fileName = fileName,
                        mimeType = mime ?: Downloads.mimeFromName(fileName)
                    )
                }
            } else {
                holder.btnDownload.visibility = View.GONE
            }
        }

        // Tombol Cetak → PDF
        if (holder.btnPrint != null) {
            if (!storagePath.isNullOrBlank()) {
                // Nama PDF: pakai code kalau ada, kalau tidak pakai originalName
                val base = when {
                    !code.isNullOrBlank() -> code
                    !originalName.isNullOrBlank() -> originalName.substringBeforeLast('.')
                    else -> "dokumen"
                }
                val pdfName = if (base.endsWith(".pdf", true)) base else "$base.pdf"

                holder.btnPrint.visibility = View.VISIBLE
                holder.btnPrint.setOnClickListener {
                    PdfExporter.exportToPdf(
                        context = holder.itemView.context,
                        storagePath = storagePath,
                        outputPdfName = pdfName
                    )
                }
            } else {
                holder.btnPrint.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = documents.size

    fun updateDocuments(newDocuments: List<T>) {
        documents = newDocuments
        notifyDataSetChanged()
    }

    fun setCanDelete(allowed: Boolean) {
        canDelete = allowed
        notifyDataSetChanged()
    }

    fun getDocument(position: Int): T = documents[position]
}
