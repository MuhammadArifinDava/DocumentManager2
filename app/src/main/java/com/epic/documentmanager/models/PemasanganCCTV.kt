// com/epic/documentmanager/models/PemasanganCCTV.kt
package com.epic.documentmanager.models

import java.io.Serializable
@androidx.annotation.Keep
data class PemasanganCCTV(
    val id: String = "",
    val uniqueCode: String = "",
    val nama: String = "",
    val alamat: String = "",
    val noTelepon: String = "",
    val jumlahUnit: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val updatedBy: String = "",
    val status: String = "active",
    val attachments: Map<String, String> = emptyMap()
) : Serializable
