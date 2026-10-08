package com.example.stockelectronico.data.local.entity

/** Estado técnico local; nunca forma parte del modelo remoto ni del dominio. */
enum class SyncStatus {
    SYNCED,
    PENDING,
    PENDING_DELETE
}
