package com.epic.documentmanager.ui.staff

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.epic.documentmanager.R
import com.epic.documentmanager.data.mappers.User

class StaffAdapter(
    private var items: List<User> = emptyList(),
    private val onEdit: (User) -> Unit,
    private val onDelete: (User) -> Unit
) : RecyclerView.Adapter<StaffAdapter.VH>() {

    fun submit(list: List<User>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_staff, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val u = items[position]
        h.name.text = u.name.ifBlank { "Unknown" }
        h.email.text = u.email.ifBlank { "—" }
        h.role.text = "Role: ${u.role}"
        h.status.text = if (u.isActive) "Aktif" else "Nonaktif"
        h.btnEdit.setOnClickListener { onEdit(u) }
        h.btnDelete.setOnClickListener { onDelete(u) }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvName)
        val email: TextView = v.findViewById(R.id.tvEmail)
        val role: TextView = v.findViewById(R.id.tvRole)
        val status: TextView = v.findViewById(R.id.tvStatus)
        val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
    }
}
