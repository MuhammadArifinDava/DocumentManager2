package com.epic.documentmanager.models
@androidx.annotation.Keep
data class Document(
    val id: String = "",
    val uniqueCode: String = "",
    val documentType: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val updatedBy: String = "",
    val status: String = "active",
    val documentData: Map<String, Any> = mapOf(),
    val attachments: List<String> = listOf()
) {
    constructor() : this("", "", "", "", "", "", 0L, 0L, "", "", "active", mapOf(), listOf())
}