package com.epic.documentmanager.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epic.documentmanager.models.User
import com.epic.documentmanager.repositories.AuthRepository
import com.epic.documentmanager.repositories.DocumentRepository
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val documentRepository = DocumentRepository()

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _documentCounts = MutableLiveData<DashboardStats>()
    val documentCounts: LiveData<DashboardStats> = _documentCounts

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun loadDashboardData() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Load current user

                // Load document counts
                val pembelianCount = documentRepository.getAllPembelianRumah().size
                val renovasiCount = documentRepository.getAllRenovasiRumah().size
                val acCount = documentRepository.getAllPemasanganAC().size
                val cctvCount = documentRepository.getAllPemasanganCCTV().size

                val stats = DashboardStats(
                    totalDocuments = pembelianCount + renovasiCount + acCount + cctvCount,
                    pembelianRumahCount = pembelianCount,
                    renovasiRumahCount = renovasiCount,
                    pemasanganACCount = acCount,
                    pemasanganCCTVCount = cctvCount
                )

                _documentCounts.value = stats

            } catch (e: Exception) {
                _documentCounts.value = DashboardStats()
            } finally {
                _loading.value = false
            }
        }
    }

    fun refreshData() {
        loadDashboardData()
    }
}

data class DashboardStats(
    val totalDocuments: Int = 0,
    val pembelianRumahCount: Int = 0,
    val renovasiRumahCount: Int = 0,
    val pemasanganACCount: Int = 0,
    val pemasanganCCTVCount: Int = 0
)