package com.epic.documentmanager.utils

import com.epic.documentmanager.R
import com.google.android.material.navigation.NavigationView

/**
 * Sembunyikan/tampilkan item drawer sesuai role.
 * - "Kelola Akun Staff"  -> hanya ADMIN
 * - "Laporan Bulanan"    -> disembunyikan untuk STAFF
 *
 * Pastikan ID item sesuai dengan menu XML:
 *  - R.id.menu_staff          = Kelola Akun Staff
 *  - R.id.nav_monthly_report  = Laporan Bulanan
 */
object DrawerVisibility {

    fun apply(navView: NavigationView, rawRole: String?) {
        val role = (rawRole ?: "").trim().lowercase()
        val isAdmin  = role == "admin"
        val isStaff  = role == "staff"

        val menu = navView.menu

        // Kelola Akun Staff → hanya admin
        menu.findItem(R.id.menu_staff)?.isVisible = isAdmin

        // Laporan Bulanan → sembunyikan untuk staff
        menu.findItem(R.id.nav_monthly_report)?.isVisible = !isStaff
    }
}
