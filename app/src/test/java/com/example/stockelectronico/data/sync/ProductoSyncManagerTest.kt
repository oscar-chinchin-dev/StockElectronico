package com.example.stockelectronico.data.sync

import com.example.stockelectronico.data.local.entity.ProductoEntity
import com.example.stockelectronico.data.local.entity.SyncStatus
import com.example.stockelectronico.data.remote.ProductoRemoteDataSource
import com.example.stockelectronico.data.remote.ProductoRemoteError
import com.example.stockelectronico.data.remote.ProductoRemoteResult
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.Producto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductoSyncManagerTest {
    @Test fun pendingUploadIsMarkedSyncedOnlyAfterRemoteSuccess() = runBlocking {
        val local = FakeLocal(uploads = mutableListOf(entity("a")))
        val result = manager(local, FakeRemote()).synchronize()
        assertEquals(listOf("a" to 100L), local.synced)
        assertEquals(1, result.uploaded)
    }

    @Test fun permanentFailureRemainsPendingWithoutRetry() = runBlocking {
        val local = FakeLocal(uploads = mutableListOf(entity("a")))
        val remote = FakeRemote(upsertResults = ArrayDeque(listOf(failure(ProductoRemoteError.InvalidProduct("bad")))))
        val result = manager(local, remote).synchronize()
        assertTrue(local.synced.isEmpty())
        assertEquals(1, remote.upsertCalls)
        assertEquals(1, result.failed)
    }

    @Test fun unavailableRetriesAndThenMarksSynced() = runBlocking {
        val local = FakeLocal(uploads = mutableListOf(entity("a")))
        val remote = FakeRemote(upsertResults = ArrayDeque(listOf(failure(ProductoRemoteError.Unavailable), success())))
        val waits = mutableListOf<Long>()
        manager(local, remote, waits).synchronize()
        assertEquals(2, remote.upsertCalls)
        assertEquals(listOf(1L), waits)
        assertEquals(1, local.synced.size)
    }

    @Test fun retriesStopAtThreeAttempts() = runBlocking {
        val remote = FakeRemote(upsertResults = ArrayDeque(List(3) { failure(ProductoRemoteError.Unavailable) }))
        manager(FakeLocal(uploads = mutableListOf(entity("a"))), remote).synchronize()
        assertEquals(3, remote.upsertCalls)
    }

    @Test fun authenticationFailureDoesNotRetry() = runBlocking {
        val remote = FakeRemote(upsertResults = ArrayDeque(listOf(failure(ProductoRemoteError.AuthenticationFailed(
            com.example.stockelectronico.data.remote.auth.AuthenticationError.Unknown
        )))))
        manager(FakeLocal(uploads = mutableListOf(entity("a"))), remote).synchronize()
        assertEquals(1, remote.upsertCalls)
    }

    @Test fun mutexMakesConcurrentSyncCallsProcessOnePendingVersionOnce() = runBlocking {
        val local = MutablePendingLocal(entity("a"))
        val remote = BlockingRemote()
        val manager = ProductoSyncManager(local, remote, wait = {})
        val first = async { manager.synchronize() }
        remote.started.await()
        val second = async { manager.synchronize() }
        yield()
        assertEquals(1, remote.upsertCalls)
        remote.release.complete(Unit)
        first.await()
        second.await()
        assertEquals(1, remote.upsertCalls)
    }

    @Test fun pendingDeleteIsRemovedOnlyAfterRemoteSuccess() = runBlocking {
        val local = FakeLocal(deletes = mutableListOf(entity("a", SyncStatus.PENDING_DELETE)))
        val result = manager(local, FakeRemote()).synchronize()
        assertEquals(listOf("a" to 100L), local.deleted)
        assertEquals(1, result.deleted)
    }

    @Test fun deleteFailureKeepsLocalTombstone() = runBlocking {
        val local = FakeLocal(deletes = mutableListOf(entity("a", SyncStatus.PENDING_DELETE)))
        val remote = FakeRemote(deleteResults = ArrayDeque(listOf(failure(ProductoRemoteError.PermissionDenied))))
        manager(local, remote).synchronize()
        assertTrue(local.deleted.isEmpty())
        assertEquals(1, remote.deleteCalls)
    }

    @Test fun concurrentEditDoesNotMarkNewerVersionSynced() = runBlocking {
        val local = FakeLocal(uploads = mutableListOf(entity("a")), markResult = 0)
        val result = manager(local, FakeRemote()).synchronize()
        assertEquals(0, result.uploaded)
        assertTrue(local.synced.isEmpty())
    }

    @Test fun oneFailureDoesNotStopFollowingUpload() = runBlocking {
        val local = FakeLocal(uploads = mutableListOf(entity("a"), entity("b")))
        val remote = FakeRemote(upsertResults = ArrayDeque(listOf(failure(ProductoRemoteError.PermissionDenied), success())))
        val result = manager(local, remote).synchronize()
        assertEquals(listOf("b" to 100L), local.synced)
        assertEquals(1, result.failed)
        assertEquals(1, result.uploaded)
    }

    private fun manager(local: FakeLocal, remote: FakeRemote, waits: MutableList<Long> = mutableListOf()) =
        ProductoSyncManager(local, remote, backoffMillis = { it.toLong() }, wait = { waits += it })

    private fun entity(id: String, status: SyncStatus = SyncStatus.PENDING) = ProductoEntity(
        id, "Nombre", "SKU", "Categoria", "Marca", "", 1, 1, Canal.AMBOS, 10, 100, status
    )
    private fun success() = ProductoRemoteResult.Success(Unit)
    private fun failure(error: ProductoRemoteError) = ProductoRemoteResult.Failure(error)
}

