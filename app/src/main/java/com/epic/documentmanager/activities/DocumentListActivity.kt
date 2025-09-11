package com.epic.documentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
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

class DocumentListActivity : AppCompatActivity() {

    companion object {
        const val FILTER_ALL = "ALL"
        const val FILTER_PEMBELIAN = Constants.DOC_TYPE_PEMBELIAN_RUMAH
        const val FILTER_RENOVASI  = Constants.DOC_TYPE_RENOVASI_RUMAH
        const val FILTER_AC        = Constants.DOC_TYPE_PEMASANGAN_AC
        const val FILTER_CCTV      = Constants.DOC_TYPE_PEMASANGAN_CCTV
    }

    private lateinit var viewModel: DocumentViewModel

    private lateinit var toolbar: Toolbar
    private lateinit var actFilterType: AutoCompleteTextView
    private lateinit var btnFilter: View
    private lateinit var rv: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: View

    private val adapter = RowAdapter { row -> openDetail(row) }
    private var allRows: List<Row> = emptyList()
    private var currentFilter: String = FILTER_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_list)

        viewModel = ViewModelProvider(this)[DocumentViewModel::class.java]

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        actFilterType = findViewById(R.id.actFilterType)
        btnFilter     = findViewById(R.id.btnFilter)
        rv            = findViewById(R.id.recyclerView)
        progressBar   = findViewById(R.id.progressBar)
        emptyView     = findViewById(R.id.emptyView)

        rv.layoutManager = LinearLayoutManager(this) // <-- FIX penting
        rv.adapter = adapter

        setupDropdown()
        btnFilter.setOnClickListener { applyFilterAndShow() }

        // Observers
        viewModel.loading.observe(this) { loading ->
            progressBar.visibility = if (loading == true) View.VISIBLE else View.GONE
        }
        viewModel.pembelianRumahList.observe(this) { rebuildRows() }
        viewModel.renovasiRumahList.observe(this)  { rebuildRows() }
        viewModel.pemasanganACList.observe(this)   { rebuildRows() }
        viewModel.pemasanganCCTVList.observe(this) { rebuildRows() }

        // Load semua kategori di awal
        viewModel.loadAllDocuments()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_document_list, menu) // ada ikon search
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                startActivity(Intent(this, SearchDocumentActivity::class.java))
                true
            }
            android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
            else -> super.onOptionsItemSelected(item)
        }
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
        }
    }

    private fun rebuildRows() {
        val rows = mutableListOf<Row>()
        viewModel.pembelianRumahList.value?.forEach { d ->
            rows.add(Row(FILTER_PEMBELIAN, d.uniqueCode, d.nama, d.noTelepon, d.createdAt, d))
        }
        viewModel.renovasiRumahList.value?.forEach { d ->
            rows.add(Row(FILTER_RENOVASI, d.uniqueCode, d.nama, d.noTelepon, d.createdAt, d))
        }
        viewModel.pemasanganACList.value?.forEach { d ->
            rows.add(Row(FILTER_AC, d.uniqueCode, d.nama, d.noTelepon, d.createdAt, d))
        }
        viewModel.pemasanganCCTVList.value?.forEach { d ->
            rows.add(Row(FILTER_CCTV, d.uniqueCode, d.nama, d.noTelepon, d.createdAt, d))
        }
        allRows = rows.sortedByDescending { it.createdAt }
        applyFilterAndShow()
    }

    private fun applyFilterAndShow() {
        val filtered = if (currentFilter == FILTER_ALL) allRows else allRows.filter { it.type == currentFilter }
        adapter.submit(filtered)
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
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

        private val items = mutableListOf<Row>()

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
