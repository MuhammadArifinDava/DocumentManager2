package com.epic.documentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.epic.documentmanager.R
import com.epic.documentmanager.viewmodels.AuthViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Splash 2 detik, lalu cek auth
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationStatus()
        }, 2000)
    }

    private fun checkAuthenticationStatus() {
        if (authViewModel.isUserLoggedIn()) {
            // User logged in → ke dashboard setelah ambil profil
            authViewModel.getCurrentUser()
            authViewModel.currentUser.observe(this) { user ->
                if (user != null) {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    goToLogin()
                }
            }
        } else {
            goToLogin()
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
