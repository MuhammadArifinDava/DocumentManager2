package com.epic.documentmanager.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth

object SecondaryAuth {
    private const val APP_NAME = "secondary"

    fun get(context: Context): FirebaseAuth {
        val app = try {
            FirebaseApp.getInstance(APP_NAME)
        } catch (_: IllegalStateException) {
            val opts = FirebaseOptions.fromResource(context)
                ?: throw IllegalStateException("FirebaseOptions not found. Check google-services.json.")
            FirebaseApp.initializeApp(context, opts, APP_NAME)!!
        }
        return FirebaseAuth.getInstance(app)
    }

    fun signOut(context: Context) {
        try { get(context).signOut() } catch (_: Throwable) {}
    }
}
