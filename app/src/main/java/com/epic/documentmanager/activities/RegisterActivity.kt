// app/src/main/java/com/epic/documentmanager/activities/RegisterActivity.kt
package com.epic.documentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.epic.documentmanager.R
import com.epic.documentmanager.utils.Constants
import com.epic.documentmanager.utils.ValidationUtils
import com.epic.documentmanager.viewmodels.AuthViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupViewModel()
        setupSpinner()
        setupObservers()
        setupClickListeners()
    }

    private fun initViews() {
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        spinnerRole = findViewById(R.id.spinnerRole)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupViewModel() {
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
    }

    private fun setupSpinner() {
        val roles = arrayOf(
            Constants.ROLE_STAFF,
            Constants.ROLE_MANAGER,
            Constants.ROLE_ADMIN
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter
        spinnerRole.setSelection(0)
    }

    private fun setupObservers() {
        authViewModel.registerResult.observe(this) { result ->
            if (result.isSuccess) {
                val user = result.getOrNull()
                Toast.makeText(
                    this,
                    "Registrasi berhasil! Selamat datang ${user?.fullName ?: ""}",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Registrasi gagal: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        authViewModel.loading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnRegister.isEnabled = !isLoading
            btnRegister.text = if (isLoading) "Loading..." else "Register"
        }
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener { register() }
        tvLogin.setOnClickListener { finish() }
    }

    private fun register() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val role = spinnerRole.selectedItem.toString()

        val fields = mapOf(
            "Nama Lengkap" to fullName,
            "email" to email,
            "password" to password
        )

        val errors = ValidationUtils.validateForm(fields)
        if (errors.isNotEmpty()) {
            Toast.makeText(this, errors.first(), Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Password dan konfirmasi password tidak sama", Toast.LENGTH_SHORT).show()
            return
        }

        authViewModel.register(fullName, email, password, role)
    }
}
