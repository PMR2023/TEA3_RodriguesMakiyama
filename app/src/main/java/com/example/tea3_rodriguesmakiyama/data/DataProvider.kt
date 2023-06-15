package com.example.tea3_rodriguesmakiyama.data

import android.app.Application
import android.util.Log
import com.example.tea3_rodriguesmakiyama.data.database.entities.ItemEntity
import com.example.tea3_rodriguesmakiyama.data.database.entities.ListEntity
import com.example.tea3_rodriguesmakiyama.data.database.entities.PseudoEntity
import com.example.tea3_rodriguesmakiyama.data.database.getDatabase
import com.example.tea3_rodriguesmakiyama.data.network.retrofit.BASE_URL
import com.example.tea3_rodriguesmakiyama.data.network.retrofit.RetrofitApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LOG_TAG = "DataProvider"

class DataProvider(application : Application) {
    private var url : String = BASE_URL
    val database = getDatabase(application)

    suspend fun getLastPseudo(): PseudoEntity = withContext(Dispatchers.Default) { database.toDoDao.getLastPseudo() }

    suspend fun getHistory(): MutableList<String> {
        return withContext(Dispatchers.IO) {
            val strings = database.toDoDao.getPseudosHistory().map { it.pseudo }
            strings.toMutableList()
        }
    }

    suspend fun updateCachedPseudo(pseudo: PseudoEntity) {
        withContext(Dispatchers.Main) {
            database.toDoDao.addPseudo(pseudo)
        }
    }

    /** Tries to login in the api, returns the hash code received, if success.
    returns null otherwise */
    suspend fun login(user : String, password : String) : String? {
        return withContext(Dispatchers.Default) {
            // If there's no internet, throws an exception
            try {
                val response = RetrofitApi.retrofitService.connectUser(user, password)
                // If success is FALSE return FALSE
                if (!response.success){
                    Log.e("$LOG_TAG LoginError", "$response")
                    null
                } else {
                    // Search for the given 'user' in the database
                    val pseudo = database.toDoDao.getPseudo(user)

                    // If the pseudo was found in the database
                    if (pseudo != null) {
                        if (!pseudo.isLast) {
                            val lastPseudo = database.toDoDao.getLastPseudo()
                            // Resetting the lasPseudo
                            lastPseudo.isLast = false
                            lastPseudo.hash = null
                            lastPseudo.password = null
                            database.toDoDao.addPseudo(lastPseudo)
                            pseudo.isLast = true
                        }
                        pseudo.password = password
                        pseudo.hash = response.hash
                        database.toDoDao.addPseudo(pseudo)
                        response.hash
                    } else {
                        // Add new pseudo and update the last Pseudo, so it is not the last anymore
                        val newPseudo = PseudoEntity(pseudo = user, hash = response.hash, password = password, isLast = true)
                        val lastPseudo = database.toDoDao.getLastPseudo()

                        try {
                            lastPseudo.isLast = false
                            lastPseudo.hash = null
                            lastPseudo.password = null
                            database.toDoDao.addPseudo(lastPseudo)
                        } catch (e : Exception) {
                            Log.e("$LOG_TAG Login", "Error(empty database): ${e.message}")
                        }
                        database.toDoDao.addPseudo(newPseudo)

                        response.hash
                    }
                }
            } catch (e : Exception) {
                Log.e("$LOG_TAG Login", "Error: ${e.message}")
                null
            }
        }
    }

    /** Gets lists from API and saves on cache (DELETES old cache before!)
     * returns emptylist if any problem */
    suspend fun getListsFromAPI(hash: String, pseudo: String) : List<ListEntity> {
        return withContext(Dispatchers.Default) {
            try {
                val response = RetrofitApi.retrofitService.getLists(hash)

                if (response.lists != null) {
                    val lists = response.lists.map{
                        ListEntity(
                            idAPI = it.id,
                            label = it.label,
                            fromPseudo = pseudo
                        )
                    }

                    val listsCache = database.toDoDao.getListsFrom(pseudo)
                    // add/update lists retrieved on DB
                    for (list in lists) {
                       var found = listsCache.find { it.idAPI == list.idAPI }
                       if (found == null) {
                           found = list
                       }
                       database.toDoDao.addList(found)
                       Log.d("$LOG_TAG getListsFromApi", "added/updated list\n$list")
                    }

                    // return lists recently saved on DB
                    database.toDoDao.getListsFrom(pseudo)

                } else {
                    emptyList()
                }
            } catch (e : Exception) {
                Log.e("$LOG_TAG getLists", "Error: ${e.message}")
                emptyList()
            }
        }
    }

