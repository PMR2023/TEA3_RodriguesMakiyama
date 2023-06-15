package com.example.tea3_rodriguesmakiyama.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ListEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0L,
    val idAPI: Int?,
    val label : String,
    val fromPseudo : String
) {
}