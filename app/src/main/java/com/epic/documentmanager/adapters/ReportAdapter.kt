package com.epic.documentmanager.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.epic.documentmanager.R
import com.epic.documentmanager.models.MonthlyReport
import com.epic.documentmanager.utils.DateUtils

class ReportAdapter(
    private var reports: List<MonthlyReport>,
    private val onItemClick: (MonthlyReport) -> Unit
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMonth: TextView = itemView.findViewById(R.id.tvMonth)
        val tvYear: TextView = itemView.findViewById(R.id.tvYear)
        val tvTotalDocuments: TextView = itemView.findViewById(R.id.tvTotalDocuments)
        val tvPembelianRumah: TextView = itemView.findViewById(R.id.tvPembelianRumah)
        val tvRenovasiRumah: TextView = itemView.findViewById(R.id.tvRenovasiRumah)
        val tvPemasanganAC: TextView = itemView.findViewById(R.id.tvPemasanganAC)
        val tvPemasanganCCTV: TextView = itemView.findViewById(R.id.tvPemasanganCCTV)
        val tvGeneratedDate: TextView = itemView.findViewById(R.id.tvGeneratedDate)

        init {
            itemView.setOnClickListener {
                onItemClick(reports[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        holder.tvMonth.text = DateUtils.getMonthName(report.month)
        holder.tvYear.text = report.year
        holder.tvTotalDocuments.text = "Total: ${report.totalDocuments}"
        holder.tvPembelianRumah.text = "PR: ${report.pembelianRumahCount}"
        holder.tvRenovasiRumah.text = "RR: ${report.renovasiRumahCount}"
        holder.tvPemasanganAC.text = "AC: ${report.pemasanganACCount}"
        holder.tvPemasanganCCTV.text = "CCTV: ${report.pemasanganCCTVCount}"
        holder.tvGeneratedDate.text = "Generated: ${DateUtils.formatDateTime(report.generatedAt)}"
    }

    override fun getItemCount(): Int = reports.size

    fun updateReports(newReports: List<MonthlyReport>) {
        reports = newReports
        notifyDataSetChanged()
    }
}