    /** Gets Lists from the cache database */
    suspend fun getCacheLists(pseudo: String) : List<ListEntity> {
        return withContext(Dispatchers.Default) {
            database.toDoDao.getListsFrom(pseudo)
        }
    }

    /** Gets items from api and saves on cache (erases all and saves). Returns a list with ItemEntity*/
    suspend fun getItemsFromAPI(hash: String, listLabel: String, listId: Int) : List<ItemEntity> {
        return withContext(Dispatchers.Default) {
            try {
                val response = RetrofitApi.retrofitService.getListItems(listId, hash)

                if (response.items != null) {
                    val items = response.items.map {
                        ItemEntity(
                        idAPI = it.id,
                        label = it.label,
                        url = it.url,
                        checked = it.isChecked,
                        fromList = listLabel
                        )
                    }

                    val itemsCache = database.toDoDao.getItemsFromList(listLabel)
                    // add/update items retrieved on DB
                    for (item in items) {
                        var found = itemsCache.find { it.idAPI == item.idAPI  }
                        if (found != null) {
                            found.checked = item.checked
                            found.url = item.url
                        } else {
                            found = item
                        }
                        database.toDoDao.addItem(found)
                        Log.d("$LOG_TAG getItemsFromApi", "added/updated item\n$item")
                    }

                    // Get recently added items from DB
                    database.toDoDao.getItemsFromList(listLabel)
                } else {
                    emptyList()
                }
            } catch (e : Exception) {
                Log.e("$LOG_TAG getItems", "Error: ${e.message}")
                emptyList()
            }
        }
    }

    /** Gets Items from the cache database. Returns a list of ItemEntity */
    suspend fun getCacheItems(listLabel: String) : List<ItemEntity> {
        return withContext(Dispatchers.Default) {
            database.toDoDao.getItemsFromList(listLabel)
        }
    }

