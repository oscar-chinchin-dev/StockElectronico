package com.example.stockelectronico.data.local.mapper

import com.example.stockelectronico.data.local.entity.ProductoEntity
import com.example.stockelectronico.data.local.entity.SyncStatus
import com.example.stockelectronico.domain.model.Producto

fun Producto.toEntity(syncStatus: SyncStatus): ProductoEntity = ProductoEntity(
    id = id,
    nombre = nombre,
    codigo = codigo,
    categoria = categoria,
    marca = marca,
    descripcion = descripcion,
    precio = precio,
    stock = stock,
    canal = canal,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

fun ProductoEntity.toDomain(): Producto = Producto(
    id = id,
    nombre = nombre,
    codigo = codigo,
    categoria = categoria,
    marca = marca,
    descripcion = descripcion,
    precio = precio,
    stock = stock,
    canal = canal,
    createdAt = createdAt,
    updatedAt = updatedAt
)
