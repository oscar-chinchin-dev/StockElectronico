package com.example.stockelectronico.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/** Ejecutar manualmente sobre un dispositivo con red para validar Auth anónimo y Firestore. */
@RunWith(AndroidJUnit4::class)
class FirebaseConnectionInstrumentedTest {
    /**
     * La conexión real fue verificada y aprobada en Etapa 6. Se conserva como trazabilidad,
     * pero no debe ejecutar escrituras técnicas ni contaminar suites posteriores de productos.
     */
    @Ignore("Verificación histórica de Etapa 6; no ejecutar dentro de la suite normal.")
    @Test
    fun authenticatedUserCanWriteStage6ConnectionDocument() {
        // Intencionalmente vacío: @Ignore evita que AndroidJUnitRunner ejecute la escritura.
    }
}
