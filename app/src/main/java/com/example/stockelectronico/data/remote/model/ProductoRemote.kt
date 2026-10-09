package com.example.stockelectronico.data.remote.model

/** Campos persistidos en Firestore; el identificador vive exclusivamente en el document ID. */
data class ProductoRemote(
    val nombre: String,
    val codigo: String,
    val categoria: String,
    val marca: String,
    val descripcion: String,
    val precio: Long,
    val stock: Int,
    val canal: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        FIELD_NOMBRE to nombre,
        FIELD_CODIGO to codigo,
        FIELD_CATEGORIA to categoria,
        FIELD_MARCA to marca,
        FIELD_DESCRIPCION to descripcion,
        FIELD_PRECIO to precio,
        FIELD_STOCK to stock,
        FIELD_CANAL to canal,
        FIELD_CREATED_AT to createdAt,
        FIELD_UPDATED_AT to updatedAt
    )

    companion object {
        const val FIELD_NOMBRE = "nombre"
        const val FIELD_CODIGO = "codigo"
        const val FIELD_CATEGORIA = "categoria"
        const val FIELD_MARCA = "marca"
        const val FIELD_DESCRIPCION = "descripcion"
        const val FIELD_PRECIO = "precio"
        const val FIELD_STOCK = "stock"
        const val FIELD_CANAL = "canal"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}
