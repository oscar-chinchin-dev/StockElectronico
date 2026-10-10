package com.example.stockelectronico.data.sync

import com.example.stockelectronico.data.remote.ProductoRemoteChange
import com.example.stockelectronico.data.remote.ProductoRemoteDataSource
import com.example.stockelectronico.data.remote.ProductoRemoteError
import com.example.stockelectronico.data.remote.ProductoRemoteEvent
import com.example.stockelectronico.data.remote.ProductoRemoteListenerRegistration
import com.example.stockelectronico.data.remote.auth.AuthenticationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ProductoRealtimeState {
    data object Idle : ProductoRealtimeState
    data object Starting : ProductoRealtimeState
    data object Listening : ProductoRealtimeState
    data class Failed(val error: ProductoRemoteError) : ProductoRealtimeState
}

/** One app-scoped subscription: Firebase -> ordered Room merge -> existing UI Flows. */
class ProductoRealtimeSyncManager(
    private val authManager: AuthenticationProvider,
    private val remote: ProductoRemoteDataSource,
    private val local: ProductoSyncLocalDataSource,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private var registration: ProductoRemoteListenerRegistration? = null
    private val mutableState = MutableStateFlow<ProductoRealtimeState>(ProductoRealtimeState.Idle)
    val state: StateFlow<ProductoRealtimeState> = mutableState.asStateFlow()

    private var generation = 0L

    fun start() {
        scope.launch {
            val requestedGeneration = mutex.withLock {
                if (registration != null || mutableState.value is ProductoRealtimeState.Starting ||
                    mutableState.value is ProductoRealtimeState.Listening
                ) return@launch
                mutableState.value = ProductoRealtimeState.Starting
                ++generation
            }
            when (val auth = authManager.ensureAuthenticated()) {
                is com.example.stockelectronico.data.remote.auth.AuthenticationResult.Failed -> mutex.withLock {
                    if (requestedGeneration == generation) {
                        mutableState.value = ProductoRealtimeState.Failed(ProductoRemoteError.AuthenticationFailed(auth.error))
                    }
                }
                is com.example.stockelectronico.data.remote.auth.AuthenticationResult.Authenticated -> {
                    val newRegistration = remote.observeProducts { event -> onEvent(requestedGeneration, event) }
                    mutex.withLock {
                        if (requestedGeneration == generation && mutableState.value is ProductoRealtimeState.Starting) {
                            registration = newRegistration
                            mutableState.value = ProductoRealtimeState.Listening
                        } else {
                            newRegistration.remove()
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        // stop is a lifecycle boundary: it does not return while a callback is
        // still inside the same mutex applying a Room batch.
        runBlocking {
            mutex.withLock {
                ++generation // Invalidates an authentication/listener registration still in flight.
                registration?.remove()
                registration = null
                mutableState.value = ProductoRealtimeState.Idle
            }
        }
    }

    private fun onEvent(listenerGeneration: Long, event: ProductoRemoteEvent) {
        when (event) {
            is ProductoRemoteEvent.Error -> scope.launch {
                mutex.withLock {
                    if (listenerGeneration != generation) return@withLock
                    registration?.remove()
                    registration = null
                    mutableState.value = ProductoRealtimeState.Failed(event.error)
                }
            }
            is ProductoRemoteEvent.Changes -> scope.launch {
                // The lifecycle mutex forms the stop boundary: validation and the
                // Room writes are one logical critical section for this generation.
                mutex.withLock {
                    if (listenerGeneration != generation) return@withLock
                    event.changes.forEach { change ->
                        when (change) {
                            is ProductoRemoteChange.Upsert -> local.mergeRemote(change.producto)
                            is ProductoRemoteChange.Removed -> local.applyRemoteRemoval(change.id)
                        }
                    }
                }
            }
        }
    }
}
