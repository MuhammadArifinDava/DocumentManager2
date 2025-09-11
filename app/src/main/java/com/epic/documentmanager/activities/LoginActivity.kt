package com.epic.documentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.epic.documentmanager.R
import com.epic.documentmanager.data.mappers.UserMapper
import com.epic.documentmanager.data.repository.StaffRepository
import com.epic.documentmanager.utils.ValidationUtils
import com.epic.documentmanager.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        progressBar = findViewById(R.id.progressBar)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        observeVM()

        btnLogin.setOnClickListener { login() }
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeVM() {
        authViewModel.loginResult.observe(this) { result ->
            if (!result.isSuccess) {
                Toast.makeText(
                    this,
                    "Login gagal: Email / Password Salah",
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }

            // Sudah berhasil sign-in Auth di ViewModel
            lifecycleScope.launch {
                try {
                    val auth = FirebaseAuth.getInstance()
                    val uid = auth.currentUser?.uid ?: throw IllegalStateException("UID null")
                    val email = auth.currentUser?.email ?: ""

                    // Klaim undangan (opsional) – abaikan error permission
                    runCatching { StaffRepository().claimInviteIfExists(uid, email) }

                    // Ambil profil user secara toleran (tanpa toObject)
                    val snap = FirebaseFirestore.getInstance()
                        .collection("users").document(uid).get().await()
                    val profile = UserMapper.from(snap)

                    // Tolak jika nonaktif
                    if (!profile.isActive) {
                        FirebaseAuth.getInstance().signOut()
                        Toast.makeText(this@LoginActivity, "Akun dinonaktifkan.", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                } catch (e: Throwable) {
                    // Jika profil belum ada → buat profil minimum biar rules tidak menghalangi
                    val auth = FirebaseAuth.getInstance()
                    val uid = auth.currentUser?.uid
                    val email = auth.currentUser?.email
                    if (uid != null && email != null) {
                        FirebaseFirestore.getInstance().collection("users").document(uid)
                            .set(
                                mapOf(
                                    "uid" to uid,
                                    "email" to email,
                                    "name" to email.substringBefore("@"),
                                    "role" to "staff",
                                    "isActive" to true
                                )
                            ).await()
                        startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            e.localizedMessage ?: "Login gagal",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        authViewModel.loading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnLogin.isEnabled = !loading
            btnLogin.text = if (loading) "Loading..." else "Login"
        }
    }

    private fun login() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val fields = mapOf("email" to email, "password" to password)

        val errors = ValidationUtils.validateForm(fields)
        if (errors.isNotEmpty()) {
            Toast.makeText(this, errors.first(), Toast.LENGTH_SHORT).show()
            return
        }
        authViewModel.login(email, password)
    }
}
