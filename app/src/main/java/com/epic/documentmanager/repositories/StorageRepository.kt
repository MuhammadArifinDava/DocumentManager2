package com.epic.documentmanager.repositories

import android.content.Context
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.Locale
import com.epic.documentmanager.utils.Constants

class StorageRepository {

    private val storage = Firebase.storage

    // ====== Helper path & name ======
    private fun destDirFor(documentType: String): String = when (documentType) {
        Constants.DOC_TYPE_PEMBELIAN_RUMAH -> "documents/PEMBELIAN_RUMAH"
        Constants.DOC_TYPE_RENOVASI_RUMAH  -> "documents/RENOVASI_RUMAH"
        Constants.DOC_TYPE_PEMASANGAN_AC   -> "documents/PEMASANGAN_AC"
        Constants.DOC_TYPE_PEMASANGAN_CCTV -> "documents/PEMASANGAN_CCTV"
        else                               -> "documents/OTHERS"
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun guessExtFromUri(uri: Uri): String {
        val seg = uri.lastPathSegment?.lowercase(Locale.ROOT) ?: return "bin"
        val dot = seg.lastIndexOf('.')
        return if (dot != -1 && dot < seg.length - 1) seg.substring(dot + 1) else "bin"
    }

    // ============================================================
    // 1) Dipakai ViewModel kamu: multi-file, TIDAK butuh Context
    // ============================================================
    suspend fun uploadMultipleFiles(
        uris: List<Uri>,
        documentType: String,
        fileNames: List<String>
    ): Result<List<String>> {
        return try {
            val dir = destDirFor(documentType)
            val urls = mutableListOf<String>()

            uris.forEachIndexed { index, uri ->
                val base = sanitize(fileNames.getOrNull(index) ?: "file_${index + 1}")
                val ext  = guessExtFromUri(uri)
                val name = "${base}_${System.currentTimeMillis()}.$ext"

                val ref = storage.reference.child("$dir/$name")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                urls.add(url)
            }

            Result.success(urls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // 2) Single file (kompatibilitas kode lama / service lama)
    // ============================================================
    suspend fun uploadDocument(
        uri: Uri,
        documentType: String,
        fileName: String
    ): Result<String> {
        return try {
            val dir = destDirFor(documentType)
            val base = sanitize(fileName.ifBlank { "file" })
            val ext  = guessExtFromUri(uri)
            val name = "${base}_${System.currentTimeMillis()}.$ext"

            val ref = storage.reference.child("$dir/$name")
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // 3) Versi dengan Context & progress (kupakai di UploadService)
    // ============================================================
    suspend fun uploadFiles(
        context: Context,
        uris: List<Uri>,
        destDir: String,
        filenamePrefix: String,
        onProgress: ((uploaded: Int, total: Int) -> Unit)? = null
    ): List<String> {
        if (uris.isEmpty()) return emptyList()

        val total = uris.size
        val urls = mutableListOf<String>()
        var uploaded = 0

        uris.forEachIndexed { index, uri ->
            val ext  = guessExtFromUri(uri)
            val name = "${sanitize(filenamePrefix)}_${System.currentTimeMillis()}_${index + 1}.$ext"
            val ref  = storage.reference.child("$destDir/$name")

            // putFile bekerja untuk content:// maupun file://, tidak perlu Context
            ref.putFile(uri).await()
            urls.add(ref.downloadUrl.await().toString())

            uploaded++
            onProgress?.invoke(uploaded, total)
        }
        return urls
    }
}
