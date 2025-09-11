package com.epic.documentmanager.utils

object Constants {

    // ===== Roles =====
    const val ROLE_ADMIN   = "admin"
    const val ROLE_MANAGER = "manager"
    const val ROLE_STAFF   = "staff"

    // ===== Firestore Collections =====
    const val USERS_COLLECTION            = "users"
    const val PEMBELIAN_RUMAH_COLLECTION  = "pembelian_rumah"
    const val RENOVASI_RUMAH_COLLECTION   = "renovasi_rumah"
    const val PEMASANGAN_AC_COLLECTION    = "pemasangan_ac"
    const val PEMASANGAN_CCTV_COLLECTION  = "pemasangan_cctv"
    const val REPORTS_COLLECTION          = "reports"

    // ===== Firebase Storage Folders =====
    const val STORAGE_DOCUMENTS      = "documents"
    const val STORAGE_PROFILE_IMAGES = "profile_images"

    // ===== Document Types (dipakai UI/VM) =====
    const val DOC_TYPE_PEMBELIAN_RUMAH = "PEMBELIAN_RUMAH"
    const val DOC_TYPE_RENOVASI_RUMAH  = "RENOVASI_RUMAH"
    const val DOC_TYPE_PEMASANGAN_AC   = "PEMASANGAN_AC"
    const val DOC_TYPE_PEMASANGAN_CCTV = "PEMASANGAN_CCTV"

    // ===== Kode Prefix (dipakai CodeGenerator) =====
    const val PREFIX_PEMBELIAN_RUMAH = "PR"
    const val PREFIX_RENOVASI_RUMAH  = "RR"
    const val PREFIX_PEMASANGAN_AC   = "AC"
    const val PREFIX_PEMASANGAN_CCTV = "CC"

    // ===== Validation / Upload =====
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_FILE_SIZE_MB    = 10           // 10 MB
    val ALLOWED_IMAGE_EXTENSIONS     = listOf("jpg","jpeg","png","gif","webp","bmp")
    val ALLOWED_DOCUMENT_EXTENSIONS  = listOf("pdf","doc","docx","txt","rtf")
}
