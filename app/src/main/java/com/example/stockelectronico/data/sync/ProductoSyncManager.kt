package com.example.stockelectronico.data.sync

import com.example.stockelectronico.data.local.mapper.toDomain
import com.example.stockelectronico.data.remote.ProductoRemoteDataSource
import com.example.stockelectronico.data.remote.ProductoRemoteError
import com.example.stockelectronico.data.remote.ProductoRemoteResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Sube la cola local en orden y sólo confirma Room tras la confirmación remota. */
class ProductoSyncManager(
    private val local: ProductoSyncLocalDataSource,
    private val remote: ProductoRemoteDataSource,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val backoffMillis: (Int) -> Long = { attempt -> attempt * 250L },
    private val wait: suspend (Long) -> Unit = { delay(it) }
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<ProductoSyncState>(ProductoSyncState.Idle)
    val state: StateFlow<ProductoSyncState> = mutableState.asStateFlow()

    suspend fun synchronize(): ProductoSyncResult = mutex.withLock {
        mutableState.value = ProductoSyncState.Syncing
        var uploaded = 0
        var deleted = 0
        var failed = 0

        local.pendingUploads().forEach { entity ->
            when (retry { remote.upsert(entity.toDomain()) }) {
                is ProductoRemoteResult.Success -> {
                    if (local.markSynced(entity.id, entity.updatedAt) == 1) uploaded++
                }
                is ProductoRemoteResult.Failure -> failed++
            }
        }
        local.pendingDeletes().forEach { entity ->
            when (retry { remote.delete(entity.id) }) {
                is ProductoRemoteResult.Success -> {
                    if (local.deleteIfStillPending(entity.id, entity.updatedAt) == 1) deleted++
                }
                is ProductoRemoteResult.Failure -> failed++
            }
        }

        val result = ProductoSyncResult(uploaded, deleted, failed, local.pendingCount())
        mutableState.value = ProductoSyncState.Completed(result)
        result
    }

    private suspend fun retry(operation: suspend () -> ProductoRemoteResult<Unit>): ProductoRemoteResult<Unit> {
        var result = operation()
        var attempt = 1
        while (attempt < maxAttempts && result.shouldRetry()) {
            wait(backoffMillis(attempt))
            attempt++
            result = operation()
        }
        return result
    }

    private fun ProductoRemoteResult<Unit>.shouldRetry(): Boolean =
        this is ProductoRemoteResult.Failure && error is ProductoRemoteError.Unavailable

    companion object { const val DEFAULT_MAX_ATTEMPTS = 3 }
}
