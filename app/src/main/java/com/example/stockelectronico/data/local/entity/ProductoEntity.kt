package com.example.stockelectronico.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.stockelectronico.domain.model.Canal

@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val codigo: String,
    val categoria: String,
    val marca: String,
    val descripcion: String,
    val precio: Long,
    val stock: Int,
    val canal: Canal,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus
)
