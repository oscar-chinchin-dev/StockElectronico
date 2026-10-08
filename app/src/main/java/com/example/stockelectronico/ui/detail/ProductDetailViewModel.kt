package com.example.stockelectronico.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stockelectronico.domain.model.Producto
import com.example.stockelectronico.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val producto: Producto? = null,
    val hasError: Boolean = false
)

class ProductDetailViewModel(
    private val productoId: String,
    private val productoRepository: ProductoRepository
) : ViewModel() {
    val uiState: StateFlow<ProductDetailUiState> = productoRepository
        .observarProductoActivoPorId(productoId)
        .map { ProductDetailUiState(isLoading = false, producto = it) }
        .catch { emit(ProductDetailUiState(isLoading = false, hasError = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState())

    private val _deleteState = MutableStateFlow(DeleteUiState())
    val deleteState = _deleteState.asStateFlow()

    fun delete() {
        if (_deleteState.value.isDeleting) return
        _deleteState.update { it.copy(isDeleting = true, hasError = false) }
        viewModelScope.launch {
            runCatching { productoRepository.marcarEliminacionPendiente(productoId) }
                .onSuccess { _deleteState.update { it.copy(isDeleting = false, succeeded = true) } }
                .onFailure { _deleteState.update { it.copy(isDeleting = false, hasError = true) } }
        }
    }
}

data class DeleteUiState(val isDeleting: Boolean = false, val succeeded: Boolean = false, val hasError: Boolean = false)

class ProductDetailViewModelFactory(
    private val productoId: String,
    private val productoRepository: ProductoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProductDetailViewModel::class.java))
        return ProductDetailViewModel(productoId, productoRepository) as T
    }
}
