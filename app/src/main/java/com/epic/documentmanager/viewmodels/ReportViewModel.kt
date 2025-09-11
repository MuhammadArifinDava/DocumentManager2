package com.epic.documentmanager.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epic.documentmanager.models.MonthlyReport
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ReportViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _reportList = MutableLiveData<List<MonthlyReport>>(emptyList())
    val reportList: LiveData<List<MonthlyReport>> = _reportList

    private val _currentReport = MutableLiveData<MonthlyReport?>()
    val currentReport: LiveData<MonthlyReport?> = _currentReport

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _generateResult = MutableLiveData<Result<Unit>>()
    val generateResult: LiveData<Result<Unit>> = _generateResult

    fun loadAllReports() {
        _loading.value = true
        viewModelScope.launch {
            try {
                // Ambil dari koleksi "monthly_reports" (ganti nama koleksi jika perlu)
                val snap = db.collection("monthly_reports")
                    .orderBy("year", Query.Direction.DESCENDING)
                    .orderBy("month", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val list = snap.documents.mapNotNull { it.toObject(MonthlyReport::class.java) }
                _reportList.value = list
            } catch (_: Exception) {
                // bisa tambahkan LiveData error kalau diperlukan
            } finally {
                _loading.value = false
            }
        }
    }

    fun generateMonthlyReport(month: String, year: String) {
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // TODO: hitung data sesungguhnya dari koleksi dokumen kamu.
                // Untuk kompilasi aman, kita buat objek report dengan nilai default.
                val report = MonthlyReport(
                    month = month,
                    year = year,
                    totalDocuments = 0,
                    pembelianRumahCount = 0,
                    renovasiRumahCount = 0,
                    pemasanganACCount = 0,
                    pemasanganCCTVCount = 0,
                    generatedAt = System.currentTimeMillis()
                )

                // Simpan/replace ke Firestore (opsional tapi bagus agar list ikut terupdate)
                db.collection("monthly_reports")
                    .document("${year}-${month}")
                    .set(report)
                    .await()

                withContext(Dispatchers.Main) {
                    _currentReport.value = report
                    _generateResult.value = Result.success(Unit)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _generateResult.value = Result.failure(e)
                }
            } finally {
                withContext(Dispatchers.Main) { _loading.value = false }
            }
        }
    }
}
