package com.epic.documentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.epic.documentmanager.R
import com.epic.documentmanager.utils.DrawerVisibility
import com.epic.documentmanager.viewmodels.AuthViewModel
import com.epic.documentmanager.viewmodels.DashboardViewModel
import com.google.android.material.navigation.NavigationView

class DashboardActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    // VM
    private lateinit var dashboardVM: DashboardViewModel
    private lateinit var authVM: AuthViewModel

    // Drawer
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    // Refresh
    private lateinit var swipeRefresh: SwipeRefreshLayout

    // Cards
    private lateinit var cardTotalDocuments: CardView
    private lateinit var cardPembelianRumah: CardView
    private lateinit var cardRenovasiRumah: CardView
    private lateinit var cardPemasanganAC: CardView
    private lateinit var cardPemasanganCCTV: CardView

    // Texts
    private lateinit var tvWelcome: TextView
    private lateinit var tvTotalDocuments: TextView
    private lateinit var tvPembelianRumah: TextView
    private lateinit var tvRenovasiRumah: TextView
    private lateinit var tvPemasanganAC: TextView
    private lateinit var tvPemasanganCCTV: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initViews()
        setupViewModels()
        setupDrawer()
        setupObservers()
        setupClickListeners()

        // === penting: samakan dgn AccountSettings ===
        // muat profil user dari AuthViewModel agar nama/email/role terisi
        authVM.getCurrentUser()

        dashboardVM.loadDashboardData()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        cardTotalDocuments = findViewById(R.id.cardTotalDocuments)
        cardPembelianRumah = findViewById(R.id.cardPembelianRumah)
        cardRenovasiRumah = findViewById(R.id.cardRenovasiRumah)
        cardPemasanganAC = findViewById(R.id.cardPemasanganAC)
        cardPemasanganCCTV = findViewById(R.id.cardPemasanganCCTV)

        tvWelcome = findViewById(R.id.tvWelcome)
        tvTotalDocuments = findViewById(R.id.tvTotalDocuments)
        tvPembelianRumah = findViewById(R.id.tvPembelianRumah)
        tvRenovasiRumah = findViewById(R.id.tvRenovasiRumah)
        tvPemasanganAC = findViewById(R.id.tvPemasanganAC)
        tvPemasanganCCTV = findViewById(R.id.tvPemasanganCCTV)
    }

    private fun setupViewModels() {
        dashboardVM = ViewModelProvider(this)[DashboardViewModel::class.java]
        authVM = ViewModelProvider(this)[AuthViewModel::class.java]
    }

    private fun setupDrawer() {
        setSupportActionBar(findViewById(R.id.toolbar))
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, findViewById(R.id.toolbar),
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

        // Pastikan header ada
        if (navigationView.headerCount == 0) {
            navigationView.inflateHeaderView(R.layout.nav_header_dashboard)
        }
    }

    private fun setupObservers() {
        // === ambil profil user dari AuthViewModel (sama seperti AccountSettings) ===
        authVM.currentUser.observe(this) { user ->
            user ?: return@observe

            tvWelcome.text = "Selamat datang, ${user.fullName}!"

            val header = navigationView.getHeaderView(0)
            header.findViewById<TextView>(R.id.tvHeaderName).text = user.fullName
            header.findViewById<TextView>(R.id.tvHeaderEmail).text = user.email
            header.findViewById<TextView>(R.id.tvHeaderRole).text = user.role.uppercase()

            // visibilitas menu sesuai role
            DrawerVisibility.apply(navigationView, user.role)
        }

        dashboardVM.documentCounts.observe(this) { s ->
            tvTotalDocuments.text = s.totalDocuments.toString()
            tvPembelianRumah.text = s.pembelianRumahCount.toString()
            tvRenovasiRumah.text = s.renovasiRumahCount.toString()
            tvPemasanganAC.text = s.pemasanganACCount.toString()
            tvPemasanganCCTV.text = s.pemasanganCCTVCount.toString()
        }

        dashboardVM.loading.observe(this) { swipeRefresh.isRefreshing = it }
    }

    private fun setupClickListeners() {
        swipeRefresh.setOnRefreshListener { dashboardVM.refreshData() }

        cardTotalDocuments.setOnClickListener {
            startActivity(Intent(this, DocumentListActivity::class.java))
        }
        cardPembelianRumah.setOnClickListener {
            Intent(this, DocumentListActivity::class.java).apply {
                putExtra("documentType", com.epic.documentmanager.utils.Constants.DOC_TYPE_PEMBELIAN_RUMAH)
                startActivity(this)
            }
        }
        cardRenovasiRumah.setOnClickListener {
            Intent(this, DocumentListActivity::class.java).apply {
                putExtra("documentType", com.epic.documentmanager.utils.Constants.DOC_TYPE_RENOVASI_RUMAH)
                startActivity(this)
            }
        }
        cardPemasanganAC.setOnClickListener {
            Intent(this, DocumentListActivity::class.java).apply {
                putExtra("documentType", com.epic.documentmanager.utils.Constants.DOC_TYPE_PEMASANGAN_AC)
                startActivity(this)
            }
        }
        cardPemasanganCCTV.setOnClickListener {
            Intent(this, DocumentListActivity::class.java).apply {
                putExtra("documentType", com.epic.documentmanager.utils.Constants.DOC_TYPE_PEMASANGAN_CCTV)
                startActivity(this)
            }
        }
    }

    // ==== klik menu drawer ====
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_upload -> startActivity(Intent(this, UploadDocumentActivity::class.java))
            R.id.menu_list   -> startActivity(Intent(this, DocumentListActivity::class.java))

            R.id.nav_monthly_report -> {
                startActivity(Intent(this, MonthlyReportActivity::class.java))
                true
            }

            R.id.menu_staff -> {
                // Aman: item ini sudah disembunyikan untuk non-admin oleh DrawerVisibility
                startActivity(Intent(this, com.epic.documentmanager.ui.staff.StaffHostActivity::class.java))
            }

            R.id.menu_settings -> startActivity(Intent(this, AccountSettingsActivity::class.java))
            R.id.menu_logout   -> logout()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logout() {
        authVM.logout()
        val i = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(i)
        finish()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        dashboardVM.refreshData()
    }
}
