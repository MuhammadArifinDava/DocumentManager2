package com.epic.documentmanager.viewmodels

import android.net.Uri
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.Source
import com.epic.documentmanager.models.*
import com.epic.documentmanager.repositories.DocumentRepository
import com.epic.documentmanager.repositories.SearchResult
import com.epic.documentmanager.repositories.StorageRepository
import kotlinx.coroutines.launch
import android.app.Application
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.epic.documentmanager.utils.FileMeta

class DocumentViewModel : ViewModel() {
    private val documentRepository = DocumentRepository()
    private val storageRepository = StorageRepository()

    private val _pembelianRumahList = MutableLiveData<List<PembelianRumah>>()
    val pembelianRumahList: LiveData<List<PembelianRumah>> = _pembelianRumahList

    private val _renovasiRumahList = MutableLiveData<List<RenovasiRumah>>()
    val renovasiRumahList: LiveData<List<RenovasiRumah>> = _renovasiRumahList

    private val _pemasanganACList = MutableLiveData<List<PemasanganAC>>()
    val pemasanganACList: LiveData<List<PemasanganAC>> = _pemasanganACList

    private val _pemasanganCCTVList = MutableLiveData<List<PemasanganCCTV>>()
    val pemasanganCCTVList: LiveData<List<PemasanganCCTV>> = _pemasanganCCTVList

    private val _saveResult = MutableLiveData<Result<String>>()
    val saveResult: LiveData<Result<String>> = _saveResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    private val _uploadResult = MutableLiveData<Result<List<String>>>()
    val uploadResult: LiveData<Result<List<String>>> = _uploadResult

    private val _searchResult = MutableLiveData<SearchResult>()
    val searchResult: LiveData<SearchResult> = _searchResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _uploadProgress = MutableLiveData<Int>()
    val uploadProgress: LiveData<Int> = _uploadProgress

    // Save Documents
    fun savePembelianRumah(data: PembelianRumah) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = documentRepository.savePembelianRumah(data)
                _saveResult.value = result
                if (result.isSuccess) loadAllPembelianRumah()
            } catch (e: Exception) {
                _saveResult.value = Result.failure(e)
            } finally { _loading.value = false }
        }
    }

    fun saveRenovasiRumah(data: RenovasiRumah) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = documentRepository.saveRenovasiRumah(data)
                _saveResult.value = result
                if (result.isSuccess) loadAllRenovasiRumah()
            } catch (e: Exception) {
                _saveResult.value = Result.failure(e)
            } finally { _loading.value = false }
        }
    }

    fun savePemasanganAC(data: PemasanganAC) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = documentRepository.savePemasanganAC(data)
                _saveResult.value = result
                if (result.isSuccess) loadAllPemasanganAC()
            } catch (e: Exception) {
                _saveResult.value = Result.failure(e)
            } finally { _loading.value = false }
        }
    }

    fun savePemasanganCCTV(data: PemasanganCCTV) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = documentRepository.savePemasanganCCTV(data)
                _saveResult.value = result
                if (result.isSuccess) loadAllPemasanganCCTV()
            } catch (e: Exception) {
                _saveResult.value = Result.failure(e)
            } finally { _loading.value = false }
        }
    }

    // Load Documents
    fun loadAllPembelianRumah() {
        viewModelScope.launch {
            try { _pembelianRumahList.value = documentRepository.getAllPembelianRumah() }
            catch (_: Exception) { _pembelianRumahList.value = emptyList() }
        }
    }

    fun loadAllRenovasiRumah() {
        viewModelScope.launch {
            try { _renovasiRumahList.value = documentRepository.getAllRenovasiRumah() }
            catch (_: Exception) { _renovasiRumahList.value = emptyList() }
        }
    }

    fun loadAllPemasanganAC() {
        viewModelScope.launch {
            try { _pemasanganACList.value = documentRepository.getAllPemasanganAC() }
            catch (_: Exception) { _pemasanganACList.value = emptyList() }
        }
    }

    fun loadAllPemasanganCCTV() {
        viewModelScope.launch {
            try { _pemasanganCCTVList.value = documentRepository.getAllPemasanganCCTV() }
            catch (_: Exception) { _pemasanganCCTVList.value = emptyList() }
        }
    }

    fun loadAllDocuments() {
        loadAllPembelianRumah()
        loadAllRenovasiRumah()
        loadAllPemasanganAC()
        loadAllPemasanganCCTV()
    }

    // FORCE REFRESH: ambil langsung dari SERVER (buat onResume daftar dokumen)
    fun reloadAllFromServer() {
        viewModelScope.launch {
            try {
                _pembelianRumahList.value = documentRepository.getAllPembelianRumah(Source.SERVER)
                _renovasiRumahList.value  = documentRepository.getAllRenovasiRumah(Source.SERVER)
                _pemasanganACList.value   = documentRepository.getAllPemasanganAC(Source.SERVER)
                _pemasanganCCTVList.value = documentRepository.getAllPemasanganCCTV(Source.SERVER)
            } catch (_: Exception) { /* biarin */ }
        }
    }

    // Delete Documents
    fun deletePembelianRumah(id: String) {
        viewModelScope.launch {
            try {
                val result = documentRepository.deletePembelianRumah(id)
                _deleteResult.value = result
                if (result.isSuccess) loadAllPembelianRumah()
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
            }
        }
    }

    fun deleteRenovasiRumah(id: String) {
        viewModelScope.launch {
            try {
                val result = documentRepository.deleteRenovasiRumah(id)
                _deleteResult.value = result
                if (result.isSuccess) loadAllRenovasiRumah()
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
            }
        }
    }

    fun deletePemasanganAC(id: String) {
        viewModelScope.launch {
            try {
                val result = documentRepository.deletePemasanganAC(id)
                _deleteResult.value = result
                if (result.isSuccess) loadAllPemasanganAC()
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
            }
        }
    }

    fun deletePemasanganCCTV(id: String) {
        viewModelScope.launch {
            try {
                val result = documentRepository.deletePemasanganCCTV(id)
                _deleteResult.value = result
                if (result.isSuccess) loadAllPemasanganCCTV()
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
            }
        }
    }

    // Upload Files
    // semula
