package com.epic.documentmanager.activities

import android.Manifest
import android.content.Intent
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.epic.documentmanager.R
import com.epic.documentmanager.utils.ReportPdfWriter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthlyReportActivity : AppCompatActivity() {

    private lateinit var actMonth: MaterialAutoCompleteTextView
    private lateinit var actYear: MaterialAutoCompleteTextView

    private lateinit var secPembelian: LinearLayout
    private lateinit var secRenovasi: LinearLayout
    private lateinit var secAC: LinearLayout
    private lateinit var secCCTV: LinearLayout
    private lateinit var tvTotalAll: TextView

    private lateinit var btnDownload: View

    private var cal = Calendar.getInstance()
    private var startMillis = 0L
    private var endMillis = 0L

    // >> HAPUS field unit
    data class Row(
        val nama: String,
        val alamat: String,
        val telp: String
    )

    private val rowsPembelian = mutableListOf<Row>()
    private val rowsRenovasi = mutableListOf<Row>()
    private val rowsAC = mutableListOf<Row>()
    private val rowsCCTV = mutableListOf<Row>()

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null

    private val months = listOf(
        "01 - Januari", "02 - Februari", "03 - Maret", "04 - April",
        "05 - Mei", "06 - Juni", "07 - Juli", "08 - Agustus",
        "09 - September", "10 - Oktober", "11 - November", "12 - Desember"
    )
    private val years = (2022..Calendar.getInstance().get(Calendar.YEAR)).map { it.toString() }

    private val requestWritePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(this, "Izin penyimpanan ditolak", Toast.LENGTH_SHORT).show()
        else onClickDownload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_report)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGoYearly)
            .setOnClickListener { startActivity(Intent(this, YearlyReportActivity::class.java)) }

        findViewById<Toolbar>(R.id.toolbarMonthly)?.let { tb ->
            setSupportActionBar(tb)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            tb.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        actMonth = findViewById(R.id.actMonth)
        actYear = findViewById(R.id.actYear)
        secPembelian = findViewById(R.id.containerPembelian)
        secRenovasi  = findViewById(R.id.containerRenovasi)
        secAC        = findViewById(R.id.containerAC)
        secCCTV      = findViewById(R.id.containerCCTV)
        tvTotalAll   = findViewById(R.id.tvTotalAll)
        btnDownload  = findViewById(R.id.btnDownloadPdf)

        actMonth.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, months))
        actYear.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, years))

        val nowMonth = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val nowYear = cal.get(Calendar.YEAR).toString()
        actMonth.setText("$nowMonth - ${months[nowMonth.toInt() - 1].substring(5)}", false)
        actYear.setText(nowYear, false)

        actMonth.setOnItemClickListener { _, _, _, _ -> recalcRangeAndReload() }
        actYear.setOnItemClickListener { _, _, _, _ -> recalcRangeAndReload() }

        run {
            val reloadId = resources.getIdentifier("btnReload", "id", packageName)
            if (reloadId != 0) findViewById<View>(reloadId)?.setOnClickListener { recalcRangeAndReload() }
        }

        btnDownload.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestWritePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    return@setOnClickListener
                }
            }
            onClickDownload()
        }

        recalcRangeAndReload()
    }

    private fun recalcRangeAndReload() {
        val pickedMonth = actMonth.text?.toString()?.take(2)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)
        val pickedYear = actYear.text?.toString()?.toIntOrNull() ?: cal.get(Calendar.YEAR)

        cal.clear()
        cal.set(Calendar.YEAR, pickedYear)
        cal.set(Calendar.MONTH, pickedMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        startMillis = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        endMillis = cal.timeInMillis

        loadJob?.cancel()

        rowsPembelian.clear(); rowsRenovasi.clear(); rowsAC.clear(); rowsCCTV.clear()
        renderAll()

        loadJob = scope.launch {
            try {
                val p = async { fetchCollection("pembelian_rumah") }
                val r = async { fetchCollection("renovasi_rumah") }
                val a = async { fetchCollection("pemasangan_ac") }
                val c = async { fetchCollection("pemasangan_cctv") }

                rowsPembelian.setAll(p.await())
                rowsRenovasi.setAll(r.await())
                rowsAC.setAll(a.await())
                rowsCCTV.setAll(c.await())

                renderAll()
            } catch (e: Throwable) {
                if (e !is CancellationException) {
                    Toast.makeText(this@MonthlyReportActivity, e.message ?: "Gagal memuat laporan", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun MutableList<Row>.setAll(list: List<Row>) { clear(); addAll(list) }

    private suspend fun fetchCollection(colName: String): List<Row> = withContext(Dispatchers.IO) {
        val out = mutableListOf<Row>()

        try {
            val q1 = db.collection(colName)
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("createdAt", Timestamp(startMillis / 1000, 0))
                .whereLessThan("createdAt", Timestamp(endMillis / 1000, 0))
                .get(Source.SERVER).await()
            for (doc in q1) out += mapRow(doc.data ?: emptyMap())
        } catch (_: Throwable) {}

        if (out.isEmpty()) {
            try {
                val q2 = db.collection(colName)
                    .whereEqualTo("status", "active")
                    .whereGreaterThanOrEqualTo("createdAt", startMillis)
                    .whereLessThan("createdAt", endMillis)
                    .get(Source.SERVER).await()
                for (doc in q2) out += mapRow(doc.data ?: emptyMap())
            } catch (_: Throwable) {}
        }

        if (out.isEmpty()) {
            try {
                val all = db.collection(colName).get(Source.CACHE).await()
                for (doc in all) {
                    val data = doc.data ?: continue
                    val status = (data["status"] as? String)?.lowercase(Locale.ROOT)
                    if (status != null && status != "active") continue
                    val created = extractMillis(data)
                    if (created != null && created in startMillis until endMillis) {
                        out += mapRow(data)
                    }
                }
            } catch (_: Throwable) {}
        }

        if (out.isEmpty()) {
            val all = db.collection(colName).get(Source.SERVER).await()
            for (doc in all) {
                val data = doc.data ?: continue
                val status = (data["status"] as? String)?.lowercase(Locale.ROOT)
                if (status != null && status != "active") continue
                val created = extractMillis(data)
                if (created != null && created in startMillis until endMillis) {
                    out += mapRow(data)
                }
            }
        }

        out.distinctBy { "${it.nama}|${it.alamat}|${it.telp}" }
    }

    private fun extractMillis(d: Map<String, Any?>): Long? {
        (d["createdAt"] as? com.google.firebase.Timestamp)?.let { return it.toDate().time }
        (d["createdAt"] as? Number)?.let { return it.toLong() }
        (d["createdAtMillis"] as? Number)?.let { return it.toLong() }
        (d["createdAt"] as? Map<*, *>)?.let { m ->
            val sec = (m["_seconds"] as? Number)?.toLong()
            if (sec != null) return sec * 1000
        }
        return null
    }

    // >> mapping TANPA unit
    private fun mapRow(d: Map<String, Any?>): Row {
        val nama = (d["nama"] ?: d["namaPembeli"] ?: d["name"] ?: "-").toString()
        val alamat = (d["alamat"] ?: d["alamatKTP"] ?: d["address"] ?: "-").toString()
        val telp = (d["noTelepon"] ?: d["telepon"] ?: d["phone"] ?: "-").toString()
        return Row(nama, alamat, telp)
    }

    private fun renderAll() {
        renderSection(secPembelian, rowsPembelian)
        renderSection(secRenovasi,  rowsRenovasi)
        renderSection(secAC,        rowsAC)
        renderSection(secCCTV,      rowsCCTV)
        val totalDocs = rowsPembelian.size + rowsRenovasi.size + rowsAC.size + rowsCCTV.size
        tvTotalAll.text = "Total Dokumen: $totalDocs"
    }

    private fun renderSection(container: LinearLayout, rows: List<Row>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)

        rows.forEach { r ->
            val v = inflater.inflate(R.layout.item_report_row, container, false)
            v.findViewById<TextView>(R.id.colNama)?.text   = r.nama
            v.findViewById<TextView>(R.id.colAlamat)?.text = r.alamat
            v.findViewById<TextView>(R.id.colTelp)?.text   = r.telp
            // Tidak lagi mengisi colUnit
            container.addView(v)
        }

        val total = rows.size
        val tv = TextView(this).apply {
            setTextColor(ContextCompat.getColor(this@MonthlyReportActivity, android.R.color.darker_gray))
            text = "Total Dokumen: $total"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * resources.displayMetrics.density).toInt() }
        }
        container.addView(tv)
    }

    private fun onClickDownload() {
        if (rowsPembelian.isEmpty() && rowsRenovasi.isEmpty() && rowsAC.isEmpty() && rowsCCTV.isEmpty()) {
            Toast.makeText(this, "Tidak ada data laporan untuk periode ini", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            val fileName = "Laporan_Bulanan_${sdf.format(System.currentTimeMillis())}.pdf"

            val doc = PdfDocument()
            val writer = ReportPdfWriter(
                context = this,
                title   = "Laporan Bulanan",
                subtitle = "Periode: ${actMonth.text} ${actYear.text}",
                rowsPembelian = rowsPembelian.map { ReportPdfWriter.Row(it.nama, it.alamat, it.telp) },
                rowsRenovasi  = rowsRenovasi .map { ReportPdfWriter.Row(it.nama, it.alamat, it.telp) },
                rowsAC        = rowsAC       .map { ReportPdfWriter.Row(it.nama, it.alamat, it.telp) },
                rowsCCTV      = rowsCCTV     .map { ReportPdfWriter.Row(it.nama, it.alamat, it.telp) },
            )
            writer.writeTo(doc)

            com.epic.documentmanager.utils.ReportSaver.saveToDownloads(this, fileName, doc)
            // ReportSaver akan close() doc & tampilkan toast
        } catch (e: Throwable) {
            Toast.makeText(this, e.message ?: "Gagal membuat PDF", Toast.LENGTH_LONG).show()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
