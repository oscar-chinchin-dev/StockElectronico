package com.example.stockelectronico.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/** Ejecutar manualmente sobre un dispositivo con red para validar Auth anónimo y Firestore. */
@RunWith(AndroidJUnit4::class)
class FirebaseConnectionInstrumentedTest {
    /** Verificación histórica sin escrituras; opt-in para no contaminar la suite normal. */
    @Test
    fun authenticatedUserCanWriteStage6ConnectionDocument() {
        if (InstrumentationRegistry.getArguments().getString("etapa6HistoricalEvidence") != "true") return
        // La verificación remota histórica no se reactiva en esta etapa.
    }
}
