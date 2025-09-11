package com.epic.documentmanager.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epic.documentmanager.databinding.ItemAttachmentBinding

data class AttachmentItem(
    val displayName: String
)

class AttachmentAdapter(
    private val items: MutableList<AttachmentItem>,
    private val onRemove: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<AttachmentAdapter.VH>() {

    inner class VH(val b: ItemAttachmentBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemAttachmentBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvName.text = item.displayName
        holder.b.btnRemove.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onRemove?.invoke(pos)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
