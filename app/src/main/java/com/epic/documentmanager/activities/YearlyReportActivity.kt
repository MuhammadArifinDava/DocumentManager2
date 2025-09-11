package com.epic.documentmanager.activities

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.epic.documentmanager.R
import com.epic.documentmanager.utils.ReportPdfWriter
import com.google.android.material.button.MaterialButton
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

class YearlyReportActivity : AppCompatActivity() {

    // ==== UI ====
    private lateinit var actYear: MaterialAutoCompleteTextView
    private lateinit var btnExport: MaterialButton

    private lateinit var secPembelian: LinearLayout
    private lateinit var secRenovasi: LinearLayout
    private lateinit var secAC: LinearLayout
    private lateinit var secCCTV: LinearLayout
    private lateinit var tvTotalAll: TextView
    private lateinit var progress: View

    // ==== Data rows (3 kolom) ====
    data class Row(
        val nama: String,
        val alamat: String,
        val telp: String
    )

    private val rowsPembelian = mutableListOf<Row>()
    private val rowsRenovasi  = mutableListOf<Row>()
    private val rowsAC        = mutableListOf<Row>()
    private val rowsCCTV      = mutableListOf<Row>()

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null

    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)
    private var startMillis = 0L
    private var endMillis = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yearly_report)

        // Toolbar
        findViewById<Toolbar>(R.id.toolbar)?.let { tb ->
            setSupportActionBar(tb)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            tb.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        // Bind
        actYear      = findViewById(R.id.actYear)
        btnExport    = findViewById(R.id.btnExport)
        secPembelian = findViewById(R.id.containerPembelian)
        secRenovasi  = findViewById(R.id.containerRenovasi)
        secAC        = findViewById(R.id.containerAC)
        secCCTV      = findViewById(R.id.containerCCTV)
        tvTotalAll   = findViewById(R.id.tvTotalAll)
        progress     = findViewById(R.id.progressBar)

        // Dropdown tahun 2005..2027
        val years = (2005..2027).map { it.toString() }
        actYear.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, years))
        selectedYear = Calendar.getInstance().get(Calendar.YEAR).coerceIn(2005, 2027)
        actYear.setText(selectedYear.toString(), false)
        actYear.setOnItemClickListener { _, _, pos, _ ->
            selectedYear = years[pos].toInt()
            recalcRangeAndReload()
        }

        btnExport.setOnClickListener { onClickDownload() }

        recalcRangeAndReload()
    }

    private fun recalcRangeAndReload() {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        startMillis = cal.timeInMillis

        cal.add(Calendar.YEAR, 1)
        endMillis = cal.timeInMillis

        loadJob?.cancel()
        rowsPembelian.clear(); rowsRenovasi.clear(); rowsAC.clear(); rowsCCTV.clear()
        renderAll()

        loadJob = scope.launch {
            setLoading(true)
            try {
                val p = async { fetchCollection("pembelian_rumah") }
                val r = async { fetchCollection("renovasi_rumah") }
                val a = async { fetchCollection("pemasangan_ac") }
                val c = async { fetchCollection("pemasangan_cctv") }

                rowsPembelian.clear(); rowsPembelian.addAll(p.await())
                rowsRenovasi.clear();  rowsRenovasi.addAll(r.await())
                rowsAC.clear();        rowsAC.addAll(a.await())
                rowsCCTV.clear();      rowsCCTV.addAll(c.await())

                renderAll()
            } catch (e: Throwable) {
                if (e !is CancellationException) {
                    Toast.makeText(this@YearlyReportActivity, e.message ?: "Gagal memuat data", Toast.LENGTH_LONG).show()
                }
            } finally { setLoading(false) }
        }
    }

    private fun setLoading(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private suspend fun fetchCollection(colName: String): List<Row> = withContext(Dispatchers.IO) {
        val collected = mutableListOf<Row>()

        // Query timestamp (detik)
        try {
            val q1 = db.collection(colName)
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("createdAt", Timestamp(startMillis / 1000, 0))
                .whereLessThan("createdAt", Timestamp(endMillis / 1000, 0))
                .get(Source.SERVER).await()
            for (doc in q1) collected += mapRow(doc.data ?: emptyMap())
        } catch (_: Throwable) {}

        // Fallback angka millis
        if (collected.isEmpty()) {
            try {
                val q2 = db.collection(colName)
                    .whereEqualTo("status", "active")
                    .whereGreaterThanOrEqualTo("createdAt", startMillis)
                    .whereLessThan("createdAt", endMillis)
                    .get(Source.SERVER).await()
                for (doc in q2) collected += mapRow(doc.data ?: emptyMap())
            } catch (_: Throwable) {}
        }

        // Fallback baca semua (cache/server) lalu filter manual
        if (collected.isEmpty()) {
            try {
                val all = db.collection(colName).get(Source.CACHE).await()
                for (doc in all) {
                    val data = doc.data ?: continue
                    val status = (data["status"] as? String)?.lowercase(Locale.ROOT)
                    if (status != null && status != "active") continue
                    val created = extractMillis(data)
                    if (created != null && created in startMillis until endMillis) {
                        collected += mapRow(data)
                    }
                }
            } catch (_: Throwable) {}
        }
        if (collected.isEmpty()) {
            val all = db.collection(colName).get(Source.SERVER).await()
            for (doc in all) {
                val data = doc.data ?: continue
                val status = (data["status"] as? String)?.lowercase(Locale.ROOT)
                if (status != null && status != "active") continue
                val created = extractMillis(data)
                if (created != null && created in startMillis until endMillis) {
                    collected += mapRow(data)
                }
            }
        }
        collected.distinctBy { "${it.nama}|${it.alamat}|${it.telp}" }
    }

    private fun extractMillis(d: Map<String, Any?>): Long? {
        (d["createdAt"] as? Timestamp)?.let { return it.toDate().time }
        (d["createdAt"] as? Number)?.let { return it.toLong() }
        (d["createdAtMillis"] as? Number)?.let { return it.toLong() }
        (d["createdAt"] as? Map<*, *>)?.let { m ->
            val sec = (m["_seconds"] as? Number)?.toLong()
            if (sec != null) return sec * 1000
        }
        return null
    }

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
        val total = rowsPembelian.size + rowsRenovasi.size + rowsAC.size + rowsCCTV.size
        tvTotalAll.text = "Total Dokumen: $total"
    }

    private fun renderSection(container: LinearLayout, rows: List<Row>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)

        rows.forEach { r ->
            val v = inflater.inflate(R.layout.item_report_row_three, container, false)
            v.findViewById<TextView>(R.id.colNama).text   = r.nama
            v.findViewById<TextView>(R.id.colAlamat).text = r.alamat
            v.findViewById<TextView>(R.id.colTelp).text   = r.telp
            container.addView(v)
        }

        val total = rows.size
        val tv = TextView(this).apply {
            setTextColor(ContextCompat.getColor(this@YearlyReportActivity, android.R.color.darker_gray))
            val dp8 = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, dp8, 0, dp8)
            text = "Total Dokumen: $total"
        }
        container.addView(tv)
    }

    // ====== CETAK PDF ======
    private fun onClickDownload() {
        if (rowsPembelian.isEmpty() && rowsRenovasi.isEmpty() && rowsAC.isEmpty() && rowsCCTV.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk tahun ini", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            val fileName = "Laporan_Tahunan_${selectedYear}_${sdf.format(System.currentTimeMillis())}.pdf"

            val doc = PdfDocument()
            val writer = ReportPdfWriter(
                context = this,
                title   = "Laporan Tahunan",
                subtitle = "Periode: Tahun $selectedYear",
                rowsPembelian = rowsPembelian.map { ReportPdfWriter.Row(it.nama, it.alamat, it.telp) },
                rowsRenovasi  = rowsRenovasi .map { ReportPdfWriter.Row(it.nama, it.alamat, it.telp) },
                rowsAC        = rowsAC       .map { ReportPdfWriter.Row(it.nama, it.alamat, it.telp) },
                rowsCCTV      = rowsCCTV     .map { ReportPdfWriter.Row(it.nama, it.alamat, it.telp) },
            )
            writer.writeTo(doc)

            com.epic.documentmanager.utils.ReportSaver.saveToDownloads(this, fileName, doc)
        } catch (e: Throwable) {
            Toast.makeText(this, e.message ?: "Gagal membuat PDF", Toast.LENGTH_LONG).show()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
