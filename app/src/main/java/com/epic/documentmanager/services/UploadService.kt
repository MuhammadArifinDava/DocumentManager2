package com.epic.documentmanager.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.epic.documentmanager.R
import com.epic.documentmanager.repositories.StorageRepository
import com.epic.documentmanager.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service untuk upload banyak file (format apapun) dengan progress.
 * Hasil upload dikirim via broadcast:
 *  - ACTION_UPLOAD_COMPLETE (extra: ArrayList<String> "urls")
 *  - ACTION_UPLOAD_ERROR (extra: String "message")
 *
 * Extras yang diterima:
 *  - "uris" (ArrayList<Uri>) : daftar file yang diupload
 *  - "documentType" (String) : tipe dokumen, gunakan Constants.* (PR/RR/AC/CC)
 *  - "fileNames" (ArrayList<String>) : opsional, tidak wajib dipakai
 *  - "prefix" (String) : opsional, kalau ada dipakai sebagai prefix nama file (mis. kode unik)
 */
class UploadService : Service() {

    private val storageRepository = StorageRepository()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uris: ArrayList<Uri> =
            intent?.getParcelableArrayListExtra("uris") ?: arrayListOf()
        val documentType: String = intent?.getStringExtra("documentType") ?: ""
        // Tidak wajib dipakai, disediakan agar kompatibel dengan pemanggil lama
        val fileNames: ArrayList<String> =
            intent?.getStringArrayListExtra("fileNames") ?: arrayListOf()
        val prefix: String =
            intent?.getStringExtra("prefix") ?: (documentType.ifEmpty { "DOC" })

        startForeground(NOTIFICATION_ID, buildNotification("Mengupload dokumen...", 0))

        serviceScope.launch {
            try {
                val destDir = when (documentType) {
                    Constants.DOC_TYPE_PEMBELIAN_RUMAH -> "documents/PEMBELIAN_RUMAH"
                    Constants.DOC_TYPE_RENOVASI_RUMAH -> "documents/RENOVASI_RUMAH"
                    Constants.DOC_TYPE_PEMASANGAN_AC -> "documents/PEMASANGAN_AC"
                    Constants.DOC_TYPE_PEMASANGAN_CCTV -> "documents/PEMASANGAN_CCTV"
                    else -> "documents/OTHERS"
                }

                val urls = storageRepository.uploadFiles(
                    context = applicationContext,
                    uris = uris,
                    destDir = destDir,
                    filenamePrefix = prefix
                ) { uploaded, total ->
                    val progress = if (total > 0) (uploaded * 100) / total else 0
                    updateNotification("Mengupload ($uploaded/$total)...", progress)
                }

                // Broadcast sukses
                sendBroadcast(Intent(ACTION_UPLOAD_COMPLETE).apply {
                    putStringArrayListExtra("urls", ArrayList(urls))
                })

                stopForeground(true)
                stopSelf(startId)
            } catch (e: Exception) {
                // Broadcast error
                sendBroadcast(Intent(ACTION_UPLOAD_ERROR).apply {
                    putExtra("message", e.message ?: "Gagal mengunggah berkas.")
                })
                stopForeground(true)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    // ================= Notification helpers =================

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Upload Dokumen",
                NotificationManager.IMPORTANCE_LOW
            )
            mgr.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        val icon = try {
            // kalau tidak ada ic_upload, pakai default sistem
            R.drawable.ic_upload
        } catch (_: Exception) {
            android.R.drawable.stat_sys_upload
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mengupload dokumen")
            .setContentText(text)
            .setSmallIcon(icon)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        if (progress in 1..99) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, false)
        }
        return builder.build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr?.notify(NOTIFICATION_ID, buildNotification(text, progress))
    }

    companion object {
        const val ACTION_UPLOAD_COMPLETE = "com.epic.documentmanager.ACTION_UPLOAD_COMPLETE"
        const val ACTION_UPLOAD_ERROR = "com.epic.documentmanager.ACTION_UPLOAD_ERROR"
        private const val CHANNEL_ID = "upload_channel"
        private const val NOTIFICATION_ID = 1011
    }
}
