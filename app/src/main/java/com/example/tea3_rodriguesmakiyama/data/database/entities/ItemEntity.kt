package com.example.tea3_rodriguesmakiyama.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0L,
    val idAPI: Int?,
    val label : String,
    var url : String?,
    var checked : Boolean,
    val fromList : String
) {
    fun isChecked(): Int = if(checked) {
        1
    } else{
        0
    }
}
