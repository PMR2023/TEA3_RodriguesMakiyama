package com.example.tea3_rodriguesmakiyama.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tea3_rodriguesmakiyama.data.database.entities.ItemEntity
import com.example.tea3_rodriguesmakiyama.data.database.entities.ListEntity
import com.example.tea3_rodriguesmakiyama.data.database.entities.PseudoEntity

@Dao
interface ToDoDAO {
    // Add functions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPseudo(pseudo : PseudoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addList(list : ListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItem(item : ItemEntity)

    // Get functions
    @Query("SELECT * FROM PseudoEntity WHERE pseudo == :pseudo")
    suspend fun getPseudo(pseudo: String) : PseudoEntity?

    @Query("SELECT * FROM PseudoEntity WHERE isLast == 1")
    suspend fun getLastPseudo() : PseudoEntity

    @Query("SELECT * FROM PseudoEntity ORDER BY id DESC")
    suspend fun getPseudosHistory() : List<PseudoEntity>

    @Query("SELECT * FROM ListEntity WHERE fromPseudo == :pseudo ORDER BY id ASC")
    suspend fun getListsFrom(pseudo: String) : List<ListEntity>

    @Query("SELECT * FROM ListEntity WHERE idAPI == null ORDER BY id ASC")
    suspend fun getOfflineLists() : List<ListEntity>

    @Query("SELECT * FROM ItemEntity WHERE fromList == :listLabel ORDER BY id ASC")
    suspend fun getItemsFromList(listLabel : String) : List<ItemEntity>

    // Delete functions
    @Query("DELETE FROM PseudoEntity")
    suspend fun clearPseudos()

    @Query("DELETE FROM ListEntity")
    suspend fun clearLists()

    @Query("DELETE FROM ListEntity WHERE label == :label")
    suspend fun deleteList(label: String)

    @Query("DELETE FROM ItemEntity")
    suspend fun clearItems()

    @Query("DELETE FROM ItemEntity WHERE label == :label")
    suspend fun deleteItem(label: String)
}