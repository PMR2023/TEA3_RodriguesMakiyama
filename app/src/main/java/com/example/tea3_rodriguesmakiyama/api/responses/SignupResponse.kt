package com.example.tea3_rodriguesmakiyama.api.responses

import com.example.tea3_rodriguesmakiyama.api.responses.UsersResponseItem

data class SignupResponse(
    val apiname: String,
    val status: Int,
    val success: Boolean,
    val user: UsersResponseItem?,
    val version: Double
)