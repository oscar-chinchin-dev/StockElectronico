package com.example.stockelectronico.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.stockelectronico.data.local.dao.ProductoDao
import com.example.stockelectronico.data.local.entity.ProductoEntity

@Database(entities = [ProductoEntity::class], version = 1, exportSchema = true)
@TypeConverters(RoomConverters::class)
abstract class StockElectronicoDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao

    companion object {
        const val DATABASE_NAME = "stock_electronico.db"
    }
}
