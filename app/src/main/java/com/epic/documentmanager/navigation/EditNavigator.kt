package com.epic.documentmanager.navigation

import android.content.Context
import android.content.Intent
import com.epic.documentmanager.activities.EditDocumentActivity
import com.epic.documentmanager.models.PembelianRumah
import com.epic.documentmanager.models.RenovasiRumah
import com.epic.documentmanager.models.PemasanganAC
import com.epic.documentmanager.models.PemasanganCCTV
import com.epic.documentmanager.utils.Constants

object EditNavigator {

    fun goToEdit(context: Context, data: PembelianRumah) {
        context.startActivity(
            Intent(context, EditDocumentActivity::class.java).apply {
                putExtra(EditDocumentActivity.EXTRA_DOC_TYPE, Constants.DOC_TYPE_PEMBELIAN_RUMAH)
                putExtra(EditDocumentActivity.EXTRA_DOCUMENT, data) // Serializable di model
            }
        )
    }

    fun goToEdit(context: Context, data: RenovasiRumah) {
        context.startActivity(
            Intent(context, EditDocumentActivity::class.java).apply {
                putExtra(EditDocumentActivity.EXTRA_DOC_TYPE, Constants.DOC_TYPE_RENOVASI_RUMAH)
                putExtra(EditDocumentActivity.EXTRA_DOCUMENT, data)
            }
        )
    }

    fun goToEdit(context: Context, data: PemasanganAC) {
        context.startActivity(
            Intent(context, EditDocumentActivity::class.java).apply {
                putExtra(EditDocumentActivity.EXTRA_DOC_TYPE, Constants.DOC_TYPE_PEMASANGAN_AC)
                putExtra(EditDocumentActivity.EXTRA_DOCUMENT, data)
            }
        )
    }

    fun goToEdit(context: Context, data: PemasanganCCTV) {
        context.startActivity(
            Intent(context, EditDocumentActivity::class.java).apply {
                putExtra(EditDocumentActivity.EXTRA_DOC_TYPE, Constants.DOC_TYPE_PEMASANGAN_CCTV)
                putExtra(EditDocumentActivity.EXTRA_DOCUMENT, data)
            }
        )
    }
}
