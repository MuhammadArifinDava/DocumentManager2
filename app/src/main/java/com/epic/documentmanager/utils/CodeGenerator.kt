package com.epic.documentmanager.utils

import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

object CodeGenerator {

    private val dateFormat = SimpleDateFormat("yyyyMM", Locale.getDefault())

    /**
     * Generate unique code for Pembelian Rumah documents
     * Format: PR-YYYYMM-XXXX
     */
    fun generateCodeForPembelianRumah(): String {
        return generateCode(Constants.PREFIX_PEMBELIAN_RUMAH)
    }

    /**
     * Generate unique code for Renovasi Rumah documents
     * Format: RR-YYYYMM-XXXX
     */
    fun generateCodeForRenovasiRumah(): String {
        return generateCode(Constants.PREFIX_RENOVASI_RUMAH)
    }

    /**
     * Generate unique code for Pemasangan AC documents
     * Format: AC-YYYYMM-XXXX
     */
    fun generateCodeForPemasanganAC(): String {
        return generateCode(Constants.PREFIX_PEMASANGAN_AC)
    }

    /**
     * Generate unique code for Pemasangan CCTV documents
     * Format: CC-YYYYMM-XXXX
     */
    fun generateCodeForPemasanganCCTV(): String {
        return generateCode(Constants.PREFIX_PEMASANGAN_CCTV)
    }

    /**
     * Generate code with given prefix
     * Format: PREFIX-YYYYMM-XXXX
     */
    private fun generateCode(prefix: String): String {
        val yearMonth = dateFormat.format(Date())
        val randomNumber = Random.nextInt(1000, 9999)
        return "$prefix-$yearMonth-$randomNumber"
    }

    /**
     * Generate random alphanumeric code
     */
    fun generateRandomCode(length: Int = 8): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Generate sequential code with given prefix and sequence number
     * Format: PREFIX-YYYYMM-XXXX (where XXXX is padded sequence)
     */
    fun generateSequentialCode(prefix: String, sequence: Int): String {
        val yearMonth = dateFormat.format(Date())
        val paddedSequence = sequence.toString().padStart(4, '0')
        return "$prefix-$yearMonth-$paddedSequence"
    }

    /**
     * Generate code for monthly report
     * Format: REPORT-YYYYMM-XXXX
     */
    fun generateReportCode(month: String, year: String): String {
        val randomNumber = Random.nextInt(1000, 9999)
        return "REPORT-$year$month-$randomNumber"
    }

    /**
     * Extract year and month from document code
     */
    fun extractDateFromCode(code: String): Pair<String?, String?> {
        val parts = code.split("-")
        if (parts.size >= 2) {
            val dateString = parts[1]
            if (dateString.length == 6) {
                val year = dateString.substring(0, 4)
                val month = dateString.substring(4, 6)
                return Pair(year, month)
            }
        }
        return Pair(null, null)
    }

    /**
     * Validate document code format
     */
    fun isValidDocumentCode(code: String): Boolean {
        val pattern = Regex("^(PR|RR|AC|CC)-\\d{6}-\\d{4}$")
        return pattern.matches(code)
    }

    /**
     * Get document type from code
     */
    fun getDocumentTypeFromCode(code: String): String? {
        val parts = code.split("-")
        if (parts.isNotEmpty()) {
            return when (parts[0]) {
                Constants.PREFIX_PEMBELIAN_RUMAH -> Constants.DOC_TYPE_PEMBELIAN_RUMAH
                Constants.PREFIX_RENOVASI_RUMAH -> Constants.DOC_TYPE_RENOVASI_RUMAH
                Constants.PREFIX_PEMASANGAN_AC -> Constants.DOC_TYPE_PEMASANGAN_AC
                Constants.PREFIX_PEMASANGAN_CCTV -> Constants.DOC_TYPE_PEMASANGAN_CCTV
                else -> null
            }
        }
        return null
    }
}