    /** Updates offline changes to the API
     * (changes are creation of lists and items - and its current 'checked' state) */
    suspend fun updateOfflineDifferencesToApi(pseudo: String, hash: String) {
        withContext(Dispatchers.Default) {
            val lists = database.toDoDao.getListsFrom(pseudo)
            if (lists.isNotEmpty()) {
                for (list in lists) {
                    val items = database.toDoDao.getItemsFromList(list.label)
                    if (list.idAPI == null) {
                        try {
                            // Uploads offline List
                            val response =
                                RetrofitApi.retrofitService.addNewList(
                                    label = list.label,
                                    hash = hash,
                                )
                            // Saves the apiId in cache
                            database.toDoDao.addList(
                                ListEntity(
                                    id = list.id,
                                    idAPI = response.list!!.id,
                                    label = list.label,
                                    fromPseudo = list.fromPseudo
                                )
                            )
                            // Uploads each item from the list
                            if (response.success) {
                                for (it in items) {
                                    val itemResponse = RetrofitApi.retrofitService.addNewItemToList(
                                        listId = response.list.id,
                                        label = it.label,
                                        hash = hash,
                                    )
                                    database.toDoDao.addItem(
                                        ItemEntity(
                                            id = it.id,
                                            label = it.label,
                                            idAPI = itemResponse.item!!.id,
                                            checked = it.checked,
                                            url = itemResponse.item.url,
                                            fromList = list.label
                                        )
                                    )
                                    Log.d(
                                        "$LOG_TAG updateDifferencesToApi",
                                        "updated item: ${it.label} from list: ${list.label}"
                                    )
                                }
                            }
                            Log.d("$LOG_TAG updateDifferencesToApi", "uploaded list: $list")
                        } catch (e : Exception) {
                            Log.e("$LOG_TAG updateDifferencesToApi", "Error list offline: ${e.message}")
                        }
                    } else {
                        // Upload each item (adding and modifying) to the api
                        for (item in items) {
                            if (item.idAPI == null) {
                                try {
                                    // Add the offline item to API and upload its current 'checked' state
                                    Log.d("$LOG_TAG updateDifferencesToApi", "item: $item (list in api)")
                                    val response = RetrofitApi.retrofitService.addNewItemToList(
                                        listId = list.idAPI,
                                        label = item.label,
                                        hash = hash,
                                    )
                                    Log.d(
                                        "$LOG_TAG updateDifferencesToApi",
                                        "item (list in api) ${response.item} added to api"
                                    )
                                    database.toDoDao.addItem(
                                        ItemEntity(
                                            id = item.id,
                                            idAPI = response.item!!.id,
                                            label = item.label,
                                            url = response.item.url,
                                            checked = item.checked,
                                            fromList = item.fromList
                                        )
                                    )
                                    try {
                                        RetrofitApi.retrofitService.updateItemOfList(
                                            listId = list.idAPI,
                                            itemId = response.item.id,
                                            check = item.isChecked(),
                                            hash = hash,
                                        )
                                    } catch (e: Exception) {
                                        Log.e("$LOG_TAG updateDifferencesToApi", "Error from updateItem(${item.label})OfList(${list.label}):\n${e.message}")
                                    }
                                } catch (e : Exception) {
                                    Log.e("$LOG_TAG updateDifferencesToApi", "Error List in api: ${e.message}")
                                }
                            } else {
                                // Upload an existing item current 'checked' state
                                try {
                                    RetrofitApi.retrofitService.updateItemOfList(
                                        listId = list.idAPI,
                                        itemId = item.idAPI,
                                        check = item.isChecked(),
                                        hash = hash,
                                    )
                                } catch (e: Exception) {
                                    Log.e("$LOG_TAG updateDifferencesToApi", "Error List ${list.label} and Item ${item}\n in api: ${e.message}/n" +
                                            "${list.idAPI} and ${item.idAPI} $hash and ischecked ${item.isChecked()}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun checkItemApi(listId: Int, itemId: Int, check: Int, hash: String): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                val response = RetrofitApi.retrofitService.updateItemOfList(listId, itemId, check, hash)
                response.success
            } catch (e: Exception) {
                Log.d("$LOG_TAG checkItemApi", "Error: ${e.message}")
                false
            }
        }
    }

    suspend fun checkItemCache(item: ItemEntity) {
        withContext(Dispatchers.IO) {
            database.toDoDao.addItem(item)
        }
    }

    /** Uploads lists to API, if possible, then to database, returns the list's API id or null */
    suspend fun addList(label: String, pseudo: String, hash: String) : Int? {
        return withContext(Dispatchers.Default) {
            var idApi : Int? = null
            try {
                val response = RetrofitApi.retrofitService.addNewList(label, hash)
                idApi = response.list?.id
            } catch (e : Exception) {
                Log.e("$LOG_TAG DataProvider.addList", "Error : ${e.message}")
            }
            database.toDoDao.addList(ListEntity(idAPI = idApi, label = label, fromPseudo = pseudo))
            idApi
        }
    }

    /** Uploads an item to API, if possible, then adds to database, returns the item's API id or null */
    suspend fun addItem(label: String, listPseudo : String, listApiId: Int, hash: String) : Int? {
        return withContext(Dispatchers.Default) {
            var idApi : Int? = null
            var itemUrl: String? = null
            try {
                val response = RetrofitApi.retrofitService.addNewItemToList(listApiId, label, hash)
                idApi = response.item?.id
                itemUrl = response.item?.url
            } catch (e : Exception) {
                Log.e("$LOG_TAG DataProvider.addItem", "Error : ${e.message}")
            }
            database.toDoDao.addItem(ItemEntity(idAPI = idApi, label = label, url = itemUrl,checked = false,fromList = listPseudo))
            idApi
        }
    }

    /** Deletes a lists from API, then delete from database.
     * if was not possible returns FALSE */
    suspend fun deleteList(label: String, listId: Int, hash: String): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                val response = RetrofitApi.retrofitService.deleteList(listId = listId, hash = hash)
                if (response.success) database.toDoDao.deleteList(label)
                response.success
            } catch (e : Exception) {
                Log.e("$LOG_TAG DataProvider.deleteList", "Error : ${e.message}")
                false
            }
        }
    }

    /** Deletes a item from API, then delete from database.
     * if was not possible returns FALSE */
    suspend fun deleteItem(label: String, itemId: Int, listId: Int, hash: String) : Boolean{
        return withContext(Dispatchers.Default) {
            try {
                val response = RetrofitApi.retrofitService.deleteItemOfList(listId, itemId = itemId, hash)
                if (response.success) database.toDoDao.deleteItem(label)
                response.success
            } catch (e : Exception) {
                Log.e("$LOG_TAG DataProvider.deleteItem", "Error : ${e.message}")
                false
            }
        }
    }

    /** Clear cache history */
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            database.toDoDao.clearPseudos()
            database.toDoDao.clearLists()
            database.toDoDao.clearItems()
        }
    }

    fun setNewUrl(newUrl: String) {
        url = newUrl
    }
}