package com.example.stockelectronico.data.remote

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.stockelectronico.StockElectronicoApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Ejecutar manualmente sobre un dispositivo con red para validar Auth anónimo y Firestore. */
@RunWith(AndroidJUnit4::class)
class FirebaseConnectionInstrumentedTest {
    @Test
    fun authenticatedUserCanWriteStage6ConnectionDocument() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<StockElectronicoApplication>()

        val result = application.appContainer.firebaseConnectionVerifier.verifyConnection()

        assertEquals(FirebaseConnectionResult.Success, result)
    }
}
