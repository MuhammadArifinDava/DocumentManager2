package com.epic.documentmanager.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.epic.documentmanager.R
import com.epic.documentmanager.models.PemasanganCCTV
import com.epic.documentmanager.navigation.EditNavigator

class PemasanganCCTVAdapter(
    private val items: MutableList<PemasanganCCTV>
) : RecyclerView.Adapter<PemasanganCCTVAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNama: TextView = v.findViewById(R.id.tvNama)
        val tvKode: TextView = v.findViewById(R.id.tvKode)
        val btnEdit: View? = v.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pemasangan_cctv, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNama.text = item.nama
        holder.tvKode.text = item.uniqueCode
        holder.btnEdit?.setOnClickListener {
            EditNavigator.goToEdit(holder.itemView.context, item)
        }
    }

    fun replaceAll(newItems: List<PemasanganCCTV>) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged()
    }
}
