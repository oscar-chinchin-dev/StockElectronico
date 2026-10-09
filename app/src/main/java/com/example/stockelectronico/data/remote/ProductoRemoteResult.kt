package com.example.stockelectronico.data.remote

import com.example.stockelectronico.data.remote.auth.AuthenticationError

sealed interface ProductoRemoteResult<out T> {
    data class Success<T>(val value: T) : ProductoRemoteResult<T>
    data class Failure(val error: ProductoRemoteError) : ProductoRemoteResult<Nothing>
}

sealed interface ProductoRemoteError {
    data class AuthenticationFailed(val cause: AuthenticationError) : ProductoRemoteError
    data class InvalidProduct(val reason: String) : ProductoRemoteError
    data class InvalidRemoteDocument(val documentId: String, val reason: String) : ProductoRemoteError
    data class AlreadyExists(val documentId: String) : ProductoRemoteError
    data class NotFound(val documentId: String) : ProductoRemoteError
    data object PermissionDenied : ProductoRemoteError
    data object Unavailable : ProductoRemoteError
    data class Firestore(val code: String) : ProductoRemoteError
}
