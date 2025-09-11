package com.epic.documentmanager.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

object FirebaseUtils {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // ---------- Auth helpers ----------
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun getCurrentUserEmail(): String? = auth.currentUser?.email
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // ---------- Firestore helpers ----------
    fun getUsersCollection(): CollectionReference =
        firestore.collection(Constants.USERS_COLLECTION)

    fun getPembelianRumahCollection(): CollectionReference =
        firestore.collection(Constants.PEMBELIAN_RUMAH_COLLECTION)

    fun getRenovasiRumahCollection(): CollectionReference =
        firestore.collection(Constants.RENOVASI_RUMAH_COLLECTION)

    fun getPemasanganACCollection(): CollectionReference =
        firestore.collection(Constants.PEMASANGAN_AC_COLLECTION)

    fun getPemasanganCCTVCollection(): CollectionReference =
        firestore.collection(Constants.PEMASANGAN_CCTV_COLLECTION)

    fun getReportsCollection(): CollectionReference =
        firestore.collection("reports")

    fun getDocumentCollection(type: String): CollectionReference = when (type) {
        Constants.DOC_TYPE_PEMBELIAN_RUMAH -> getPembelianRumahCollection()
        Constants.DOC_TYPE_RENOVASI_RUMAH -> getRenovasiRumahCollection()
        Constants.DOC_TYPE_PEMASANGAN_AC -> getPemasanganACCollection()
        Constants.DOC_TYPE_PEMASANGAN_CCTV -> getPemasanganCCTVCollection()
        else -> firestore.collection("documents")
    }

    // ---------- Storage helpers ----------
    fun getDocumentsStorageRef(): StorageReference =
        storage.reference.child(Constants.STORAGE_DOCUMENTS)

    fun getProfileImagesStorageRef(): StorageReference =
        storage.reference.child(Constants.STORAGE_PROFILE_IMAGES)

    fun getDocumentStorageRef(documentType: String): StorageReference =
        getDocumentsStorageRef().child(documentType)

    fun getFileStorageRef(documentType: String, fileName: String): StorageReference =
        getDocumentStorageRef(documentType).child(fileName)

    // ---------- Utils ----------
    fun generateDocumentId(): String = firestore.collection("temp").document().id
    fun getTimestamp(): Long = System.currentTimeMillis()

    /** Build map tanpa nilai null, aman tipe Map<String, Any>. */
    fun createMap(vararg pairs: Pair<String, Any?>): Map<String, Any> =
        buildMap {
            for ((k, v) in pairs) if (v != null) put(k, v)
        }

    fun generateFileName(originalName: String, documentType: String): String {
        val timestamp = System.currentTimeMillis()
        val extension = originalName.substringAfterLast(".", "")
        return buildString {
            append(documentType).append('_').append(timestamp)
            if (extension.isNotEmpty()) append('.').append(extension)
        }
    }

    fun isValidImageFile(fileName: String): Boolean {
        val valid = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
        val ext = fileName.substringAfterLast(".", "").lowercase(Locale.getDefault())
        return ext in valid
    }

    fun isValidDocumentFile(fileName: String): Boolean {
        val valid = setOf("pdf", "doc", "docx", "txt", "rtf")
        val ext = fileName.substringAfterLast(".", "").lowercase(Locale.getDefault())
        return ext in valid
    }

    // ---------- Search helpers ----------
    fun createSearchQuery(field: String, query: String): String =
        query.lowercase(Locale.getDefault())

    /**
     * Catatan: supaya query ini efektif, simpan juga field yang dicari
     * dalam bentuk lowercase di Firestore (mis. field `namaLower`).
     */
    fun buildSearchQuery(
        collection: CollectionReference,
        field: String,
        query: String
    ): Query {
        val q = query.lowercase(Locale.getDefault())
        return collection
            .whereGreaterThanOrEqualTo(field, q)
            .whereLessThanOrEqualTo(field, q + '\uf8ff')
    }

    // ---------- Date helpers ----------
    fun getStartOfDay(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun getEndOfDay(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

    fun getStartOfMonth(year: Int, month: Int): Long =
        Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun getEndOfMonth(year: Int, month: Int): Long =
        Calendar.getInstance().apply {
            set(year, month - 1, 1, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }.timeInMillis

    // ---------- Batch ----------
    fun createBatch(): WriteBatch = firestore.batch()
    fun executeBatch(batch: WriteBatch) = batch.commit()

    // ---------- Error handling ----------
    fun handleFirebaseError(exception: Exception): String = when (exception) {
        is com.google.firebase.auth.FirebaseAuthException -> when (exception.errorCode) {
            "ERROR_USER_NOT_FOUND"      -> "User tidak ditemukan"
            "ERROR_WRONG_PASSWORD"      -> "Password salah"
            "ERROR_EMAIL_ALREADY_IN_USE"-> "Email sudah digunakan"
            "ERROR_WEAK_PASSWORD"       -> "Password terlalu lemah"
            "ERROR_INVALID_EMAIL"       -> "Format email tidak valid"
            else                        -> exception.message ?: "Terjadi kesalahan autentikasi"
        }
        is com.google.firebase.firestore.FirebaseFirestoreException -> when (exception.code) {
            com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Anda tidak memiliki izin untuk melakukan operasi ini"
            com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Layanan tidak tersedia, coba lagi nanti"
            com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND ->
                "Data tidak ditemukan"
            else -> exception.message ?: "Terjadi kesalahan database"
        }
        else -> exception.message ?: "Terjadi kesalahan tidak dikenal"
    }

    // ---------- Simple document validation ----------
    fun validateDocumentData(data: Map<String, Any>): List<String> {
        val errors = mutableListOf<String>()
        if (data["nama"]?.toString().isNullOrBlank()) errors.add("Nama tidak boleh kosong")
        if (data["noTelepon"]?.toString().isNullOrBlank()) errors.add("Nomor telepon tidak boleh kosong")
        return errors
    }
}
