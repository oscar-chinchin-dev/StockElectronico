package com.example.stockelectronico.domain.model

/** Producto compartido por las capas de dominio y presentación. */
data class Producto(
    val id: String,
    val nombre: String,
    val codigo: String,
    val categoria: String,
    val marca: String,
    val descripcion: String,
    val precio: Long,
    val stock: Int,
    val canal: Canal,
    val createdAt: Long,
    val updatedAt: Long
)