// fun uploadFiles(uris: List<Uri>, docType: String, displayNames: List<String>? = null)

    // ganti jadi:
    fun uploadFiles(
        context: Context,
        uris: List<Uri>,
        docType: String,
        displayNames: List<String>? = null
    ) {
        if (uris.isEmpty()) {
            _uploadResult.postValue(Result.success(emptyList()))
            return
        }

        val storage = FirebaseStorage.getInstance()
        val outUrls = MutableList<String?>(uris.size) { null }
        var failed: Exception? = null
        var done = 0

        uris.forEachIndexed { index, uri ->
            // gunakan context yang diterima (bukan getApplication)
            val fromUri = FileMeta.fromUri(context, uri)

            val wantedNameRaw = displayNames?.getOrNull(index)?.takeIf { it.isNotBlank() } ?: fromUri.displayName
            val wantedName = FileMeta.ensureExt(wantedNameRaw, fromUri.mimeType)

            val safeName = wantedName.replace(Regex("""[^\w\-. ]"""), "_")
            val path = "documents/$docType/${System.currentTimeMillis()}_$safeName"

            val ref = storage.reference.child(path)
            val metadata = StorageMetadata.Builder()
                .setContentType(fromUri.mimeType)
                .build()

            ref.putFile(uri, metadata)
                .addOnSuccessListener {
                    ref.downloadUrl
                        .addOnSuccessListener { dl ->
                            outUrls[index] = dl.toString()
                            done++
                            if (done == uris.size) {
                                if (failed == null) {
                                    _uploadResult.postValue(Result.success(outUrls.filterNotNull()))
                                } else {
                                    _uploadResult.postValue(Result.failure(failed!!))
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            failed = e
                            done++
                            if (done == uris.size) _uploadResult.postValue(Result.failure(e))
                        }
                }
                .addOnFailureListener { e ->
                    failed = e
                    done++
                    if (done == uris.size) _uploadResult.postValue(Result.failure(e))
                }
        }
    }

    // Search Documents
    fun searchDocuments(query: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = documentRepository.searchDocuments(query)
                _searchResult.value = result
            } catch (_: Exception) {
                _searchResult.value = SearchResult(emptyList(), emptyList(), emptyList(), emptyList())
            } finally { _loading.value = false }
        }
    }

    fun clearSearchResult() {
        _searchResult.value = SearchResult(emptyList(), emptyList(), emptyList(), emptyList())
    }
}
