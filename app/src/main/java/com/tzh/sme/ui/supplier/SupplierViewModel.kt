package com.tzh.sme.ui.supplier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.data.model.SupplierModel
import com.tzh.sme.domain.usecase.supplier.SupplierUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupplierViewModel @Inject constructor(
    private val supplierUseCases: SupplierUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierUiState())
    val uiState: StateFlow<SupplierUiState> = combine(
        _uiState,
        supplierUseCases.getSuppliers().catch { e -> _uiState.update { it.copy(error = e.message) } }
    ) { state, suppliers ->
        state.copy(
            suppliers = suppliers,
            filteredSuppliers = supplierUseCases.filterSuppliers(suppliers, state.searchQuery),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SupplierUiState(isLoading = true))

    private val _sideEffect = Channel<SupplierUiSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    fun sendIntent(intent: SupplierUiIntent) {
        when (intent) {
            is SupplierUiIntent.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = intent.query) }
            is SupplierUiIntent.SelectSupplier -> selectSupplier(intent.id)
            is SupplierUiIntent.SaveSupplier -> saveSupplier(intent.name, intent.contact, intent.address, intent.category)
            is SupplierUiIntent.DeleteSupplier -> deleteSupplier(intent.id)
            SupplierUiIntent.ClearError -> _uiState.update { it.copy(error = null) }
            SupplierUiIntent.ResetSaveStatus -> _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    private fun selectSupplier(supplierId: String?) {
        if (supplierId == null) {
            _uiState.update { it.copy(selectedSupplier = null, supplierProducts = emptyList()) }
            return
        }
        viewModelScope.launch {
            val supplier = supplierUseCases.getSupplierById(supplierId)
            _uiState.update { it.copy(selectedSupplier = supplier) }
            if (supplier != null) {
                supplierUseCases.getProducts().collect { products ->
                    val filteredProducts = products.filter { it.supplierId == supplierId }
                    _uiState.update { it.copy(supplierProducts = filteredProducts) }
                }
            }
        }
    }

    private fun saveSupplier(name: String, contact: String, address: String, category: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Supplier name cannot be empty") }
            return
        }
        viewModelScope.launch {
            if (_uiState.value.suppliers.any { it.name.equals(name, ignoreCase = true) && it.id != _uiState.value.selectedSupplier?.id }) {
                _uiState.update { it.copy(error = "A supplier with the name '$name' already exists") }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, error = null) }
            val current = _uiState.value.selectedSupplier
            if (current != null) {
                supplierUseCases.updateSupplier(current.copy(name = name, contact = contact, address = address, category = category))
            } else {
                supplierUseCases.addSupplier(SupplierModel(name = name, contact = contact, address = address, category = category))
            }
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            _sideEffect.send(SupplierUiSideEffect.NavigateBack)
        }
    }

    private fun deleteSupplier(supplierId: String) {
        viewModelScope.launch {
            supplierUseCases.deleteSupplier(supplierId)
            _sideEffect.send(SupplierUiSideEffect.NavigateBack)
        }
    }
}
