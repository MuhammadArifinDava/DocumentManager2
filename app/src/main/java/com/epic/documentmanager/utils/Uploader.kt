// app/src/main/java/com/epic/documentmanager/utils/Uploader.kt
package com.epic.documentmanager.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.Normalizer
import java.util.Locale

/**
 * Util upload lampiran yang robust:
 * - Persistable URI permission (jika mungkin)
 * - Nama file unik & stabil
 * - Upload sequential + retry ringan
 *
 * Cara pakai (di Fragment/Activity penyimpan dokumen):
 *
 *   val resultMap = Uploader.uploadAll(
 *       context = requireContext(),
 *       folder  = "${docType}/${uniqueCode}",   // path di Storage
 *       uris    = selectedUris
 *   ) { index, total, name ->
 *       // optional: update progress UI -> "$index / $total : $name"
 *   }
 *
 *   // resultMap: Map<displayName, downloadUrl>
 */
object Uploader {

    // ===== Public API ========================================================

    /**
     * Upload semua URI secara berurutan. Mengembalikan Map<displayNameUnik, downloadUrl>.
     */
    suspend fun uploadAll(
        context: Context,
        folder: String,
        uris: List<Uri>,
        onProgress: ((index: Int, total: Int, name: String) -> Unit)? = null
    ): Map<String, String> {
        if (uris.isEmpty()) return emptyMap()

        // 1) Ambil permission agar URI tetap bisa diakses setelah Activity tutup
        takePersistableIfPossible(context, uris)

        // 2) Ambil nama file dari ContentResolver (fallback ke lastPathSegment),
        //    kemudian buat unik (… (2), … (3), dst)
        val cr = context.contentResolver
        val rawPairs = uris.map { uri ->
            val name = queryDisplayName(cr, uri) ?: (uri.lastPathSegment ?: "file")
            sanitizeFilename(name) to uri
        }
        val uniqueList = makeDisplayNameUnique(rawPairs)

        // 3) Upload sequential + retry kecil
        val storage = FirebaseStorage.getInstance()
        val result = LinkedHashMap<String, String>()
        val total = uniqueList.size

        for ((index, pair) in uniqueList.withIndex()) {
            val (displayName, uri) = pair
            onProgress?.invoke(index + 1, total, displayName)

            val url = uploadSingleWithRetry(
                cr = cr,
                storage = storage,
                folder = folder,
                name = displayName,
                uri = uri
            )

            if (url != null) result[displayName] = url
            // jika null, kita skip saja (biar proses lain tetap lanjut)
        }

        return result
    }

    // ===== Internal helpers ==================================================

    private suspend fun uploadSingleWithRetry(
        cr: ContentResolver,
        storage: FirebaseStorage,
        folder: String,
        name: String,
        uri: Uri,
        maxRetry: Int = 2
    ): String? {
        var attempt = 0
        var lastErr: Throwable? = null

        while (attempt <= maxRetry) {
            try {
                // Buka stream dari content://
                cr.openInputStream(uri).use { input ->
                    if (input == null) throw IllegalStateException("Tidak bisa membuka stream untuk $uri")
                    val ref = storage.reference.child("$folder/${name}")
                    // Upload…
                    val task = ref.putStream(input).await()
                    // Ambil download URL
                    return task.storage.downloadUrl.await().toString()
                }
            } catch (t: Throwable) {
                lastErr = t
                attempt++
                if (attempt <= maxRetry) delay(500L * attempt) // backoff ringan
            }
        }
        // log seperlunya; kembalikan null agar caller bisa lanjut
        return null
    }

    /**
     * Ambil DISPLAY_NAME dari ContentResolver (untuk Document/Media Provider).
     */
    private fun queryDisplayName(cr: ContentResolver, uri: Uri): String? {
        return try {
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Bersihkan nama file (hapus karakter aneh, normalisasi spasi), tetap pertahankan ekstensi.
     */
    private fun sanitizeFilename(original: String): String {
        val trimmed = original.trim().ifBlank { "file" }
        val dot = trimmed.lastIndexOf('.')
        val namePart = if (dot > 0) trimmed.substring(0, dot) else trimmed
        val extPart = if (dot > 0) trimmed.substring(dot) else ""

        val cleaned = Normalizer.normalize(namePart, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("[^a-zA-Z0-9 _-]".toRegex(), "_")
            .replace("\\s+".toRegex(), " ")
            .trim()
            .ifBlank { "file" }

        return "$cleaned$extPart"
    }

    /**
     * Ambil persistable permission untuk setiap URI jika memungkinkan.
     * Ini penting untuk URI hasil ACTION_OPEN_DOCUMENT.
     */
    private fun takePersistableIfPossible(context: Context, uris: List<Uri>) {
        val cr = context.contentResolver
        uris.forEach { uri ->
            try {
                // Kita coba claim dengan READ | PERSISTABLE.
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                cr.takePersistableUriPermission(uri, flags)
            } catch (_: SecurityException) {
                // Bukan dari SAF (ACTION_GET_CONTENT) atau provider tidak mendukung → aman diabaikan
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Beri suffix (2), (3)… bila ada nama sama, agar semua unik & urut.
     */
    private fun makeDisplayNameUnique(
        list: List<Pair<String, Uri>>
    ): List<Pair<String, Uri>> {
        val seen = HashMap<String, Int>()
        return list.map { (name, uri) ->
            val count = (seen[name] ?: 0) + 1
            seen[name] = count
            if (count == 1) name to uri else "$name ($count)" to uri
        }
    }
}
