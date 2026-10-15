package com.example.stockelectronico.ui.external

import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.ExternalCatalogPage
import com.example.stockelectronico.domain.model.ExternalCatalogProduct
import com.example.stockelectronico.domain.model.Producto
import com.example.stockelectronico.domain.repository.ExternalCatalogRepository
import com.example.stockelectronico.domain.repository.ExternalCatalogResult
import com.example.stockelectronico.domain.repository.ProductoRepository
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExternalCatalogViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `loads catalog and retries after error`() = runTest {
        val catalog = FakeCatalog(listOf(failure(), success()))
        val vm = ExternalCatalogViewModel(catalog, FakeProducts())
        assertTrue(vm.catalogState.first { it is ExternalCatalogUiState.Error } is ExternalCatalogUiState.Error)
        vm.loadCatalog()
        val state = vm.catalogState.first { it is ExternalCatalogUiState.Success } as ExternalCatalogUiState.Success
        assertEquals(1, state.products.size)
        assertEquals(2, catalog.calls)
    }

    @Test fun `publishes Loading while catalog request is pending then Success with products`() = runTest {
        val response = CompletableDeferred<ExternalCatalogResult<ExternalCatalogPage>>()
        val catalog = SuspendingCatalog(response)
        val vm = ExternalCatalogViewModel(catalog, FakeProducts())

        assertEquals(ExternalCatalogUiState.Loading, vm.catalogState.value)
        response.complete(success())

        val state = vm.catalogState.first { it is ExternalCatalogUiState.Success } as ExternalCatalogUiState.Success
        assertEquals(listOf(external()), state.products)
    }

    @Test fun `blank supplier is blocked and valid import uses repository once`() = runTest {
        val products = FakeProducts()
        val vm = ExternalCatalogViewModel(FakeCatalog(listOf(success())), products, ExternalCatalogImportMapper(now = { 1L }, newId = { "uuid" }))
        vm.beginImport(external())
        vm.confirmImport()
        assertTrue(vm.importState.value.supplierInvalid)
        assertTrue(products.created.isEmpty())
        vm.onSupplierChanged(" Tech ")
        vm.onChannelChanged(Canal.SUCURSAL)
        vm.confirmImport()
        vm.importState.first { it.succeeded }
        assertEquals(1, products.created.size)
        assertEquals("Tech", products.created.single().proveedor)
        assertEquals(Canal.SUCURSAL, products.created.single().canal)
    }

    @Test fun `simultaneous confirms only create one product`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val products = FakeProducts(gate)
        val vm = ExternalCatalogViewModel(FakeCatalog(listOf(success())), products)
        vm.beginImport(external()); vm.onSupplierChanged("Tech")
        vm.confirmImport(); vm.confirmImport()
        assertEquals(1, products.created.size)
        gate.complete(Unit)
        vm.importState.first { it.succeeded }
        assertEquals(1, products.created.size)
    }

    private fun external() = ExternalCatalogProduct(4, "Mouse", "Desc", "cat", 10.0, 2, "Brand", "SKU", null, emptyList())
    private fun success() = ExternalCatalogResult.Success(ExternalCatalogPage(listOf(external()), 1, 0, 20))
    private fun failure() = ExternalCatalogResult.Failure(com.example.stockelectronico.domain.repository.ExternalCatalogError.Network(IOException()))
}

private class FakeCatalog(private val replies: List<ExternalCatalogResult<ExternalCatalogPage>>) : ExternalCatalogRepository {
    var calls = 0
    override suspend fun getProducts(limit: Int, skip: Int): ExternalCatalogResult<ExternalCatalogPage> = replies[calls++]
    override suspend fun getProduct(id: Int): ExternalCatalogResult<ExternalCatalogProduct> = error("unused")
}

private class SuspendingCatalog(
    private val response: CompletableDeferred<ExternalCatalogResult<ExternalCatalogPage>>
) : ExternalCatalogRepository {
    override suspend fun getProducts(limit: Int, skip: Int): ExternalCatalogResult<ExternalCatalogPage> = response.await()
    override suspend fun getProduct(id: Int): ExternalCatalogResult<ExternalCatalogProduct> = error("unused")
}

private class FakeProducts(private val gate: CompletableDeferred<Unit>? = null) : ProductoRepository {
    val created = mutableListOf<Producto>()
    override fun observarProductosActivos(): Flow<List<Producto>> = emptyFlow()
    override fun buscarProductos(texto: String): Flow<List<Producto>> = emptyFlow()
    override suspend fun obtenerProductoActivoPorId(id: String): Producto? = null
    override fun observarProductoActivoPorId(id: String): Flow<Producto?> = emptyFlow()
    override suspend fun crearProducto(producto: Producto): Producto { created += producto; gate?.await(); return producto }
    override suspend fun actualizarProducto(producto: Producto): Producto = producto
    override suspend fun marcarEliminacionPendiente(id: String) = Unit
    override suspend fun eliminarFisicamentePorId(id: String) = Unit
    override suspend fun obtenerPendientesDeSincronizacion(): List<Producto> = emptyList()
}
