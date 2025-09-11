package com.epic.documentmanager.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.epic.documentmanager.R
import com.epic.documentmanager.fragments.PembelianRumahFragment
import com.epic.documentmanager.fragments.PemasanganACFragment
import com.epic.documentmanager.fragments.PemasanganCCTVFragment
import com.epic.documentmanager.fragments.RenovasiRumahFragment
import com.epic.documentmanager.models.PembelianRumah
import com.epic.documentmanager.models.PemasanganAC
import com.epic.documentmanager.models.PemasanganCCTV
import com.epic.documentmanager.models.RenovasiRumah
import com.epic.documentmanager.utils.Constants

class EditDocumentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DOC_TYPE = "extra_doc_type"
        const val EXTRA_DOCUMENT = "extra_document"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_document)

        supportActionBar?.apply {
            title = getString(R.string.title_edit_document)
            setDisplayHomeAsUpEnabled(true)
        }

        if (savedInstanceState != null) return

        val docType = intent.getStringExtra(EXTRA_DOC_TYPE) ?: ""
        val doc = intent.getSerializableExtra(EXTRA_DOCUMENT)

        val fragment = when (docType) {
            Constants.DOC_TYPE_PEMBELIAN_RUMAH -> {
                val data = doc as PembelianRumah
                PembelianRumahFragment.newInstanceForEdit(data)
            }
            Constants.DOC_TYPE_RENOVASI_RUMAH -> {
                val data = doc as RenovasiRumah
                RenovasiRumahFragment.newInstanceForEdit(data)
            }
            Constants.DOC_TYPE_PEMASANGAN_AC -> {
                val data = doc as PemasanganAC
                PemasanganACFragment.newInstanceForEdit(data)
            }
            Constants.DOC_TYPE_PEMASANGAN_CCTV -> {
                val data = doc as PemasanganCCTV
                PemasanganCCTVFragment.newInstanceForEdit(data)
            }
            else -> null
        } ?: run {
            finish()
            return
        }

        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
