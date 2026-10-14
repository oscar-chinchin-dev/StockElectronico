package com.example.stockelectronico.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.stockelectronico.data.local.dao.ProductoDao
import com.example.stockelectronico.data.local.entity.ProductoEntity

@Database(entities = [ProductoEntity::class], version = 2, exportSchema = true)
@TypeConverters(RoomConverters::class)
abstract class StockElectronicoDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao

    companion object {
        const val DATABASE_NAME = "stock_electronico.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE productos ADD COLUMN proveedor TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
