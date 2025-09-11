package com.epic.documentmanager.data.model

import com.google.firebase.Timestamp

data class Invite(
        val id: String = "",
        val email: String = "",
        val role: String = "staff",   // admin | manager | staff
        val isActive: Boolean = true,
        val used: Boolean = false,
        val createdAt: Timestamp? = null
)
