package com.epic.documentmanager.utils

import android.content.Context
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage

object OriginalDownloader {

    /**
     * storagePath: path file di Firebase Storage (mis. "documents/abc.jpg")
     * fileName: nama file output (boleh tanpa ekstensi; kami tambahkan dari metadata)
     * mimeType: opsional; kalau null akan diambil dari metadata
     */
    fun download(context: Context, storagePath: String, fileName: String, mimeType: String? = null) {
        val ref = FirebaseStorage.getInstance().reference.child(storagePath)

        // Ambil metadata untuk dapat contentType (agar nama tidak jadi .bin)
        ref.metadata
            .addOnSuccessListener { md ->
                val ct = mimeType ?: md.contentType
                val finalName = Downloads.ensureExt(fileName, ct)

                ref.downloadUrl
                    .addOnSuccessListener { uri ->
                        Downloads.enqueueDownloadOriginal(
                            context = context,
                            url = uri,
                            fileNameRaw = finalName,                       // <- perbaikan nama arg
                            mimeTypeRaw = ct ?: Downloads.mimeFromName(finalName) // <- perbaikan nama arg
                        )
                        Toast.makeText(context, "Mengunduh $finalName…", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            context,
                            it.localizedMessage ?: "Gagal mendapatkan URL unduhan",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    context,
                    it.localizedMessage ?: "Gagal membaca metadata",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
