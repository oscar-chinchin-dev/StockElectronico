package com.example.stockelectronico.data.local.database

import androidx.room.TypeConverter
import com.example.stockelectronico.data.local.entity.SyncStatus
import com.example.stockelectronico.domain.model.Canal

/** Conserva los enums como sus nombres estables en columnas TEXT. */
class RoomConverters {
    @TypeConverter
    fun canalToStorage(value: Canal): String = value.name

    @TypeConverter
    fun canalFromStorage(value: String): Canal = Canal.valueOf(value)

    @TypeConverter
    fun syncStatusToStorage(value: SyncStatus): String = value.name

    @TypeConverter
    fun syncStatusFromStorage(value: String): SyncStatus = SyncStatus.valueOf(value)
}
