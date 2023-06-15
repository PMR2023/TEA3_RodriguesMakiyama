package com.example.tea3_rodriguesmakiyama.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.TEA3_RodriguesMakiyama.R
import com.example.tea3_rodriguesmakiyama.api.Api
import com.example.tea3_rodriguesmakiyama.data.network.retrofit.BASE_URL
import com.example.tea3_rodriguesmakiyama.data.network.retrofit.RetrofitApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LOG_TAG_RETROFIT = "RetrofitTest"
private const val LOG_TAG = "MainActivity"

class MainActivity: TEAActivity() {
    lateinit var pseudoET : EditText
    lateinit var passET : EditText
    lateinit var okButton : Button
    lateinit var signupButton : Button

    val mContext = this

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        super.onCreate(savedInstanceState)

        pseudoET = findViewById(R.id.pseudo)
        passET = findViewById(R.id.pass)
        okButton = findViewById(R.id.buttonOKpseudo)
        signupButton = findViewById(R.id.buttonSignup)

        //test retrofit
        /*coroutineScope.launch {
            try {
                var responseLogin = RetrofitApi.retrofitService.connectUser("myuser", "123")
                //toastAlerter(responseLogin.toString())
                Log.d(LOG_TAG_RETROFIT, responseLogin.toString())
                //responseLogin = RetrofitApi.retrofitService.addNewUser("myuser2", "321", responseLogin.hash)
                Log.d(LOG_TAG_RETROFIT, responseLogin.toString())
                val responseLists = RetrofitApi.retrofitService.getLists(responseLogin.hash)
                Log.d(LOG_TAG_RETROFIT, responseLists.toString())
                var responseItems = RetrofitApi.retrofitService.getListItems(responseLists.lists!![0].id, responseLogin.hash)
                Log.d(LOG_TAG_RETROFIT, responseItems.toString())
                //add new item WORKS
                //responseItems = RetrofitApi.retrofitService.addNewItemToList(responseLists.lists[0].id, "ItemT1", responseLogin.hash)
                responseItems = RetrofitApi.retrofitService.updateItemOfList(responseLists.lists[0].id, responseItems.items!![0].id, 1, responseLogin.hash)
                Log.d(LOG_TAG_RETROFIT, responseItems.toString())
                // delete recently added item WORKS
                //responseItems = RetrofitApi.retrofitService.deleteItemOfList(responseLists.lists!![0].id, responseItems.item!!.id, responseLogin.hash)
                //responseItems = RetrofitApi.retrofitService.getListItems(responseLists.lists!![0].id, responseLogin.hash)
                //Log.d(LOG_TAG, responseItems.toString())
                Log.d(LOG_TAG_RETROFIT, "Fin du test Retrofit test idAuto ")
            } catch(e: Exception) {
                Log.e("$LOG_TAG_RETROFIT", "${e.message}")
            }

        }*/


        // Sees if there's a lastPseudo to autofill and autologin
        coroutineScope.launch {
            // Throws an exception if the database is empty
            try {
                val lastPseudo = dataProvider.getLastPseudo()
                pseudoET.setText(lastPseudo.pseudo)
                if (lastPseudo.hash != null) {
                    val hash = dataProvider.login(lastPseudo.pseudo, lastPseudo.password!!)
                    Log.d("$LOG_TAG autoLogin", "hash:$hash")
                    if (hash != null) {
                        // Updates the hash from Last Pseudo, just in case!
                        lastPseudo.hash = hash
                        dataProvider.updateCachedPseudo(lastPseudo)
                        Log.d("$LOG_TAG autoLogin", "$lastPseudo hash:$hash")
                        navigateToShowLists(lastPseudo.pseudo, hash, showCached = false)
                    } else {
                        // Create an AlertDialog to ask if the user wants to see the cache,
                        //  defining buttons, clickListeners and texts
                        val builder = AlertDialog.Builder(this@MainActivity)
                        builder.setTitle(getString(R.string.alert_cache_title))
                        builder.setMessage(getString(R.string.alert_cache_message))
                        builder.setPositiveButton(getString(R.string.buttonOK)) { _, _ -> navigateToShowLists(lastPseudo.pseudo, lastPseudo.hash!!,true)}
                        builder.setNegativeButton(R.string.buttonCancel) { _, _ ->}
                        builder.show()
                    }
                }
            } catch (e : Exception) {
                Log.e("$LOG_TAG autologin", "Error: ${e.message}")
            }
        }

        // Monitors internet connection
        coroutineScope.launch {
            while(true) {
                if(!Api.checkNetwork(mContext) && okButton.isClickable) {
                    okButton.isClickable = false
                    okButton.setBackgroundColor(Color.rgb(93,93,93))

                    signupButton.isClickable = false
                    signupButton.setBackgroundColor(Color.rgb(93,93,93))
                } else if(Api.checkNetwork(mContext) && !okButton.isClickable) {
                    okButton.isClickable = true
                    okButton.setBackgroundColor(Color.rgb(103,80,164))

                    signupButton.isClickable = true
                    signupButton.setBackgroundColor(Color.rgb(103,80,164))
                }
                delay(1000)
            }
        }


        // Setting the login listener
        okButton.setOnClickListener {
            val url = sharedPref.getString(getString(R.string.settings_key_apiurl), "")
            try {
                // reset base_URL on sharedPreferences
                Log.d("$LOG_TAG url verify", "new url: $url")
                dataProvider.setNewUrl(url!!)

                // Login attempt
                val pseudo = pseudoET.text.toString()
                val pass = passET.text.toString()

                if(pseudo.isNotEmpty() && pass.isNotEmpty()) {
                    coroutineScope.launch {
                        val hash = dataProvider.login(pseudo, pass)
                        Log.d("Login", "p=$pseudo p=$pass, $hash")
                        if(hash != null) {
                            navigateToShowLists(pseudo, hash, false)
                        } else {
                            toastAlerter(getString(R.string.login_error_message))
                        }
                    }
                } else {
                    toastAlerter(getString(R.string.login_invalid_data))
                }
            } catch (e: Exception) {
                Log.e("$LOG_TAG URL Check", "Invalid URL: $url\n${e.message}")
                toastAlerter("Error: ${e.message}")
            }
        }

        // Setting Signup Listener
        signupButton.setOnClickListener {
            startActivity(Intent(applicationContext, SignupActivity::class.java))
        }

        // Change ActionBar's name (optional)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun navigateToShowLists(pseudo: String, hash: String, showCached: Boolean) {
        startActivity(Intent(applicationContext, ChoixListActivity::class.java).apply {
            val bundle = Bundle()
            bundle.putString(getString(R.string.key_pseudo), pseudo)
            bundle.putString(getString(R.string.key_hash), hash)
            bundle.putBoolean(getString(R.string.key_is_cache), showCached)
            putExtras(bundle)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}