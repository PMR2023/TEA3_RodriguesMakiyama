package com.example.TEA3_RodriguesMakiyama.api.responses

data class SignupResponse(
    val apiname: String,
    val status: Int,
    val success: Boolean,
    val user: UsersResponseItem?,
    val version: Double
)