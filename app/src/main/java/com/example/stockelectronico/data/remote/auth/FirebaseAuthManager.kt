package com.example.stockelectronico.data.remote.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.tasks.await

/** Obtiene una sesión anónima reutilizable sin exponer detalles de Firebase a la UI. */
class FirebaseAuthManager(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun ensureAuthenticated(): AuthenticationResult {
        firebaseAuth.currentUser?.let { return AuthenticationResult.Authenticated(it.uid) }

        return try {
            val user = firebaseAuth.signInAnonymously().await().user
                ?: return AuthenticationResult.Failed(AuthenticationError.Unknown)
            AuthenticationResult.Authenticated(user.uid)
        } catch (error: FirebaseAuthException) {
            AuthenticationResult.Failed(AuthenticationError.Firebase(error.errorCode))
        } catch (_: Exception) {
            AuthenticationResult.Failed(AuthenticationError.Unknown)
        }
    }
}

sealed interface AuthenticationResult {
    data class Authenticated(val userId: String) : AuthenticationResult
    data class Failed(val error: AuthenticationError) : AuthenticationResult
}

sealed interface AuthenticationError {
    data class Firebase(val code: String) : AuthenticationError
    data object Unknown : AuthenticationError
}
