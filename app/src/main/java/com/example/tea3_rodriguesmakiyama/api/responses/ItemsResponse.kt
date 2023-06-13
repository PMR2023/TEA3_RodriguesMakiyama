package com.example.tea3_rodriguesmakiyama.api.responses

data class ItemsResponse(
    val apiname: String,
    val items: List<ItemResponseItem>,
    val status: Int,
    val success: Boolean,
    val version: Double
)

data class ItemResponseItem(
    val checked: String,
    val id: String,
    val label: String
)