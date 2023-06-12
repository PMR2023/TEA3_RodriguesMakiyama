package com.example.TEA3_RodriguesMakiyama.api.responses

data class HashResponse(
    val apiname: String,
    val hash: String,
    val status: Int,
    val success: Boolean,
    val version: Double
)