package com.epic.documentmanager.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epic.documentmanager.data.mappers.User   // ⬅️ gunakan User dari mappers
import com.epic.documentmanager.data.repository.StaffRepository
import kotlinx.coroutines.launch

class StaffViewModel(
    private val repo: StaffRepository = StaffRepository()
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>(emptyList())
    val users: LiveData<List<User>> = _users

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun startListening() = viewModelScope.launch {
        try {
            _loading.value = true
            _users.value = repo.fetchUsers()
        } catch (t: Throwable) {
            _error.value = t.message
        } finally {
            _loading.value = false
        }
    }

    fun refresh() = startListening()

    fun search(q: String) = viewModelScope.launch {
        try {
            _loading.value = true
            _users.value = repo.searchUsers(q)
        } catch (t: Throwable) {
            _error.value = t.message
        } finally {
            _loading.value = false
        }
    }

    fun deleteUser(uid: String, onDone: () -> Unit) = viewModelScope.launch {
        try {
            _loading.value = true
            repo.deleteUserProfile(uid)
            onDone()
            _users.value = repo.fetchUsers()
        } catch (t: Throwable) {
            _error.value = t.message
        } finally {
            _loading.value = false
        }
    }
}
