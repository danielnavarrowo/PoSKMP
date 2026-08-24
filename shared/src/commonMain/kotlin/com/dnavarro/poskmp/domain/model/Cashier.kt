package com.dnavarro.poskmp.domain.model

data class Cashier(
    val id: String,
    val nombre: String,
    val pin: String = "0000",
    val activo: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)
