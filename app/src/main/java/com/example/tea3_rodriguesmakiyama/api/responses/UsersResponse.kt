package com.example.tea3_rodriguesmakiyama.api.responses

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