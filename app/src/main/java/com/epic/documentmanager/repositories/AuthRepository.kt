package com.epic.documentmanager.repositories

import android.os.Build
import androidx.annotation.RequiresApi
import com.epic.documentmanager.models.User
import com.epic.documentmanager.utils.Constants
import com.epic.documentmanager.utils.FirebaseUtils
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.tasks.await

class AuthRepository {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = FirebaseUtils.auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User not found")

            // Get user data from Firestore
            val userDoc = FirebaseUtils.getUsersCollection()
                .document(firebaseUser.uid)
                .get()
                .await()

            if (userDoc.exists()) {
                val user = userDoc.toObject(User::class.java) ?: throw Exception("Invalid user data")
                Result.success(user)
            } else {
                // Create user document if doesn't exist (for compatibility)
                val newUser = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: email,
                    fullName = firebaseUser.displayName ?: "Unknown User",
                    role = Constants.ROLE_STAFF,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isActive = true
                )

                FirebaseUtils.getUsersCollection()
                    .document(firebaseUser.uid)
                    .set(newUser)
                    .await()

                Result.success(newUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, fullName: String, role: String): Result<User> {
        return try {
            val authResult = FirebaseUtils.auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Registration failed")

            // Create user document in Firestore
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                fullName = fullName,
                role = role,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isActive = true
            )

            FirebaseUtils.getUsersCollection()
                .document(firebaseUser.uid)
                .set(user)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCurrentUser(): User? {
        return try {
            val firebaseUser = FirebaseUtils.auth.currentUser ?: return null

            val userDoc = FirebaseUtils.getUsersCollection()
                .document(firebaseUser.uid)
                .get()
                .await()

            if (userDoc.exists()) {
                userDoc.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Boolean> {
        return try {
            val user = FirebaseUtils.auth.currentUser ?: throw Exception("User not logged in")
            val email = user.email ?: throw Exception("Email not found")

            // Re-authenticate user
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()

            // Change password
            user.updatePassword(newPassword).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(user: User): Result<Boolean> {
        return try {
            FirebaseUtils.getUsersCollection()
                .document(user.uid)
                .set(user.copy(updatedAt = System.currentTimeMillis()))
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        FirebaseUtils.auth.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return FirebaseUtils.auth.currentUser != null
    }

    suspend fun resetPassword(email: String): Result<Boolean> {
        return try {
            FirebaseUtils.auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(): Result<Boolean> {
        return try {
            val user = FirebaseUtils.auth.currentUser ?: throw Exception("User not logged in")

            // Delete user document from Firestore
            FirebaseUtils.getUsersCollection()
                .document(user.uid)
                .delete()
                .await()

            // Delete Firebase Auth user
            user.delete().await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

//package com.epic.documentmanager.repositories
//
//import com.google.firebase.auth.AuthResult
//import com.google.firebase.auth.FirebaseUser
//import com.epic.documentmanager.models.User
//import com.epic.documentmanager.utils.Constants
//import com.epic.documentmanager.utils.FirebaseUtils
//import kotlinx.coroutines.tasks.await
//
//class AuthRepository {
//
//    suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
//        return try {
//            val result = FirebaseUtils.auth.signInWithEmailAndPassword(email, password).await()
//            Result.success(result.user!!)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    suspend fun registerUser(email: String, password: String, fullName: String, role: String): Result<FirebaseUser> {
//        return try {
//            val result = FirebaseUtils.auth.createUserWithEmailAndPassword(email, password).await()
//            val user = result.user!!
//
//            // Create user profile in Firestore
//            val userProfile = User(
//                uid = user.uid,
//                email = email,
//                fullName = fullName,
//                role = role,
//                createdAt = System.currentTimeMillis(),
//                updatedAt = System.currentTimeMillis(),
//                isActive = true
//            )
//
//            FirebaseUtils.firestore.collection(Constants.USERS_COLLECTION)
//                .document(user.uid)
//                .set(userProfile)
//                .await()
//
//            Result.success(user)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    suspend fun getCurrentUser(): User? {
//        return try {
//            val currentUser = FirebaseUtils.auth.currentUser
//            if (currentUser != null) {
//                val doc = FirebaseUtils.firestore.collection(Constants.USERS_COLLECTION)
//                    .document(currentUser.uid)
//                    .get()
//                    .await()
//                doc.toObject(User::class.java)
//            } else {
//                null
//            }
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    suspend fun updatePassword(newPassword: String): Result<Unit> {
//        return try {
//            FirebaseUtils.auth.currentUser?.updatePassword(newPassword)?.await()
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    fun logout() {
//        FirebaseUtils.auth.signOut()
//    }
//
//    fun isUserLoggedIn(): Boolean {
//        return FirebaseUtils.auth.currentUser != null
//    }
//}