private class FakeLocal(
    val uploads: MutableList<ProductoEntity> = mutableListOf(),
    val deletes: MutableList<ProductoEntity> = mutableListOf(),
    private val markResult: Int = 1,
    private val deleteResult: Int = 1
) : ProductoSyncLocalDataSource {
    val synced = mutableListOf<Pair<String, Long>>()
    val deleted = mutableListOf<Pair<String, Long>>()
    override suspend fun pendingUploads() = uploads.toList()
    override suspend fun pendingDeletes() = deletes.toList()
    override suspend fun markSynced(id: String, updatedAt: Long): Int = markResult.also { if (it == 1) synced += id to updatedAt }
    override suspend fun deleteIfStillPending(id: String, updatedAt: Long): Int = deleteResult.also { if (it == 1) deleted += id to updatedAt }
    override suspend fun pendingCount(): Int = uploads.size + deletes.size - synced.size - deleted.size
    override fun observePendingCount(): Flow<Int> = MutableStateFlow(0)
}

private class FakeRemote(
    private val upsertResults: ArrayDeque<ProductoRemoteResult<Unit>> = ArrayDeque(),
    private val deleteResults: ArrayDeque<ProductoRemoteResult<Unit>> = ArrayDeque()
) : ProductoRemoteDataSource {
    var upsertCalls = 0
    var deleteCalls = 0
    override suspend fun upsert(producto: Producto): ProductoRemoteResult<Unit> = (++upsertCalls).let { upsertResults.removeFirstOrNull() ?: ProductoRemoteResult.Success(Unit) }
    override suspend fun delete(id: String): ProductoRemoteResult<Unit> = (++deleteCalls).let { deleteResults.removeFirstOrNull() ?: ProductoRemoteResult.Success(Unit) }
    override suspend fun create(producto: Producto) = ProductoRemoteResult.Success(Unit)
    override suspend fun getById(id: String): ProductoRemoteResult<Producto> =
        ProductoRemoteResult.Failure(ProductoRemoteError.NotFound(id))
    override suspend fun getAll(): ProductoRemoteResult<List<Producto>> = ProductoRemoteResult.Success(emptyList())
    override suspend fun update(producto: Producto) = ProductoRemoteResult.Success(Unit)
}

private class MutablePendingLocal(private var pending: ProductoEntity?) : ProductoSyncLocalDataSource {
    override suspend fun pendingUploads() = listOfNotNull(pending)
    override suspend fun pendingDeletes() = emptyList<ProductoEntity>()
    override suspend fun markSynced(id: String, updatedAt: Long): Int =
        if (pending?.id == id && pending?.updatedAt == updatedAt) 1.also { pending = null } else 0
    override suspend fun deleteIfStillPending(id: String, updatedAt: Long) = 0
    override suspend fun pendingCount() = if (pending == null) 0 else 1
    override fun observePendingCount(): Flow<Int> = MutableStateFlow(0)
}

private class BlockingRemote : ProductoRemoteDataSource {
    val started = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    var upsertCalls = 0
    override suspend fun upsert(producto: Producto): ProductoRemoteResult<Unit> {
        upsertCalls++
        started.complete(Unit)
        release.await()
        return ProductoRemoteResult.Success(Unit)
    }
    override suspend fun create(producto: Producto) = ProductoRemoteResult.Success(Unit)
    override suspend fun getById(id: String): ProductoRemoteResult<Producto> = ProductoRemoteResult.Failure(ProductoRemoteError.NotFound(id))
    override suspend fun getAll() = ProductoRemoteResult.Success(emptyList<Producto>())
    override suspend fun update(producto: Producto) = ProductoRemoteResult.Success(Unit)
    override suspend fun delete(id: String) = ProductoRemoteResult.Success(Unit)
}
