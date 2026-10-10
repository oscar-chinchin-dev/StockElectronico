package com.example.stockelectronico.data.sync

import com.example.stockelectronico.data.local.dao.RemoteMergeResult
import com.example.stockelectronico.data.local.entity.ProductoEntity
import com.example.stockelectronico.data.remote.ProductoRemoteChange
import com.example.stockelectronico.data.remote.ProductoRemoteDataSource
import com.example.stockelectronico.data.remote.ProductoRemoteError
import com.example.stockelectronico.data.remote.ProductoRemoteEvent
import com.example.stockelectronico.data.remote.ProductoRemoteListenerRegistration
import com.example.stockelectronico.data.remote.ProductoRemoteResult
import com.example.stockelectronico.data.remote.auth.AuthenticationProvider
import com.example.stockelectronico.data.remote.auth.AuthenticationResult
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.Producto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductoRealtimeSyncManagerTest {
    @Test fun listenerErrorReleasesRegistrationAndLaterStartCreatesOneNewListener() = runBlocking {
        val remote = ListenerRemote(); val manager = manager(remote)
        manager.start(); remote.emit(0, ProductoRemoteEvent.Error(ProductoRemoteError.Unavailable))
        assertTrue(manager.state.value is ProductoRealtimeState.Failed)
        assertEquals(0, remote.activeListeners)
        manager.start()
        assertTrue(manager.state.value is ProductoRealtimeState.Listening)
        assertEquals(2, remote.listenerRegistrations)
        assertEquals(1, remote.activeListeners)
    }

    @Test fun repeatedStartWhileListeningDoesNotDuplicateTheListener() = runBlocking {
        val remote = ListenerRemote(); val manager = manager(remote)
        manager.start(); manager.start()
        assertTrue(manager.state.value is ProductoRealtimeState.Listening)
        assertEquals(1, remote.listenerRegistrations); assertEquals(1, remote.activeListeners)
    }

    @Test fun lateRemovedFromStoppedListenerDoesNotDeleteCurrentRoomProduct() = runBlocking {
        val remote = ListenerRemote(); val local = RecordingLocal(); val manager = manager(remote, local)
        manager.start(); manager.stop(); manager.start()
        local.products["p"] = product("CURRENT", 200)
        remote.emit(0, ProductoRemoteEvent.Changes(listOf(ProductoRemoteChange.Removed("p"))))
        assertEquals(product("CURRENT", 200), local.products["p"])
        assertTrue(local.removals.isEmpty()); assertEquals(1, remote.activeListeners)
        assertTrue(manager.state.value is ProductoRealtimeState.Listening)
    }

    @Test fun lateUpsertFromStoppedListenerDoesNotChangeCurrentRoomProduct() = runBlocking {
        val remote = ListenerRemote(); val local = RecordingLocal(); val manager = manager(remote, local)
        manager.start(); manager.stop(); manager.start()
        local.products["p"] = product("CURRENT", 200)
        remote.emit(0, ProductoRemoteEvent.Changes(listOf(ProductoRemoteChange.Upsert(product("DANGEROUS", 300)))))
        assertEquals(product("CURRENT", 200), local.products["p"])
        assertTrue(local.merges.isEmpty()); assertTrue(manager.state.value is ProductoRealtimeState.Listening)
    }

    @Test fun stopDuringAuthenticationPreventsLateRegistration() = runBlocking {
        val remote = ListenerRemote(); val authentication = CompletableDeferred<AuthenticationResult>()
        val manager = manager(remote, auth = object : AuthenticationProvider {
            override suspend fun ensureAuthenticated() = authentication.await()
        })
        manager.start(); assertTrue(manager.state.value is ProductoRealtimeState.Starting)
        manager.stop(); authentication.complete(AuthenticationResult.Authenticated("test"))
        assertTrue(manager.state.value is ProductoRealtimeState.Idle)
        assertEquals(1, remote.listenerRegistrations); assertEquals(0, remote.activeListeners)
    }

    private fun manager(
        remote: ListenerRemote,
        local: RecordingLocal = RecordingLocal(),
        auth: AuthenticationProvider = object : AuthenticationProvider {
            override suspend fun ensureAuthenticated() = AuthenticationResult.Authenticated("test")
        }
    ) = ProductoRealtimeSyncManager(auth, remote, local, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
}

private class RecordingLocal : ProductoSyncLocalDataSource {
    val products = mutableMapOf<String, Producto>(); val merges = mutableListOf<Producto>(); val removals = mutableListOf<String>()
    override suspend fun pendingUploads() = emptyList<ProductoEntity>()
    override suspend fun pendingDeletes() = emptyList<ProductoEntity>()
    override suspend fun markSynced(id: String, updatedAt: Long) = 0
    override suspend fun deleteIfStillPending(id: String, updatedAt: Long) = 0
    override suspend fun pendingCount() = 0
    override fun observePendingCount(): Flow<Int> = MutableStateFlow(0)
    override suspend fun mergeRemote(producto: Producto): RemoteMergeResult { merges += producto; products[producto.id] = producto; return RemoteMergeResult.APPLIED }
    override suspend fun applyRemoteRemoval(id: String): RemoteMergeResult { removals += id; products.remove(id); return RemoteMergeResult.DELETED }
}

private class ListenerRemote : ProductoRemoteDataSource {
    private val listeners = mutableListOf<(ProductoRemoteEvent) -> Unit>()
    private val active = mutableListOf<Boolean>()
    var listenerRegistrations = 0; var activeListeners = 0
    override fun observeProducts(listener: (ProductoRemoteEvent) -> Unit): ProductoRemoteListenerRegistration {
        val index = listeners.size; listeners += listener; active += true; listenerRegistrations++; activeListeners++
        return ProductoRemoteListenerRegistration { if (active[index]) { active[index] = false; activeListeners-- } }
    }
    /** Deliberately permits a callback after remove(), as Firestore can have one in flight. */
    fun emit(index: Int, event: ProductoRemoteEvent) = listeners[index].invoke(event)
    override suspend fun upsert(producto: Producto) = ProductoRemoteResult.Success(Unit)
    override suspend fun delete(id: String) = ProductoRemoteResult.Success(Unit)
    override suspend fun create(producto: Producto) = ProductoRemoteResult.Success(Unit)
    override suspend fun getById(id: String) = ProductoRemoteResult.Failure(ProductoRemoteError.NotFound(id))
    override suspend fun getAll() = ProductoRemoteResult.Success(emptyList<Producto>())
    override suspend fun update(producto: Producto) = ProductoRemoteResult.Success(Unit)
}

private fun product(nombre: String, updatedAt: Long) = Producto("p", nombre, "SKU", "Categoria", "Marca", "", 1, 1, Canal.AMBOS, 10, updatedAt)
