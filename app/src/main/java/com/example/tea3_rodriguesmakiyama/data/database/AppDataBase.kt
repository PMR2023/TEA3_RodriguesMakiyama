package com.example.tea3_rodriguesmakiyama.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tea3_rodriguesmakiyama.data.database.entities.ItemEntity
import com.example.tea3_rodriguesmakiyama.data.database.entities.ListEntity
import com.example.tea3_rodriguesmakiyama.data.database.entities.PseudoEntity

@Database(entities = [PseudoEntity::class, ListEntity::class, ItemEntity::class], version = 1)
abstract class AppDataBase : RoomDatabase() {
    abstract val toDoDao : ToDoDAO
}

private lateinit var INSTANCE: AppDataBase

fun getDatabase(context: Context): AppDataBase {
    synchronized(AppDataBase::class.java) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room.databaseBuilder(context.applicationContext,
                AppDataBase::class.java,
                "appdatabase").build()
        }
    }
    return INSTANCE
}