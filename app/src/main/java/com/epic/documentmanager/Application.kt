package com.epic.documentmanager

import android.app.Application
import com.google.firebase.FirebaseApp

class DocumentManagerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize other components if needed
        setupCrashlytics()
        setupAnalytics()
    }

    private fun setupCrashlytics() {
        // Setup Firebase Crashlytics if needed
        // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    private fun setupAnalytics() {
        // Setup Firebase Analytics if needed
        // FirebaseAnalytics.getInstance(this)
    }
}