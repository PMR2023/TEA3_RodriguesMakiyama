package com.example.TEA3_RodriguesMakiyama.retrofit

data class Response(
    val version : Double,
    val success : Boolean,
    val status : Int,
    val hash : String = "",
    val lists : List<ResponseList>? = null,
    val list : ResponseList? = null,
    val items : List<ResponseItem>? = null,
    val item : ResponseItem? = null
)

data class ResponseList (
    val id : Int,
    val label: String)

data class ResponseItem(
    val id : Int,
    val label : String,
    val url : String?,
    val checked : String
) {
    val isChecked = checked == "1"
}