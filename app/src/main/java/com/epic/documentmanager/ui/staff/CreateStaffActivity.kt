package com.epic.documentmanager.ui.staff

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.epic.documentmanager.R
import com.epic.documentmanager.auth.SecondaryAuth
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CreateStaffActivity : AppCompatActivity() {

    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilRole: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirm: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var actRole: AutoCompleteTextView
    private lateinit var progress: View
    private lateinit var btnSave: View
    private lateinit var btnCancel: View

    private fun String?.orIfBlank(default: String) =
        if (this == null || this.isBlank()) default else this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_staff)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tambah Staff"

        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilRole = findViewById(R.id.tilRole)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirm = findViewById(R.id.tilConfirm)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirm = findViewById(R.id.etConfirm)
        actRole = findViewById(R.id.spRole)
        progress = findViewById(R.id.progress)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        val roles = listOf("staff", "manager", "admin")
        actRole.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, roles))
        actRole.setText("staff", false)
        actRole.setOnClickListener { actRole.showDropDown() }

        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { createStaff() }
    }

    private fun createStaff() {
        val name = etName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
        val role = actRole.text?.toString()?.trim().orIfBlank("staff")
        val pass = etPassword.text?.toString()?.trim().orEmpty()
        val conf = etConfirm.text?.toString()?.trim().orEmpty()

        var ok = true
        if (name.isBlank()) { tilName.error = "Nama wajib"; ok = false } else tilName.error = null
        if (email.isBlank()) { tilEmail.error = "Email wajib"; ok = false } else tilEmail.error = null
        if (pass.length < 6) { tilPassword.error = "Min 6 karakter"; ok = false } else tilPassword.error = null
        if (pass != conf) { tilConfirm.error = "Tidak sama"; ok = false } else tilConfirm.error = null
        if (!ok) return

        progress.visibility = View.VISIBLE; btnSave.isEnabled = false

        val sAuth = SecondaryAuth.get(this)
        sAuth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { res ->
                val uid = res.user?.uid ?: return@addOnSuccessListener
                val profile = mapOf(
                    "uid" to uid,
                    "email" to email,
                    "name" to name,
                    "role" to role,
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(profile)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Staff dibuat.", Toast.LENGTH_SHORT).show()
                        SecondaryAuth.signOut(this)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, it.message ?: "Gagal menyimpan profil", Toast.LENGTH_LONG).show()
                        SecondaryAuth.signOut(this); stop()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message ?: "Gagal membuat akun", Toast.LENGTH_LONG).show()
                stop()
            }
    }

    private fun stop() { progress.visibility = View.GONE; btnSave.isEnabled = true }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
