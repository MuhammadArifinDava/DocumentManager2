// app/src/main/java/com/epic/documentmanager/activities/UploadDocumentActivity.kt
package com.epic.documentmanager.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.epic.documentmanager.R
import com.epic.documentmanager.fragments.PembelianRumahFragment
import com.epic.documentmanager.fragments.RenovasiRumahFragment
import com.epic.documentmanager.fragments.PemasanganACFragment
import com.epic.documentmanager.fragments.PemasanganCCTVFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class UploadDocumentActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_document)

        // Toolbar opsional
        findViewById<MaterialToolbar?>(R.id.toolbar)?.let { tb ->
            setSupportActionBar(tb)
            tb.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Upload Dokumen"
        }

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        // Penting: simpan 4 fragment sekaligus supaya upload tidak terputus saat geser tab
        viewPager.offscreenPageLimit = 4

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4
            override fun createFragment(position: Int) = when (position) {
                0 -> PembelianRumahFragment()
                1 -> RenovasiRumahFragment()
                2 -> PemasanganACFragment()
                else -> PemasanganCCTVFragment()
            }

            // Stabilkan ID agar Fragment tidak dibuat ulang tanpa perlu
            override fun getItemId(position: Int): Long = when (position) {
                0 -> 100L
                1 -> 200L
                2 -> 300L
                else -> 400L
            }
            override fun containsItem(itemId: Long): Boolean =
                itemId == 100L || itemId == 200L || itemId == 300L || itemId == 400L
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = when (pos) {
                0 -> "Pembelian Rumah"
                1 -> "Renovasi Rumah"
                2 -> "Pemasangan AC"
                else -> "Pemasangan CCTV"
            }
        }.attach()
    }
}
