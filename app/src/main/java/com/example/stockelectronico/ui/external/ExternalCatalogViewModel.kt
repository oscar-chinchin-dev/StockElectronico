package com.example.stockelectronico.ui.external

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.ExternalCatalogProduct
import com.example.stockelectronico.domain.repository.ExternalCatalogRepository
import com.example.stockelectronico.domain.repository.ExternalCatalogResult
import com.example.stockelectronico.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ExternalCatalogUiState {
    data object Loading : ExternalCatalogUiState
    data class Success(val products: List<ExternalCatalogProduct>) : ExternalCatalogUiState
    data object Error : ExternalCatalogUiState
}

data class ImportUiState(
    val product: ExternalCatalogProduct? = null,
    val supplier: String = "",
    val channel: Canal = Canal.AMBOS,
    val isImporting: Boolean = false,
    val supplierInvalid: Boolean = false,
    val error: Boolean = false,
    val succeeded: Boolean = false
)

class ExternalCatalogViewModel(
    private val catalogRepository: ExternalCatalogRepository,
    private val productoRepository: ProductoRepository,
    private val importMapper: ExternalCatalogImportMapper = ExternalCatalogImportMapper()
) : ViewModel() {
    private val _catalogState = MutableStateFlow<ExternalCatalogUiState>(ExternalCatalogUiState.Loading)
    val catalogState: StateFlow<ExternalCatalogUiState> = _catalogState.asStateFlow()
    private val _importState = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    init { loadCatalog() }

    fun loadCatalog() {
        _catalogState.value = ExternalCatalogUiState.Loading
        viewModelScope.launch {
            _catalogState.value = when (val result = catalogRepository.getProducts(limit = 20, skip = 0)) {
                is ExternalCatalogResult.Success -> ExternalCatalogUiState.Success(result.value.products)
                is ExternalCatalogResult.Failure -> ExternalCatalogUiState.Error
            }
        }
    }

    fun beginImport(product: ExternalCatalogProduct) {
        _importState.value = ImportUiState(product = product)
    }

    fun cancelImport() { _importState.value = ImportUiState() }
    fun onSupplierChanged(value: String) = _importState.update { it.copy(supplier = value, supplierInvalid = false, error = false) }
    fun onChannelChanged(value: Canal) = _importState.update { it.copy(channel = value) }
    fun consumeSuccess() = _importState.update { it.copy(succeeded = false) }

    fun confirmImport() {
        val current = _importState.value
        val product = current.product ?: return
        if (current.isImporting) return
        if (current.supplier.isBlank()) {
            _importState.update { it.copy(supplierInvalid = true) }
            return
        }
        _importState.update { it.copy(isImporting = true, error = false) }
        viewModelScope.launch {
            runCatching { productoRepository.crearProducto(importMapper.toProducto(product, current.supplier, current.channel)) }
                .onSuccess { _importState.update { it.copy(isImporting = false, succeeded = true) } }
                .onFailure { _importState.update { it.copy(isImporting = false, error = true) } }
        }
    }
}

class ExternalCatalogViewModelFactory(
    private val catalogRepository: ExternalCatalogRepository,
    private val productoRepository: ProductoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ExternalCatalogViewModel::class.java))
        return ExternalCatalogViewModel(catalogRepository, productoRepository) as T
    }
}
