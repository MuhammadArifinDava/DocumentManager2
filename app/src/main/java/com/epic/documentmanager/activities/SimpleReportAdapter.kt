package com.epic.documentmanager.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.epic.documentmanager.R
import com.epic.documentmanager.models.*

class SimpleReportAdapter : RecyclerView.Adapter<SimpleReportAdapter.VH>() {
    private val items = mutableListOf<Row>()
    data class Row(val title: String, val subtitle: String)

    fun submit(
        year: Int,
        pembelian: List<PembelianRumah>,
        renovasi: List<RenovasiRumah>,
        ac: List<PemasanganAC>,
        cctv: List<PemasanganCCTV>
    ) {
        items.clear()
        items += pembelian.map { Row(it.uniqueCode, "Pembelian Rumah • ${it.nama}") }
        items += renovasi.map  { Row(it.uniqueCode, "Renovasi Rumah • ${it.nama}") }
        items += ac.map        { Row(it.uniqueCode, "Pemasangan AC • ${it.nama}") }
        items += cctv.map      { Row(it.uniqueCode, "Pemasangan CCTV • ${it.nama}") }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(p: ViewGroup, vType: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_simple_report, p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = items[pos]
        h.title.text = r.title
        h.sub.text   = r.subtitle
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvTitle)
        val sub:   TextView = v.findViewById(R.id.tvSubtitle)
    }
}
