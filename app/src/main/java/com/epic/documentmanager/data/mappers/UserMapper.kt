package com.epic.documentmanager.data.mappers

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "staff",
    val isActive: Boolean = true
) {
    val fullName: String get() = name   // ⬅️ alias untuk kompatibilitas
}


object UserMapper {
    fun from(doc: DocumentSnapshot): User {
        val d = doc.data ?: emptyMap<String, Any?>()
        val uid = (d["uid"] ?: doc.id).toString()
        val email = (d["email"] ?: "").toString()
        val name = (d["name"] ?: d["nama"] ?: "Unknown").toString()
        val role = (d["role"] ?: "staff").toString()
        val isActive = when (val v = d["isActive"]) {
            is Boolean -> v
            is String -> v.equals("true", true)
            is Number -> v.toInt() != 0
            else -> true
        }
        // createdAt/updatedAt sengaja diabaikan agar tidak memicu deserialize error
        @Suppress("UNUSED_VARIABLE")
        val createdAt = d["createdAt"] as? Timestamp
        return User(uid, email, name, role, isActive)
    }
}
