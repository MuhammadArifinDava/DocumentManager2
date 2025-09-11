package com.epic.documentmanager.models
@androidx.annotation.Keep
data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val profileImageUrl: String = ""
) {
    constructor() : this("", "", "", "", 0L, 0L, true, "")
}