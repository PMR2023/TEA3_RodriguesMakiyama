package com.example.TEA3_RodriguesMakiyama.api.responses

data class UsersResponse(
    val apiname: String,
    val status: Int,
    val success: Boolean,
    val users: List<UsersResponseItem>,
    val version: Double
)

data class UsersResponseItem(
    val id: String,
    val pseudo: String
)