package com.epic.documentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.epic.documentmanager.R
import com.epic.documentmanager.utils.Constants
import com.epic.documentmanager.utils.DateUtils
import com.epic.documentmanager.viewmodels.DocumentViewModel

class SearchDocumentActivity : AppCompatActivity() {

    companion object {
        private const val FILTER_ALL = "ALL"
        private const val FILTER_PEMBELIAN = Constants.DOC_TYPE_PEMBELIAN_RUMAH
        private const val FILTER_RENOVASI  = Constants.DOC_TYPE_RENOVASI_RUMAH
        private const val FILTER_AC        = Constants.DOC_TYPE_PEMASANGAN_AC
        private const val FILTER_CCTV      = Constants.DOC_TYPE_PEMASANGAN_CCTV
    }

    private lateinit var viewModel: DocumentViewModel
    private lateinit var toolbar: Toolbar
    private lateinit var etQuery: EditText
    private lateinit var actFilterType: AutoCompleteTextView
    private lateinit var btnSearch: View
    private lateinit var rv: RecyclerView
    private lateinit var progressBar: View
    private lateinit var emptyView: View

    private var currentFilter: String = FILTER_ALL
    private val adapter = RowAdapter { row -> openDetail(row) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_document)

        viewModel = ViewModelProvider(this)[DocumentViewModel::class.java]

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        etQuery       = findViewById(R.id.etQuery)
        actFilterType = findViewById(R.id.actFilterType)
        btnSearch     = findViewById(R.id.btnSearch)
        rv            = findViewById(R.id.recyclerView)
        progressBar   = findViewById(R.id.progressBar)
        emptyView     = findViewById(R.id.emptyView)

        rv.layoutManager = LinearLayoutManager(this) // <-- FIX penting
        rv.adapter = adapter

        setupDropdown()

        viewModel.loading.observe(this) { loading ->
            progressBar.visibility = if (loading == true) View.VISIBLE else View.GONE
        }
        viewModel.searchResult.observe(this) { result ->
            val rows = buildList {
                result.pembelianRumah.forEach { d ->
                    add(Row(FILTER_PEMBELIAN, d.uniqueCode, d.nama, d.noTelepon, d.createdAt, d))
                }
                result.renovasiRumah.forEach { d ->
                    add(Row(FILTER_RENOVASI, d.uniqueCode, d.nama, d.noTelepon, d.createdAt, d))
                }
                result.pemasanganAC.forEach { d ->
                    add(Row(FILTER_AC, d.uniqueCode, d.nama, d.noTelepon, d.createdAt, d))
                }
                result.pemasanganCCTV.forEach { d ->
                    add(Row(FILTER_CCTV, d.uniqueCode, d.nama, d.noTelepon, d.createdAt, d))
                }
            }.sortedByDescending { it.createdAt }

            applyFilterAndShow(rows)
        }

        btnSearch.setOnClickListener { performSearch() }
        intent.getStringExtra("prefill_query")?.let { etQuery.setText(it); performSearch() }
    }

    private fun setupDropdown() {
        val pairs = listOf(
            "All" to FILTER_ALL,
            "Pembelian Rumah" to FILTER_PEMBELIAN,
            "Renovasi Rumah"  to FILTER_RENOVASI,
            "Pemasangan AC"   to FILTER_AC,
            "Pemasangan CCTV" to FILTER_CCTV
        )
        val labels = pairs.map { it.first }
        val map    = pairs.associate { it.first to it.second }

        actFilterType.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, labels))
        actFilterType.setText("All", false)
        actFilterType.setOnItemClickListener { parent, _, pos, _ ->
            val label = parent.getItemAtPosition(pos) as String
            currentFilter = map[label] ?: FILTER_ALL
            applyFilterAndShow(adapter.items.toList())
        }
    }

    private fun performSearch() {
        val q = etQuery.text?.toString()?.trim().orEmpty()
        if (q.isEmpty()) {
            Toast.makeText(this, "Masukkan nama atau kode unik", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.searchDocuments(q)
    }

    private fun applyFilterAndShow(all: List<Row>) {
        val filtered = if (currentFilter == FILTER_ALL) all else all.filter { it.type == currentFilter }
        adapter.submit(filtered)
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        if (filtered.isEmpty()) Toast.makeText(this, "Tidak ada hasil", Toast.LENGTH_SHORT).show()
    }

    private fun openDetail(row: Row) {
        val intent = Intent(this, DocumentDetailActivity::class.java).apply {
            putExtra("documentType", row.type)
            putExtra("document", row.raw as java.io.Serializable)
        }
        startActivity(intent)
    }

    data class Row(
        val type: String,
        val code: String,
        val name: String,
        val phone: String,
        val createdAt: Long,
        val raw: Any
    )

    private class RowAdapter(
        val onClick: (Row) -> Unit
    ) : RecyclerView.Adapter<RowAdapter.VH>() {

        val items = mutableListOf<Row>()

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvCode:  TextView = v.findViewById(R.id.tvCode)
            val tvName:  TextView = v.findViewById(R.id.tvName)
            val tvPhone: TextView = v.findViewById(R.id.tvPhone)
            val tvDate:  TextView = v.findViewById(R.id.tvDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_document_card, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = items[position]
            holder.tvCode.text  = row.code
            holder.tvName.text  = row.name.ifBlank { "—" }
            holder.tvPhone.text = row.phone.ifBlank { "—" }
            holder.tvDate.text  = DateUtils.formatDateTime(row.createdAt)
            holder.itemView.setOnClickListener { onClick(row) }
        }

        override fun getItemCount(): Int = items.size

        fun submit(newItems: List<Row>) {
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = items.size
                override fun getNewListSize() = newItems.size
                override fun areItemsTheSame(o: Int, n: Int) = items[o].code == newItems[n].code
                override fun areContentsTheSame(o: Int, n: Int) = items[o] == newItems[n]
            })
            items.clear()
            items.addAll(newItems)
            diff.dispatchUpdatesTo(this)
        }
    }
}
