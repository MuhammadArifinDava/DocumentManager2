package com.epic.documentmanager.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epic.documentmanager.data.mappers.User
import com.epic.documentmanager.utils.FirebaseUtils.firestore
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    // ==== State umum ====
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _currentUser = MutableLiveData<User?>(null)   // kompatibel dgn Activity lama
    val currentUser: LiveData<User?> = _currentUser

    // ==== Hasil aksi (kompatibel dgn Activity lama) ====
    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _registerResult = MutableLiveData<Result<User>>()
    val registerResult: LiveData<Result<User>> = _registerResult

    private val _changePasswordResult = MutableLiveData<Result<Unit>>()
    val changePasswordResult: LiveData<Result<Unit>> = _changePasswordResult

    init {
        // sinkronkan user saat app dibuka
        viewModelScope.launch { refreshCurrentUser() }
    }

    // ===== Helpers =====
    private fun mapUser(uid: String, data: Map<String, Any?>?, fallbackEmail: String?): User {
        val d = data ?: emptyMap()
        val email = (d["email"] ?: fallbackEmail ?: "").toString()
        val name = (d["name"] ?: d["nama"] ?: email.substringBefore("@", "Unknown User")).toString()
        val role = (d["role"] ?: "staff").toString()
        val isActive = when (val v = d["isActive"]) {
            is Boolean -> v
            is String -> v.equals("true", true)
            is Number -> v.toInt() != 0
            else -> true
        }
        return User(uid = uid, email = email, name = name, role = role, isActive = isActive)
    }

    private suspend fun loadProfile(uid: String, email: String?): User? {
        val snap = db.collection("users").document(uid).get().await()
        if (!snap.exists()) return null
        return mapUser(uid, snap.data, email)
    }

    private suspend fun ensureMinimumProfile(uid: String, email: String) {
        db.collection("users").document(uid).set(
            mapOf(
                "uid" to uid,
                "email" to email,
                "name" to email.substringBefore("@"),
                "role" to "staff",
                "isActive" to true
            )
        ).await()
    }

    // ===== API dipakai Activity lama =====
    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    fun getCurrentUser(): User? = _currentUser.value

    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }

    suspend fun refreshCurrentUser() {
        val u = auth.currentUser ?: run {
            _currentUser.postValue(null); return
        }
        val profile = loadProfile(u.uid, u.email) ?: run {
            // buat profil minimum bila belum ada
            if (u.email != null) ensureMinimumProfile(u.uid, u.email!!)
            loadProfile(u.uid, u.email)
        }
        // jika nonaktif, jangan set sebagai current
        if (profile != null && profile.isActive) {
            _currentUser.postValue(profile)
        } else {
            logout()
        }
    }

    // ====== Login ======
    fun login(email: String, password: String) = viewModelScope.launch {
        _loading.value = true
        try {
            val res = auth.signInWithEmailAndPassword(email, password).await()
            val uid = res.user?.uid ?: throw IllegalStateException("Auth user null")
            val prof = loadProfile(uid, res.user?.email)
                ?: run {
                    val e = res.user?.email ?: email
                    ensureMinimumProfile(uid, e)
                    loadProfile(uid, e)
                }
            val user = prof ?: throw IllegalStateException("Profil tidak ditemukan")
            if (!user.isActive) throw IllegalStateException("Akun dinonaktifkan")
            _currentUser.value = user
            _loginResult.value = Result.success(user)
        } catch (t: Throwable) {
            auth.signOut()
            _loginResult.value = Result.failure(t)
        } finally {
            _loading.value = false
        }
    }

    // ====== Register ======
    fun register(fullName: String, email: String, password: String, role: String) {
        _loading.postValue(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""
                val userDoc = mapOf(
                    "uid" to uid,
                    "fullName" to fullName,
                    "email" to email,
                    "role" to role,
                    "status" to "active",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("users").document(uid)
                    .set(userDoc)
                    .addOnSuccessListener {
                        _registerResult.postValue(Result.success(User(uid, fullName, email, role)))
                        _loading.postValue(false)
                    }
                    .addOnFailureListener { e ->
                        _registerResult.postValue(Result.failure(e))
                        _loading.postValue(false)
                    }
            }
            .addOnFailureListener { e ->
                _registerResult.postValue(Result.failure(e))
                _loading.postValue(false)
            }
    }

    // ====== Ganti Password (digunakan di AccountSettings) ======
    fun changePassword(currentPassword: String, newPassword: String) = viewModelScope.launch {
        _loading.value = true
        try {
            val u = auth.currentUser ?: throw IllegalStateException("Belum login")
            val email = u.email ?: throw IllegalStateException("Email tidak tersedia")
            // re-auth
            val cred = EmailAuthProvider.getCredential(email, currentPassword)
            u.reauthenticate(cred).await()
            u.updatePassword(newPassword).await()
            _changePasswordResult.value = Result.success(Unit)
        } catch (t: Throwable) {
            _changePasswordResult.value = Result.failure(t)
        } finally {
            _loading.value = false
        }
    }
}
