package com.epic.documentmanager.data.repository

import com.epic.documentmanager.data.mappers.User
import com.epic.documentmanager.data.mappers.UserMapper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StaffRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val col get() = db.collection("users")

    suspend fun fetchUsers(): List<User> {
        val snap = col.get().await()
        return snap.documents
            .map { UserMapper.from(it) }
            .filter { it.isActive }                       // ⬅️ hanya aktif yang ditampilkan
            .sortedBy { it.name.lowercase() }
    }

    suspend fun searchUsers(q: String): List<User> {
        val key = q.trim().lowercase()
        val all = fetchUsers()
        if (key.isBlank()) return all
        return all.filter { it.name.lowercase().contains(key) || it.email.lowercase().contains(key) }
    }

    /** "Hapus" = nonaktifkan & beri timestamp. */
    suspend fun deleteUserProfile(uid: String) {
        col.document(uid).update(
            mapOf(
                "isActive" to false,
                "deletedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Optional; aman jika tidak ada koleksi invites. */
    suspend fun claimInviteIfExists(uid: String, email: String) { /* no-op */ }
}
