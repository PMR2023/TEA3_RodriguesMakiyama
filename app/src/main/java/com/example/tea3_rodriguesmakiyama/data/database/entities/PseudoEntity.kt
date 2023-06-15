package com.example.tea3_rodriguesmakiyama.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PseudoEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0L,
    val pseudo : String,
    var hash : String? = null,
    var password: String?,
    var isLast : Boolean
)
