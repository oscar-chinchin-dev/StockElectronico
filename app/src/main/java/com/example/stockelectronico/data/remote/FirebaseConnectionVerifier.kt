package com.example.stockelectronico.data.remote

import com.example.stockelectronico.data.remote.auth.AuthenticationError
import com.example.stockelectronico.data.remote.auth.AuthenticationResult
import com.example.stockelectronico.data.remote.auth.FirebaseAuthManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

/**
 * Verificación técnica puntual de Etapa 6. No forma parte del flujo productivo ni sincroniza
 * productos locales. Debe invocarse explícitamente desde una prueba instrumentada debug.
 */
class FirebaseConnectionVerifier(
    private val authManager: FirebaseAuthManager,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun verifyConnection(): FirebaseConnectionResult {
        when (val authentication = authManager.ensureAuthenticated()) {
            is AuthenticationResult.Failed -> {
                return FirebaseConnectionResult.AuthenticationFailed(authentication.error)
            }
            is AuthenticationResult.Authenticated -> Unit
        }

        return try {
            firestore.collection(PRODUCTOS_COLLECTION)
                .document(CONNECTION_DOCUMENT_ID)
                .set(
                    mapOf(
                        "tipo" to "conexion",
                        "etapa" to 6,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            FirebaseConnectionResult.Success
        } catch (error: FirebaseFirestoreException) {
            FirebaseConnectionResult.FirestoreFailed(
                code = error.code.name,
                debugMessage = error.message?.take(MAX_DEBUG_MESSAGE_LENGTH)
            )
        } catch (_: Exception) {
            FirebaseConnectionResult.FirestoreFailed("UNKNOWN")
        }
    }

    companion object {
        const val PRODUCTOS_COLLECTION = "productos"
        // Firestore reserva IDs que coinciden con __.*__; este ID debe permanecer temporal.
        const val CONNECTION_DOCUMENT_ID = "conexion_etapa6_temporal"
        private const val MAX_DEBUG_MESSAGE_LENGTH = 500
    }
}

sealed interface FirebaseConnectionResult {
    data object Success : FirebaseConnectionResult
    data class AuthenticationFailed(val error: AuthenticationError) : FirebaseConnectionResult
    /** Detalle acotado para la prueba instrumentada; no se presenta en la UI productiva. */
    data class FirestoreFailed(
        val code: String,
        val debugMessage: String? = null
    ) : FirebaseConnectionResult
}
