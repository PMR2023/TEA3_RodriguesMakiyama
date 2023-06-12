package com.example.TEA3_RodriguesMakiyama.api.responses

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