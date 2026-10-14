package com.example.stockelectronico.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stockelectronico.data.sync.ProductoSyncLocalDataSource
import com.example.stockelectronico.data.sync.ProductoSyncManager
import com.example.stockelectronico.data.sync.ProductoSyncState
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.Producto
import com.example.stockelectronico.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventoryUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val categoriaSeleccionada: String? = null,
    val canalSeleccionado: Canal? = null,
    val proveedorSeleccionado: String? = null,
    val categoriasDisponibles: List<String> = emptyList(),
    val proveedoresDisponibles: List<String> = emptyList(),
    val productos: List<Producto> = emptyList(),
    val totalProductos: Int = 0,
    val hasError: Boolean = false,
    val pendingCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncFailed: Boolean = false
)

private data class InventorySource(val productos: List<Producto> = emptyList(), val hasError: Boolean = false)

class InventoryViewModel(
    private val productoRepository: ProductoRepository,
    private val syncManager: ProductoSyncManager? = null,
    private val syncLocal: ProductoSyncLocalDataSource? = null
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val categoriaSeleccionada = MutableStateFlow<String?>(null)
    private val canalSeleccionado = MutableStateFlow<Canal?>(null)
    private val proveedorSeleccionado = MutableStateFlow<String?>(null)

    private val inventoryState: StateFlow<InventoryUiState> = combine(
        productoRepository.observarProductosActivos().map { InventorySource(it) }
            .catch { emit(InventorySource(hasError = true)) },
        query, categoriaSeleccionada, canalSeleccionado, proveedorSeleccionado
    ) { source, texto, categoria, canal, proveedor ->
        InventoryUiState(
            isLoading = false, query = texto, categoriaSeleccionada = categoria,
            canalSeleccionado = canal, proveedorSeleccionado = proveedor,
            categoriasDisponibles = availableCategories(source.productos),
            proveedoresDisponibles = availableSuppliers(source.productos),
            productos = filterInventoryProducts(source.productos, texto, categoria, canal, proveedor),
            totalProductos = source.productos.size, hasError = source.hasError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    val uiState: StateFlow<InventoryUiState> = combine(
        inventoryState,
        syncLocal?.observePendingCount() ?: flowOf(0),
        syncManager?.state ?: flowOf(ProductoSyncState.Idle)
    ) { inventory, pendingCount, syncState ->
        inventory.copy(
            pendingCount = pendingCount,
            isSyncing = syncState is ProductoSyncState.Syncing,
            syncFailed = (syncState as? ProductoSyncState.Completed)?.result?.failed?.let { it > 0 } == true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun onQueryChanged(value: String) = query.update { value }
    fun onCategoriaChanged(value: String?) = categoriaSeleccionada.update { value }
    fun onCanalChanged(value: Canal?) = canalSeleccionado.update { value }
    fun onProveedorChanged(value: String?) = proveedorSeleccionado.update { value }
    fun synchronize() { syncManager?.let { manager -> viewModelScope.launch { manager.synchronize() } } }
}

internal fun filterInventoryProducts(productos: List<Producto>, query: String, categoria: String?, canal: Canal?, proveedor: String?): List<Producto> {
    val normalizedQuery = query.trim()
    return productos.filter { producto ->
        val matchesQuery = normalizedQuery.isBlank() || listOf(producto.nombre, producto.codigo, producto.marca, producto.proveedor, producto.categoria, producto.descripcion)
            .any { it.contains(normalizedQuery, ignoreCase = true) }
        matchesQuery && (categoria == null || producto.categoria == categoria) &&
            (canal == null || producto.canal == canal) && (proveedor == null || producto.proveedor.trim() == proveedor)
    }
}

internal fun availableCategories(productos: List<Producto>): List<String> =
    productos.map { it.categoria.trim() }.filter { it.isNotEmpty() }.distinct().sortedBy { it.lowercase() }

internal fun availableSuppliers(productos: List<Producto>): List<String> =
    productos.map { it.proveedor.trim() }.filter { it.isNotEmpty() }.distinct().sortedBy { it.lowercase() }

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
