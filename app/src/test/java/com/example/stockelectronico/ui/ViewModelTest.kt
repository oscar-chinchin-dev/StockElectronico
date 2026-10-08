package com.example.stockelectronico.ui

import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.Producto
import com.example.stockelectronico.domain.repository.ProductoRepository
import com.example.stockelectronico.ui.detail.ProductDetailViewModel
import com.example.stockelectronico.ui.inventory.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `empty query observes full inventory`() = runTest {
        val repository = FakeProductoRepository(active = listOf(producto("1", "Teclado", "TEC-1")))
        val viewModel = InventoryViewModel(repository)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(listOf(""), repository.searches)
        assertEquals("Teclado", state.productos.single().nombre)
    }

    @Test
    fun `query uses repository search and updates state`() = runTest {
        val repository = FakeProductoRepository(active = listOf(producto("1", "Teclado", "TEC-1")))
        val viewModel = InventoryViewModel(repository)
        viewModel.uiState.first { !it.isLoading }

        viewModel.onQueryChanged("TEC")
        val state = viewModel.uiState.first { it.query == "TEC" && !it.isLoading }

        assertEquals(listOf("", "TEC"), repository.searches)
        assertEquals("TEC", state.query)
        assertEquals("TEC-1", state.productos.single().codigo)
    }

    @Test
    fun `detail exposes existing product`() = runTest {
        val repository = FakeProductoRepository(active = listOf(producto("1", "Mouse", "MOU-1")))
        val viewModel = ProductDetailViewModel("1", repository)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals("Mouse", state.producto?.nombre)
        assertFalse(state.hasError)
    }

    @Test
    fun `detail exposes missing product without crashing`() = runTest {
        val viewModel = ProductDetailViewModel("missing", FakeProductoRepository())

        val state = viewModel.uiState.first { !it.isLoading }

        assertNull(state.producto)
        assertFalse(state.hasError)
    }

    @Test
    fun `inventory exposes controlled error when repository fails`() = runTest {
        val viewModel = InventoryViewModel(FakeProductoRepository(inventoryError = true))

        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.hasError)
        assertTrue(state.productos.isEmpty())
    }

    @Test
    fun `detail exposes controlled error when repository fails`() = runTest {
        val viewModel = ProductDetailViewModel("1", FakeProductoRepository(detailError = true))

        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.hasError)
        assertNull(state.producto)
    }
}

private class FakeProductoRepository(
    active: List<Producto> = emptyList(),
    private val inventoryError: Boolean = false,
    private val detailError: Boolean = false
) : ProductoRepository {
    private val products = MutableStateFlow(active)
    val searches = mutableListOf<String>()

    override fun observarProductosActivos(): Flow<List<Producto>> {
        searches += ""
        if (inventoryError) return flow { throw IllegalStateException("Fallo técnico de inventario") }
        return products
    }

    override fun buscarProductos(texto: String): Flow<List<Producto>> {
        searches += texto
        return flowOf(products.value.filter {
            it.nombre.contains(texto, ignoreCase = true) || it.codigo.contains(texto, ignoreCase = true)
        })
    }

    override suspend fun obtenerProductoActivoPorId(id: String): Producto? = products.value.find { it.id == id }
    override fun observarProductoActivoPorId(id: String): Flow<Producto?> {
        if (detailError) return flow { throw IllegalStateException("Fallo técnico de detalle") }
        return flowOf(products.value.find { it.id == id })
    }
    override suspend fun crearProducto(producto: Producto): Producto = producto
    override suspend fun actualizarProducto(producto: Producto): Producto = producto
    override suspend fun marcarEliminacionPendiente(id: String) = Unit
    override suspend fun eliminarFisicamentePorId(id: String) = Unit
    override suspend fun obtenerPendientesDeSincronizacion(): List<Producto> = emptyList()
}

private fun producto(id: String, nombre: String, codigo: String) = Producto(
    id = id,
    nombre = nombre,
    codigo = codigo,
    categoria = "Periféricos",
    marca = "Ejemplo",
    descripcion = "",
    precio = 12_990,
    stock = 5,
    canal = Canal.AMBOS,
    createdAt = 0,
    updatedAt = 0
)
