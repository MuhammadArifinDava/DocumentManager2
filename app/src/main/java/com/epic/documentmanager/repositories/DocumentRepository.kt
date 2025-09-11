package com.epic.documentmanager.repositories

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.epic.documentmanager.models.*
import com.epic.documentmanager.utils.Constants
import com.epic.documentmanager.utils.DateUtils
import com.epic.documentmanager.utils.FirebaseUtils
import kotlinx.coroutines.tasks.await
import java.util.Locale

class DocumentRepository {

    // ============================================================
    // =============== SAVE / UPSERT (Create or Update) ===========
    // ============================================================

    suspend fun savePembelianRumah(data: PembelianRumah): Result<String> {
        return try {
            val col = FirebaseUtils.firestore.collection(Constants.PEMBELIAN_RUMAH_COLLECTION)
            val isCreate = data.id.isEmpty()
            val docRef = if (isCreate) col.document() else col.document(data.id)

            val now = System.currentTimeMillis()
            val dataToSave = if (isCreate) {
                data.copy(
                    id = docRef.id,
                    createdAt = if (data.createdAt == 0L) now else data.createdAt,
                    createdBy = data.createdBy.ifEmpty { FirebaseUtils.getCurrentUserId() ?: "" },
                    updatedAt = now,
                    updatedBy = FirebaseUtils.getCurrentUserId() ?: "",
                    status = data.status.ifEmpty { "active" }
                )
            } else {
                data.copy(
                    id = data.id,
                    updatedAt = now,
                    updatedBy = FirebaseUtils.getCurrentUserId() ?: ""
                )
            }

            docRef.set(dataToSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveRenovasiRumah(data: RenovasiRumah): Result<String> {
        return try {
            val col = FirebaseUtils.firestore.collection(Constants.RENOVASI_RUMAH_COLLECTION)
            val isCreate = data.id.isEmpty()
            val docRef = if (isCreate) col.document() else col.document(data.id)

            val now = System.currentTimeMillis()
            val dataToSave = if (isCreate) {
                data.copy(
                    id = docRef.id,
                    createdAt = if (data.createdAt == 0L) now else data.createdAt,
                    createdBy = data.createdBy.ifEmpty { FirebaseUtils.getCurrentUserId() ?: "" },
                    updatedAt = now,
                    updatedBy = FirebaseUtils.getCurrentUserId() ?: "",
                    status = data.status.ifEmpty { "active" }
                )
            } else {
                data.copy(
                    id = data.id,
                    updatedAt = now,
                    updatedBy = FirebaseUtils.getCurrentUserId() ?: ""
                )
            }

            docRef.set(dataToSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePemasanganAC(data: PemasanganAC): Result<String> {
        return try {
            val col = FirebaseUtils.firestore.collection(Constants.PEMASANGAN_AC_COLLECTION)
            val isCreate = data.id.isEmpty()
            val docRef = if (isCreate) col.document() else col.document(data.id)

            val now = System.currentTimeMillis()
            val dataToSave = if (isCreate) {
                data.copy(
                    id = docRef.id,
                    createdAt = if (data.createdAt == 0L) now else data.createdAt,
                    createdBy = data.createdBy.ifEmpty { FirebaseUtils.getCurrentUserId() ?: "" },
                    updatedAt = now,
                    updatedBy = FirebaseUtils.getCurrentUserId() ?: "",
                    status = data.status.ifEmpty { "active" }
                )
            } else {
                data.copy(
                    id = data.id,
                    updatedAt = now,
                    updatedBy = FirebaseUtils.getCurrentUserId() ?: ""
                )
            }

            docRef.set(dataToSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePemasanganCCTV(data: PemasanganCCTV): Result<String> {
        return try {
            val col = FirebaseUtils.firestore.collection(Constants.PEMASANGAN_CCTV_COLLECTION)
            val isCreate = data.id.isEmpty()
            val docRef = if (isCreate) col.document() else col.document(data.id)

            val now = System.currentTimeMillis()
            val dataToSave = if (isCreate) {
                data.copy(
                    id = docRef.id,
                    createdAt = if (data.createdAt == 0L) now else data.createdAt,
                    createdBy = data.createdBy.ifEmpty { FirebaseUtils.getCurrentUserId() ?: "" },
                    updatedAt = now,
                    updatedBy = FirebaseUtils.getCurrentUserId() ?: "",
                    status = data.status.ifEmpty { "active" }
                )
            } else {
                data.copy(
                    id = data.id,
                    updatedAt = now,
                    updatedBy = FirebaseUtils.getCurrentUserId() ?: ""
                )
            }

            docRef.set(dataToSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // =============== GET ALL (fallback + source) ================
    // ============================================================

    private suspend inline fun <reified T> getAllWithFallback(
        collection: String,
        orderField: String = "createdAt",
        source: Source = Source.DEFAULT
    ): List<T> {
        val ref = FirebaseUtils.firestore.collection(collection)

        // Primary: status == active + orderBy
        try {
            val snap = ref.whereEqualTo("status", "active")
                .orderBy(orderField, Query.Direction.DESCENDING)
                .get(source).await()
            if (!snap.isEmpty) return snap.documents.mapNotNull { it.toObject(T::class.java) }
        } catch (_: Exception) { }

        // Fallback #1: status == active (tanpa orderBy)
        try {
            val snap = ref.whereEqualTo("status", "active").get(source).await()
            if (!snap.isEmpty) return snap.documents.mapNotNull { it.toObject(T::class.java) }
        } catch (_: Exception) { }

        // Fallback #2: orderBy createdAt saja
        try {
            val snap = ref.orderBy(orderField, Query.Direction.DESCENDING).get(source).await()
            if (!snap.isEmpty) return snap.documents.mapNotNull { it.toObject(T::class.java) }
        } catch (_: Exception) { }

        // Fallback #3: ambil semua dokumen lalu filter active (biar konsisten)
        return try {
            val snap = ref.get(source).await()
            snap.documents
                .mapNotNull { it.toObject(T::class.java) }
                .filter { any ->
                    try {
                        val f = any!!::class.java.getDeclaredField("status").apply { isAccessible = true }
                        (f.get(any) as? String)?.equals("active", true) == true
                    } catch (_: Throwable) { true } // kalau model tak punya status, biarin lewat
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getAllPembelianRumah(source: Source = Source.DEFAULT): List<PembelianRumah> =
        getAllWithFallback(Constants.PEMBELIAN_RUMAH_COLLECTION, source = source)

    suspend fun getAllRenovasiRumah(source: Source = Source.DEFAULT): List<RenovasiRumah> =
        getAllWithFallback(Constants.RENOVASI_RUMAH_COLLECTION, source = source)

    suspend fun getAllPemasanganAC(source: Source = Source.DEFAULT): List<PemasanganAC> =
        getAllWithFallback(Constants.PEMASANGAN_AC_COLLECTION, source = source)

    suspend fun getAllPemasanganCCTV(source: Source = Source.DEFAULT): List<PemasanganCCTV> =
        getAllWithFallback(Constants.PEMASANGAN_CCTV_COLLECTION, source = source)

    // ============================================================
    // ===================== SOFT DELETE ==========================
    // ============================================================
    suspend fun deletePembelianRumah(id: String): Result<Unit> {
        return try {
            FirebaseUtils.firestore.collection(Constants.PEMBELIAN_RUMAH_COLLECTION)
                .document(id)
                .update("status", "deleted", "updatedAt", System.currentTimeMillis()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRenovasiRumah(id: String): Result<Unit> {
        return try {
            FirebaseUtils.firestore.collection(Constants.RENOVASI_RUMAH_COLLECTION)
                .document(id)
                .update("status", "deleted", "updatedAt", System.currentTimeMillis()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePemasanganAC(id: String): Result<Unit> {
        return try {
            FirebaseUtils.firestore.collection(Constants.PEMASANGAN_AC_COLLECTION)
                .document(id)
                .update("status", "deleted", "updatedAt", System.currentTimeMillis()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePemasanganCCTV(id: String): Result<Unit> {
        return try {
            FirebaseUtils.firestore.collection(Constants.PEMASANGAN_CCTV_COLLECTION)
                .document(id)
                .update("status", "deleted", "updatedAt", System.currentTimeMillis()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // ======================= SEARCH =============================
    // ============================================================
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun searchDocuments(query: String): SearchResult {
        val pembelianRumah = mutableListOf<PembelianRumah>()
        val renovasiRumah = mutableListOf<RenovasiRumah>()
        val pemasanganAC = mutableListOf<PemasanganAC>()
        val pemasanganCCTV = mutableListOf<PemasanganCCTV>()

        try {
            val prQuery = FirebaseUtils.firestore.collection(Constants.PEMBELIAN_RUMAH_COLLECTION)
                .whereEqualTo("status", "active").get().await()
            prQuery.documents.forEach { doc ->
                val data = doc.toObject(PembelianRumah::class.java)
                if (data != null && (data.nama.contains(query, true) || data.uniqueCode.contains(query, true))) {
                    pembelianRumah.add(data)
                }
            }

            val rrQuery = FirebaseUtils.firestore.collection(Constants.RENOVASI_RUMAH_COLLECTION)
                .whereEqualTo("status", "active").get().await()
            rrQuery.documents.forEach { doc ->
                val data = doc.toObject(RenovasiRumah::class.java)
                if (data != null && (data.nama.contains(query, true) || data.uniqueCode.contains(query, true))) {
                    renovasiRumah.add(data)
                }
            }

            val acQuery = FirebaseUtils.firestore.collection(Constants.PEMASANGAN_AC_COLLECTION)
                .whereEqualTo("status", "active").get().await()
            acQuery.documents.forEach { doc ->
                val data = doc.toObject(PemasanganAC::class.java)
                if (data != null && (data.nama.contains(query, true) || data.uniqueCode.contains(query, true))) {
                    pemasanganAC.add(data)
                }
            }

            val cctvQuery = FirebaseUtils.firestore.collection(Constants.PEMASANGAN_CCTV_COLLECTION)
                .whereEqualTo("status", "active").get().await()
            cctvQuery.documents.forEach { doc ->
                val data = doc.toObject(PemasanganCCTV::class.java)
                if (data != null && (data.nama.contains(query, true) || data.uniqueCode.contains(query, true))) {
                    pemasanganCCTV.add(data)
                }
            }
        } catch (_: Exception) { }

        return SearchResult(
            pembelianRumah = pembelianRumah,
            renovasiRumah = renovasiRumah,
            pemasanganAC = pemasanganAC,
            pemasanganCCTV = pemasanganCCTV
        )
    }

    // ============================================================
    // ==================== MONTHLY REPORT ========================
    // ============================================================
    suspend fun generateMonthlyReport(year: Int, month: Int): MonthlyReport {
        val startTime = DateUtils.getStartOfMonth(year, month)
        val endTime = DateUtils.getEndOfMonth(year, month)

        var pembelianCount = 0
        var renovasiCount = 0
        var acCount = 0
        var cctvCount = 0

        try {
            pembelianCount = FirebaseUtils.firestore.collection(Constants.PEMBELIAN_RUMAH_COLLECTION)
                .whereGreaterThanOrEqualTo("createdAt", startTime)
                .whereLessThanOrEqualTo("createdAt", endTime)
                .whereEqualTo("status", "active")
                .get().await().size()

            renovasiCount = FirebaseUtils.firestore.collection(Constants.RENOVASI_RUMAH_COLLECTION)
                .whereGreaterThanOrEqualTo("createdAt", startTime)
                .whereLessThanOrEqualTo("createdAt", endTime)
                .whereEqualTo("status", "active")
                .get().await().size()

            acCount = FirebaseUtils.firestore.collection(Constants.PEMASANGAN_AC_COLLECTION)
                .whereGreaterThanOrEqualTo("createdAt", startTime)
                .whereLessThanOrEqualTo("createdAt", endTime)
                .whereEqualTo("status", "active")
                .get().await().size()

            cctvCount = FirebaseUtils.firestore.collection(Constants.PEMASANGAN_CCTV_COLLECTION)
                .whereGreaterThanOrEqualTo("createdAt", startTime)
                .whereLessThanOrEqualTo("createdAt", endTime)
                .whereEqualTo("status", "active")
                .get().await().size()
        } catch (_: Exception) { }

        val totalCount = pembelianCount + renovasiCount + acCount + cctvCount
        return MonthlyReport(
            month = month.toString().padStart(2, '0'),
            year = year.toString(),
            totalDocuments = totalCount,
            pembelianRumahCount = pembelianCount,
            renovasiRumahCount = renovasiCount,
            pemasanganACCount = acCount,
            pemasanganCCTVCount = cctvCount,
            generatedAt = System.currentTimeMillis(),
            generatedBy = FirebaseUtils.getCurrentUserId() ?: ""
        )
    }
}

// Hasil pencarian agregat
data class SearchResult(
    val pembelianRumah: List<PembelianRumah>,
    val renovasiRumah: List<RenovasiRumah>,
    val pemasanganAC: List<PemasanganAC>,
    val pemasanganCCTV: List<PemasanganCCTV>
)
