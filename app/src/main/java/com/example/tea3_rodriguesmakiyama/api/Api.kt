package com.example.tea3_rodriguesmakiyama.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.example.tea3_rodriguesmakiyama.api.responses.HashResponse
import com.example.tea3_rodriguesmakiyama.api.responses.ItemsResponse
import com.example.tea3_rodriguesmakiyama.api.responses.ListsResponse
import com.example.tea3_rodriguesmakiyama.api.responses.SignupResponse
import com.example.tea3_rodriguesmakiyama.api.responses.UsersResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL


object Api {
    private var API_URL = ""

    //---[AUXILIARY METHODS]---//
    fun checkNetwork(context: Context) : Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if  (capabilities != null &&
            (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
             capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
             capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) {
                return true
        }
        return false
    }

    fun getApiUrl() : String = API_URL

    fun setApiUrl(url: String) {
        API_URL = url
    }

    //---[PUBLIC METHODS]---//
    suspend fun login(
        user: String,
        password: String
    ) : HashResponse = withContext(Dispatchers.IO) {
        val response: String? = call(
            "/authenticate",
            "POST",
            params = mapOf(
                "user" to user,
                "password" to password
            )
        )

        if(!response.isNullOrEmpty()) {
            Gson().fromJson(response, HashResponse::class.java)
        } else {
            HashResponse(
                "todo",
                "",
                -1,
                false,
                1.2
            )
        }
    }

    suspend fun signup(
        user: String,
        password: String
    ) : SignupResponse = withContext(Dispatchers.IO) {
        val adminHash = login("tom", "web").hash

        val response: String? = call(
            "/users",
            "POST",
            params = mapOf(
                "pseudo" to user,
                "pass" to password
            ),
            headers = mapOf(
                "hash" to adminHash
            )
        )

        if(!response.isNullOrEmpty()) {
            Gson().fromJson(response, SignupResponse::class.java)
        } else {
            SignupResponse(
                "todo",
                -1,
                false,
                null,
                1.2
            )
        }
    }

    suspend fun checkUserAvailable(
        user: String
    ) : Boolean = withContext(Dispatchers.IO) {
        val adminHash = login("tom", "web").hash

        val response: String? = call(
            "/users",
            "GET",
            headers = mapOf(
                "hash" to adminHash
            )
        )

        var id = -1
        if(!response.isNullOrEmpty()) {
            val usersResponse = Gson().fromJson(response, UsersResponse::class.java)
            if(!usersResponse.users.isNullOrEmpty()) {
                usersResponse.users.forEach {
                    if(it.pseudo == user) {
                        id = it.id.toInt()
                    }
                }
            }
        }
        id == -1
    }

    suspend fun getLists(
        hash: String
    ) : ListsResponse = withContext(Dispatchers.IO) {
        val response: String? = call(
            "/lists",
            "GET",
            params = mapOf(
                "hash" to hash
            )
        )

        if(!response.isNullOrEmpty()) {
            Gson().fromJson(response, ListsResponse::class.java)
        } else {
            ListsResponse(
                "todo",
                listOf(),
                -1,
                false,
                1.2
            )
        }
    }

    suspend fun addList(
        listName: String,
        hash: String
    ) : Unit = withContext(Dispatchers.IO) {
        call(
            "/lists",
            "POST",
            params = mapOf(
                "label" to listName
            ),
            headers = mapOf(
                "hash" to hash
            )
        )
    }

    suspend fun delList(
        listName: String,
        hash: String
    ) : Unit = withContext(Dispatchers.IO) {
        val listId = getListId(listName, hash)

        if(listId != null) {
            call(
                "/lists/$listId",
                "DELETE",
                headers = mapOf(
                    "hash" to hash
                )
            )
        }
    }

    suspend fun getItems(
        listName: String,
        hash: String
    ) : ItemsResponse = withContext(Dispatchers.IO) {
        val listId = getListId(listName, hash)

        if(listId != null) {
            val response = call(
                "/lists/$listId/items",
                "GET",
                headers = mapOf(
                    "hash" to hash
                )
            )

            if(!response.isNullOrEmpty()) {
                Gson().fromJson(response, ItemsResponse::class.java)
            } else {
                ItemsResponse(
                    "todo",
                    listOf(),
                    -1,
                    false,
                    1.2
                )
            }
        } else {
            ItemsResponse(
                "todo",
                listOf(),
                -1,
                false,
                1.2
            )
        }
    }

    suspend fun addItem(
        listName: String,
        itemName: String,
        hash: String
    ) : Unit = withContext(Dispatchers.IO) {
        val listId = getListId(listName, hash)

        if(listId != null) {
            call(
                "/lists/$listId/items",
                "POST",
                params = mapOf(
                    "label" to itemName
                ),
                headers = mapOf(
                    "hash" to hash
                )
            )
        }
    }

    suspend fun checkItem(
        listName: String,
        itemName: String,
        check: Boolean,
        hash: String
    ) : Unit = withContext(Dispatchers.IO) {
        val listId = getListId(listName, hash)
        val itemId = getItemId(listName, itemName, hash)

        if(listId != null && itemId != null) {
            val checkStr: String = if(check) "1" else "0"
            call(
                "/lists/$listId/items/$itemId",
                "PUT",
                params = mapOf(
                    "check" to checkStr
                ),
                headers = mapOf(
                    "hash" to hash
                )
            )
        }
    }

    suspend fun delItem(
        listName: String,
        itemName: String,
        hash: String
    ) : Unit = withContext(Dispatchers.IO) {
        val listId = getListId(listName, hash)
        val itemId = getItemId(listName, itemName, hash)

        if(listId != null && itemId != null) {
            call(
                "/lists/$listId/items/$itemId",
                "DELETE",
                headers = mapOf(
                    "hash" to hash
                )
            )
        }
    }

    //---[PRIVATE METHODS]---//
    private suspend fun getListId(
        listName: String,
        hash: String
    ): Int? {
        val lists = getLists(hash)

        if(lists.success && lists.lists.isNotEmpty()) {
            lists.lists.forEach {
                if(it.label == listName) {
                    return it.id.toInt()
                }
            }
        }
        return null
    }

    private suspend fun getItemId(
        listName: String,
        itemName: String,
        hash: String
    ): Int? {
        val items = getItems(listName, hash)

        if(items.success && items.items.isNotEmpty()) {
            items.items.forEach {
                if(it.label == itemName) {
                    return it.id.toInt()
                }
            }
        }
        return null
    }

    private fun call(
        endpoint: String,
        method: String = "GET",
        params: Map<String, String> = mapOf(),
        headers: Map<String, String> = mapOf()
    ): String? {
        var urlConnection: HttpURLConnection? = null
        var reader: BufferedReader? = null
        var uriBuilder = Uri.Builder()
        var requestUrl = API_URL + endpoint

        var response : String?

        try {
            if(!params.isNullOrEmpty()) {
                params.forEach {
                    uriBuilder.appendQueryParameter(it.key, it.value)
                }
                requestUrl += uriBuilder.build().toString()
            }

            urlConnection = URL(requestUrl).openConnection() as HttpURLConnection
            urlConnection.requestMethod = method

            if(!headers.isNullOrEmpty()) {
                headers.forEach {
                    urlConnection.setRequestProperty(it.key, it.value)
                }
            }

            urlConnection.connect()

            reader = urlConnection.inputStream?.bufferedReader()
            response = reader?.readText()
        } catch (e : Exception) {
            response = null
        }finally {
                urlConnection?.disconnect()
                reader?.close()
        }

        return response
    }
}