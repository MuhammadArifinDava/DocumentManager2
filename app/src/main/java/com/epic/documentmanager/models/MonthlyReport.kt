package com.epic.documentmanager.models
data class MonthlyReport(
    val month: String = "",
    val year: String = "",
    val totalDocuments: Int = 0,
    val pembelianRumahCount: Int = 0,
    val renovasiRumahCount: Int = 0,
    val pemasanganACCount: Int = 0,
    val pemasanganCCTVCount: Int = 0,
    val documents: List<Document> = listOf(),
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String = ""
) {
    constructor() : this("", "", 0, 0, 0, 0, 0, listOf(), 0L, "")
}