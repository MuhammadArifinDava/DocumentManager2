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

        // Toolbar opsional: kalau tidak ada di layout tidak bikin crash
        findViewById<MaterialToolbar?>(R.id.toolbar)?.let { tb ->
            setSupportActionBar(tb)
            tb.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Upload Dokumen"
        }

        // Wajib ada di layout berikut ID-nya
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4
            override fun createFragment(position: Int) = when (position) {
                0 -> PembelianRumahFragment()
                1 -> RenovasiRumahFragment()
                2 -> PemasanganACFragment()
                else -> PemasanganCCTVFragment()
            }
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
