package com.epic.documentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.epic.documentmanager.R
import com.epic.documentmanager.utils.ValidationUtils
import com.epic.documentmanager.viewmodels.AuthViewModel

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel

    // Views
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnChangePassword: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_settings)

        setupActionBar()
        initViews()
        setupViewModel()
        setupObservers()
        setupClickListeners()

        loadUserData()
    }

    private fun setupActionBar() {
        // pakai toolbar dari layout supaya tombol Back selalu tampil meski theme NoActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Pengaturan Akun"
        }
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun initViews() {
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUserRole = findViewById(R.id.tvUserRole)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupViewModel() {
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
    }

    private fun setupObservers() {
        authViewModel.currentUser.observe(this) { user ->
            user?.let {
                tvUserName.text = it.fullName
                tvUserEmail.text = it.email
                tvUserRole.text = it.role.uppercase()
            }
        }

        authViewModel.changePasswordResult.observe(this) { result ->
            progressBar.visibility = View.GONE
            btnChangePassword.isEnabled = true

            if (result.isSuccess) {
                Toast.makeText(this, "Password berhasil diubah", Toast.LENGTH_SHORT).show()
                clearPasswordFields()
            } else {
                Toast.makeText(
                    this,
                    "Gagal mengubah password: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        authViewModel.loading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        btnChangePassword.setOnClickListener { changePassword() }
        btnLogout.setOnClickListener { showLogoutConfirmation() }
    }

    private fun loadUserData() {
        authViewModel.getCurrentUser()
    }

    private fun changePassword() {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        val fields = mapOf(
            "Password saat ini" to currentPassword,
            "Password baru" to newPassword,
            "Konfirmasi password" to confirmPassword
        )

        val errors = ValidationUtils.validateForm(fields)
        if (errors.isNotEmpty()) {
            Toast.makeText(this, errors.first(), Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Password baru dan konfirmasi password tidak sama", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "Password baru minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnChangePassword.isEnabled = false

        authViewModel.changePassword(currentPassword, newPassword)
    }

    private fun clearPasswordFields() {
        etCurrentPassword.text.clear()
        etNewPassword.text.clear()
        etConfirmPassword.text.clear()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ -> logout() }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun logout() {
        authViewModel.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
