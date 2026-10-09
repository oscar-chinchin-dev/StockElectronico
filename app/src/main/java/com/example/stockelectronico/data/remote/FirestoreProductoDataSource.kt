package com.example.stockelectronico.data.remote

import com.example.stockelectronico.data.remote.auth.AuthenticationResult
import com.example.stockelectronico.data.remote.auth.FirebaseAuthManager
import com.example.stockelectronico.data.remote.mapper.ProductoRemoteMapper
import com.example.stockelectronico.domain.model.Producto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/** Implementación Firestore explícita; sus operaciones no tienen efectos sobre Room. */
class FirestoreProductoDataSource(
    private val authManager: FirebaseAuthManager,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProductoRemoteDataSource {
    override suspend fun create(producto: Producto): ProductoRemoteResult<Unit> {
        val remote = when (val mapped = ProductoRemoteMapper.toRemote(producto)) {
            is ProductoRemoteResult.Success -> mapped.value
            is ProductoRemoteResult.Failure -> return mapped
        }
        authenticate()?.let { return it }
        return try {
            firestore.runTransaction { transaction ->
                val reference = firestore.collection(PRODUCTOS_COLLECTION).document(producto.id)
                if (transaction.get(reference).exists()) throw AlreadyExistsException(producto.id)
                transaction.set(reference, remote.toFirestoreMap())
            }.await()
            ProductoRemoteResult.Success(Unit)
        } catch (error: AlreadyExistsException) {
            ProductoRemoteResult.Failure(ProductoRemoteError.AlreadyExists(error.documentId))
        } catch (error: Exception) { error.toFailure() }
    }

    override suspend fun getById(id: String): ProductoRemoteResult<Producto> {
        validateId(id)?.let { return it }
        authenticate()?.let { return it }
        return try {
            // Esta capa representa lecturas remotas explícitas, no el caché local de Firestore.
            val snapshot = firestore.collection(PRODUCTOS_COLLECTION).document(id).get(Source.SERVER).await()
            if (!snapshot.exists()) ProductoRemoteResult.Failure(ProductoRemoteError.NotFound(id))
            else ProductoRemoteMapper.toProducto(snapshot.id, snapshot.data.orEmpty())
        } catch (error: Exception) { error.toFailure() }
    }

    override suspend fun getAll(): ProductoRemoteResult<List<Producto>> {
        authenticate()?.let { return it }
        return try {
            val products = mutableListOf<Producto>()
            for (document in firestore.collection(PRODUCTOS_COLLECTION).get(Source.SERVER).await().documents) {
                when (val mapped = ProductoRemoteMapper.toProducto(document.id, document.data.orEmpty())) {
                    is ProductoRemoteResult.Success -> products += mapped.value
                    is ProductoRemoteResult.Failure -> return mapped
                }
            }
            ProductoRemoteResult.Success(products)
        } catch (error: Exception) { error.toFailure() }
    }

    override suspend fun update(producto: Producto): ProductoRemoteResult<Unit> {
        val remote = when (val mapped = ProductoRemoteMapper.toRemote(producto)) {
            is ProductoRemoteResult.Success -> mapped.value
            is ProductoRemoteResult.Failure -> return mapped
        }
        authenticate()?.let { return it }
        return try {
            firestore.runTransaction { transaction ->
                val reference = firestore.collection(PRODUCTOS_COLLECTION).document(producto.id)
                if (!transaction.get(reference).exists()) throw NotFoundException(producto.id)
                // set sin merge reemplaza cualquier campo residual ajeno al contrato remoto.
                transaction.set(reference, remote.toFirestoreMap())
            }.await()
            ProductoRemoteResult.Success(Unit)
        } catch (error: NotFoundException) {
            ProductoRemoteResult.Failure(ProductoRemoteError.NotFound(error.documentId))
        } catch (error: Exception) { error.toFailure() }
    }

    /** Firestore delete es idempotente: borrar un ID inexistente se considera exitoso. */
    override suspend fun delete(id: String): ProductoRemoteResult<Unit> {
        validateId(id)?.let { return it }
        authenticate()?.let { return it }
        return try {
            firestore.collection(PRODUCTOS_COLLECTION).document(id).delete().await()
            ProductoRemoteResult.Success(Unit)
        } catch (error: Exception) { error.toFailure() }
    }

    private suspend fun authenticate(): ProductoRemoteResult.Failure? = when (val result = authManager.ensureAuthenticated()) {
        is AuthenticationResult.Authenticated -> null
        is AuthenticationResult.Failed -> ProductoRemoteResult.Failure(ProductoRemoteError.AuthenticationFailed(result.error))
    }

    private fun validateId(id: String): ProductoRemoteResult.Failure? = id.takeIf { it.isBlank() }?.let {
        ProductoRemoteResult.Failure(ProductoRemoteError.InvalidProduct("id es obligatorio."))
    }

    private fun Exception.toFailure(): ProductoRemoteResult.Failure = when (this) {
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> ProductoRemoteResult.Failure(ProductoRemoteError.PermissionDenied)
            FirebaseFirestoreException.Code.UNAVAILABLE -> ProductoRemoteResult.Failure(ProductoRemoteError.Unavailable)
            else -> ProductoRemoteResult.Failure(ProductoRemoteError.Firestore(code.name))
        }
        else -> ProductoRemoteResult.Failure(ProductoRemoteError.Firestore("UNKNOWN"))
    }

    private class AlreadyExistsException(val documentId: String) : RuntimeException()
    private class NotFoundException(val documentId: String) : RuntimeException()

    companion object { const val PRODUCTOS_COLLECTION = "productos" }
}
