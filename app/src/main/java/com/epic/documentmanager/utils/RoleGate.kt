package com.epic.documentmanager.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object RoleGate {
    suspend fun isAdmin(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
        val role = doc.getString("role") ?: "staff"
        return role == "admin"
    }

    suspend fun requireAdminOrThrow() {
        if (!isAdmin()) error("Akses ditolak. Hanya Admin yang dapat membuka halaman ini.")
    }
}
