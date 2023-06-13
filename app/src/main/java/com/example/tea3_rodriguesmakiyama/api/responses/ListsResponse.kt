package com.example.tea3_rodriguesmakiyama.api.responses

data class ListsResponse(
    val apiname: String,
    val lists: List<ListResponseItem>,
    val status: Int,
    val success: Boolean,
    val version: Double
)

data class ListResponseItem(
    val id: String,
    val label: String
)