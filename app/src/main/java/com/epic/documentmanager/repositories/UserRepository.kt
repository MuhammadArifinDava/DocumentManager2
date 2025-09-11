package com.epic.documentmanager.repositories

import com.epic.documentmanager.models.User
import com.epic.documentmanager.utils.Constants
import com.epic.documentmanager.utils.FirebaseUtils
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val usersCol = FirebaseUtils.firestore.collection(Constants.USERS_COLLECTION)

    suspend fun getUserById(userId: String): User? = try {
        val doc = usersCol.document(userId).get().await()
        doc.toObject(User::class.java)
    } catch (_: Exception) {
        null
    }

    suspend fun updateUser(user: User): Result<Unit> = try {
        usersCol
            .document(user.uid)
            .set(user.copy(updatedAt = System.currentTimeMillis()))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Ambil semua user staff/manager yang aktif.
     * Catatan: Firestore `whereIn` maksimal 10 nilai -> di sini cuma 2 nilai.
     */
    suspend fun getAllStaff(): List<User> = try {
        val roles = listOf(Constants.ROLE_STAFF, Constants.ROLE_MANAGER)
        val qs = usersCol
            .whereIn("role", roles)
            .whereEqualTo("isActive", true)
            .get()
            .await()

        qs.documents.mapNotNull { it.toObject(User::class.java) }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun deactivateUser(userId: String): Result<Unit> = try {
        usersCol
            .document(userId)
            .update(
                mapOf(
                    "isActive" to false,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUserRole(userId: String): String? = try {
        val doc = usersCol.document(userId).get().await()
        doc.getString("role")
    } catch (_: Exception) {
        null
    }
}
