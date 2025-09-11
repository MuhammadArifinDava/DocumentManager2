// app/src/main/java/com/epic/documentmanager/utils/StoragePermission.kt
package com.epic.documentmanager.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object StoragePermission {
    private const val REQ = 4421

    fun ensureLegacyWritePermission(activity: Activity, onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 29) {
            onGranted(); return
        }
        val granted = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) onGranted()
        else ActivityCompat.requestPermissions(
            activity, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), REQ
        )
    }
}
