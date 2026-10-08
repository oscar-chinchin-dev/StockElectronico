package com.example.stockelectronico.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.Producto
import com.example.stockelectronico.domain.repository.ProductoRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProductFormUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val productNotFound: Boolean = false,
    val saveSucceeded: Boolean = false,
    val persistenceError: Boolean = false,
    val nombre: String = "",
    val codigo: String = "",
    val categoria: String = "",
    val marca: String = "",
    val descripcion: String = "",
    val precio: String = "",
    val stock: String = "",
    val canal: Canal = Canal.SUCURSAL,
    val nombreError: Int? = null,
    val codigoError: Int? = null,
    val categoriaError: Int? = null,
    val marcaError: Int? = null,
    val precioError: Int? = null,
    val stockError: Int? = null
)

class ProductFormViewModel(
    private val productId: String?,
    private val productoRepository: ProductoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ProductFormUiState(isEditMode = productId != null, isLoading = productId != null)
    )
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    init {
        if (productId != null) loadProduct(productId)
    }

    fun onNombreChanged(value: String) = update { copy(nombre = value, nombreError = null) }
    fun onCodigoChanged(value: String) = update { copy(codigo = value, codigoError = null) }
    fun onCategoriaChanged(value: String) = update { copy(categoria = value, categoriaError = null) }
    fun onMarcaChanged(value: String) = update { copy(marca = value, marcaError = null) }
    fun onDescripcionChanged(value: String) = update { copy(descripcion = value) }
    fun onPrecioChanged(value: String) = update { copy(precio = value, precioError = null) }
    fun onStockChanged(value: String) = update { copy(stock = value, stockError = null) }
    fun onCanalChanged(value: Canal) = update { copy(canal = value) }

    fun save() {
        val current = _uiState.value
        if (current.isSaving || current.isLoading || current.productNotFound) return
        val errors = validate(current)
        if (errors != null) {
            _uiState.value = errors
            return
        }
        val precio = current.precio.trim().toLong()
        val stock = current.stock.trim().toInt()
        _uiState.update { it.copy(isSaving = true, persistenceError = false) }
        viewModelScope.launch {
            runCatching {
                val product = Producto(
                    id = productId.orEmpty(),
                    nombre = current.nombre.trim(),
                    codigo = current.codigo.trim(),
                    categoria = current.categoria.trim(),
                    marca = current.marca.trim(),
                    descripcion = current.descripcion.trim(),
                    precio = precio,
                    stock = stock,
                    canal = current.canal,
                    createdAt = 0L,
                    updatedAt = 0L
                )
                if (productId == null) productoRepository.crearProducto(product)
                else productoRepository.actualizarProducto(product)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSucceeded = true) }
            }.onFailure {
                _uiState.update { it.copy(isSaving = false, persistenceError = true) }
            }
        }
    }

    fun consumeSaveSucceeded() = update { copy(saveSucceeded = false) }

    private fun loadProduct(id: String) {
        viewModelScope.launch {
            runCatching { productoRepository.obtenerProductoActivoPorId(id) }
                .onSuccess { product ->
                    _uiState.update { state ->
                        if (product == null) state.copy(isLoading = false, productNotFound = true)
                        else state.copy(
                            isLoading = false,
                            nombre = product.nombre,
                            codigo = product.codigo,
                            categoria = product.categoria,
                            marca = product.marca,
                            descripcion = product.descripcion,
                            precio = product.precio.toString(),
                            stock = product.stock.toString(),
                            canal = product.canal
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false, persistenceError = true) } }
        }
    }

    private fun validate(state: ProductFormUiState): ProductFormUiState? {
        val precio = state.precio.trim().toLongOrNull()
        val stock = state.stock.trim().toIntOrNull()
        val result = state.copy(
            nombreError = if (state.nombre.isBlank()) com.example.stockelectronico.R.string.form_error_name_required else null,
            codigoError = if (state.codigo.isBlank()) com.example.stockelectronico.R.string.form_error_code_required else null,
            categoriaError = if (state.categoria.isBlank()) com.example.stockelectronico.R.string.form_error_category_required else null,
            marcaError = if (state.marca.isBlank()) com.example.stockelectronico.R.string.form_error_brand_required else null,
            precioError = when { precio == null -> com.example.stockelectronico.R.string.form_error_price_invalid; precio < 0 -> com.example.stockelectronico.R.string.form_error_price_negative; else -> null },
            stockError = when { stock == null -> com.example.stockelectronico.R.string.form_error_stock_invalid; stock < 0 -> com.example.stockelectronico.R.string.form_error_stock_negative; else -> null }
        )
        return result.takeIf { it.nombreError != null || it.codigoError != null || it.categoriaError != null || it.marcaError != null || it.precioError != null || it.stockError != null }
    }

    private fun update(transform: ProductFormUiState.() -> ProductFormUiState) = _uiState.update(transform)
}

class ProductFormViewModelFactory(
    private val productId: String?,
    private val productoRepository: ProductoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProductFormViewModel::class.java))
        return ProductFormViewModel(productId, productoRepository) as T
    }
}
