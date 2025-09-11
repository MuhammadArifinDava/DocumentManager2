package com.epic.documentmanager.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.epic.documentmanager.R
import com.epic.documentmanager.auth.SecondaryAuth
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class StaffFormFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilRole: TextInputLayout
    private lateinit var tilCurrentPassword: TextInputLayout
    private lateinit var tilNewPassword: TextInputLayout

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var actRole: AutoCompleteTextView
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText

    private lateinit var loading: View
    private lateinit var btnSave: View
    private lateinit var btnCancel: View

    private var targetUid: String? = null
    private var originalEmail: String = ""

    private fun String?.orIfBlank(def: String) = if (this == null || this.isBlank()) def else this

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_staff_form, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tilName = view.findViewById(R.id.tilName)
        tilEmail = view.findViewById(R.id.tilEmail)
        tilRole = view.findViewById(R.id.tilRole)
        tilCurrentPassword = view.findViewById(R.id.tilCurrentPassword)
        tilNewPassword = view.findViewById(R.id.tilNewPassword)

        etName = view.findViewById(R.id.etName)
        etEmail = view.findViewById(R.id.etEmail)
        actRole = view.findViewById(R.id.spRole)
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)

        loading = view.findViewById(R.id.progress)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)

        val roles = listOf("staff", "manager", "admin")
        actRole.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, roles))
        actRole.setOnClickListener { actRole.showDropDown() }

        targetUid = arguments?.getString(ARG_UID)
        targetUid?.let { loadUserSafe(it) }

        btnCancel.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        btnSave.setOnClickListener { onSave() }
    }

    private fun onSave() {
        val uid = targetUid
        val name = etName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
        val role = actRole.text?.toString()?.trim().orIfBlank("staff")
        val currentPass = etCurrentPassword.text?.toString()?.trim().orEmpty()
        val newPass = etNewPassword.text?.toString()?.trim().orEmpty()

        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Mode edit memerlukan UID.", Toast.LENGTH_SHORT).show()
            return
        }
        var ok = true
        if (name.isBlank()) { tilName.error = "Nama wajib diisi"; ok = false } else tilName.error = null
        if (email.isBlank()) { tilEmail.error = "Email wajib diisi"; ok = false } else tilEmail.error = null
        if (!ok) return

        val needAuthChange = (email != originalEmail) || newPass.isNotEmpty()
        if (needAuthChange && currentPass.isBlank()) {
            tilCurrentPassword.error = "Isi password saat ini untuk mengganti email/password"
            return
        } else tilCurrentPassword.error = null

        if (newPass.isNotEmpty() && newPass.length < 6) {
            tilNewPassword.error = "Password baru minimal 6 karakter"; return
        } else tilNewPassword.error = null

        loading.isVisible = true
        btnSave.isEnabled = false

        val patch = mapOf(
            "name" to name,
            "email" to email,
            "role" to role,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        suspend fun doAuthChange() {
            if (!needAuthChange) return
            try {
                val sAuth = SecondaryAuth.get(requireContext())
                val authRes = sAuth.signInWithEmailAndPassword(originalEmail, currentPass).await()
                val user = authRes.user ?: throw IllegalStateException("User sekunder null")

                if (email != originalEmail) user.updateEmail(email).await()
                if (newPass.isNotEmpty()) user.updatePassword(newPass).await()
            } finally {
                SecondaryAuth.signOut(requireContext())
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            try {
                doAuthChange()
                db.collection("users").document(uid)
                    .set(patch, SetOptions.merge()).await()

                Toast.makeText(requireContext(), "Perubahan disimpan.", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } catch (e: Throwable) {
                Toast.makeText(requireContext(), e.message ?: "Gagal menyimpan", Toast.LENGTH_LONG).show()
                loading.isVisible = false
                btnSave.isEnabled = true
            }
        }
    }

    private fun loadUserSafe(uid: String) {
        loading.isVisible = true
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val data = doc.data ?: emptyMap<String, Any?>()
                val name = (data["name"] ?: data["nama"])?.toString()?.trim().orEmpty()
                val email = (data["email"] ?: "").toString().lowercase()
                val role = (data["role"] as? String).orIfBlank("staff")
                @Suppress("UNUSED_VARIABLE")
                val createdAt: Timestamp? = data["createdAt"] as? Timestamp

                etName.setText(name)
                etEmail.setText(email)
                actRole.setText(role, false)
                originalEmail = email
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.message ?: "Gagal memuat data", Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener { loading.isVisible = false }
    }

    companion object {
        private const val ARG_UID = "uid"
        fun newInstance(uid: String?) = StaffFormFragment().apply {
            arguments = bundleOf(ARG_UID to uid)
        }
    }
}
