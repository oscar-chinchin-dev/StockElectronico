package com.example.stockelectronico.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stockelectronico.domain.model.Producto
import com.example.stockelectronico.domain.repository.ProductoRepository
import com.example.stockelectronico.data.sync.ProductoSyncLocalDataSource
import com.example.stockelectronico.data.sync.ProductoSyncManager
import com.example.stockelectronico.data.sync.ProductoSyncState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class InventoryUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val productos: List<Producto> = emptyList(),
    val hasError: Boolean = false,
    val pendingCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncFailed: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val productoRepository: ProductoRepository,
    private val syncManager: ProductoSyncManager? = null,
    private val syncLocal: ProductoSyncLocalDataSource? = null
) : ViewModel() {
    private val query = MutableStateFlow("")

    private val inventoryState: StateFlow<InventoryUiState> = query
        .flatMapLatest { texto ->
            val productos = if (texto.isBlank()) {
                productoRepository.observarProductosActivos()
            } else {
                productoRepository.buscarProductos(texto)
            }
            productos.map { InventoryUiState(isLoading = false, query = texto, productos = it) }
                .catch { emit(InventoryUiState(isLoading = false, query = texto, hasError = true)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    val uiState: StateFlow<InventoryUiState> = kotlinx.coroutines.flow.combine(
        inventoryState,
        syncLocal?.observePendingCount() ?: kotlinx.coroutines.flow.flowOf(0),
        syncManager?.state ?: kotlinx.coroutines.flow.flowOf(ProductoSyncState.Idle)
    ) { inventory, pendingCount, syncState ->
        inventory.copy(
            pendingCount = pendingCount,
            isSyncing = syncState is ProductoSyncState.Syncing,
            syncFailed = (syncState as? ProductoSyncState.Completed)?.result?.failed?.let { it > 0 } == true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun onQueryChanged(value: String) {
        query.update { value }
    }

    fun synchronize() { syncManager?.let { manager -> viewModelScope.launch { manager.synchronize() } } }
}

class InventoryViewModelFactory(
    private val productoRepository: ProductoRepository,
    private val syncManager: ProductoSyncManager? = null,
    private val syncLocal: ProductoSyncLocalDataSource? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(InventoryViewModel::class.java))
        return InventoryViewModel(productoRepository, syncManager, syncLocal) as T
    }
}
