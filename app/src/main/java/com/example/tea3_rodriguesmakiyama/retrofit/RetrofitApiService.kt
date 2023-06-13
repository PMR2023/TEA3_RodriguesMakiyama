package com.example.tea3_rodriguesmakiyama.retrofit

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

const val BASE_URL = "http://tomnab.fr/todo-api/"

// Creates a Moshi object with the MoshiBuilder giving it an adapter
private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

/* Creates a retrofit object, associating it to a converter (our moshi object, created by a MoshiConvertFactory)
    and the base URL */
private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

interface RetrofitApiService {
    // Using suspend makes this a Coroutine function, automatically integrated in Retrofit and Moshi
    @POST("authenticate")
    suspend fun connectUser(@Query("user") user : String,
                            @Query("password") password : String) : Response

    @POST("users?pseudo=toto&pass=tata")
    suspend fun addNewUser(@Query("pseudo") pseudo : String,
                           @Query("pass") password : String,
                           @Query("hash") hash : String) : Response

    @GET("lists")
    suspend fun getLists(@Query("hash") hash : String) : Response

    @GET("lists/{listId}/items")
    suspend fun getListItems(@Path("listId") listId : Int,
                             @Query("hash") hash : String) : Response

    @POST("lists")
    suspend fun addNewList(@Query("label") label : String,
                           @Query("hash") hash : String) : Response

    @DELETE("lists/{listId}")
    suspend fun deleteList(@Path("listId") listId : Int,
                           @Query("hash") hash : String) : Response

    @POST("lists/{listId}/items")
    suspend fun addNewItemToList(@Path("listId") listId : Int,
                                 @Query("label") label :String,
                                 @Query("hash") hash : String) : Response

    @PUT("lists/{listId}/items/{itemId}")
    suspend fun updateItemOfList(@Path("listId") listId : Int,
                                 @Path("itemId") itemId : Int,
                                 @Query("check") check : Int,
                                 @Query("hash") hash : String) : Response

    @DELETE("lists/{listId}/items/{itemId}")
    suspend fun deleteItemOfList(@Path("listId") listId : Int,
                                 @Path("itemId") itemId : Int,
                                 @Query("hash") hash : String) : Response
}

/** Since the retrofit.create() is an expensive call, we expose our API to the rest of the app using
 * a singleton 'object' and it is lazily initiated (only initiated when needed)
 */
object RetrofitApi {
    val retrofitService: RetrofitApiService by lazy {
        retrofit.create(RetrofitApiService::class.java)
    }
}