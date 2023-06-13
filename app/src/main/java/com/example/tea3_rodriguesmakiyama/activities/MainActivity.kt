package com.example.tea3_rodriguesmakiyama.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.example.TEA3_RodriguesMakiyama.R
import com.example.tea3_rodriguesmakiyama.api.Api
import com.example.tea3_rodriguesmakiyama.classes.Data
import com.example.tea3_rodriguesmakiyama.retrofit.RetrofitApi
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val LOG_TAG = "RetrofitTest"

class MainActivity: TEAActivity() {
    lateinit var data : Data
    lateinit var pseudoET : EditText
    lateinit var passET : EditText
    lateinit var okButton : Button
    lateinit var signupButton : Button

    val mContext = this

    private val coroutineScope = CoroutineScope(
        Dispatchers.Main
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        super.onCreate(savedInstanceState)

        coroutineScope.launch {
            try {
                var responseLogin = RetrofitApi.retrofitService.connectUser("myuser2", "321")
                //toastAlerter(responseLogin.toString())
                Log.d(LOG_TAG, responseLogin.toString())
                //responseLogin = RetrofitApi.retrofitService.addNewUser("myuser2", "321", responseLogin.hash)
                Log.d(LOG_TAG, responseLogin.toString())
                //var responseLists = RetrofitApi.retrofitService.getLists(responseLogin.hash)
                //Log.d(LOG_TAG, responseLists.toString())
                // add new list WORKS
                //responseLists = RetrofitApi.retrofitService.addNewList("listaT1", responseLogin.hash)
                //Log.d(LOG_TAG, responseLists.toString())
                // delete recently added list
                //responseLists = RetrofitApi.retrofitService.deleteList(responseLists.list!!.id, responseLogin.hash)
                //Log.d(LOG_TAG, "DELETE:"+responseLists.toString())
                //responseLists = RetrofitApi.retrofitService.getLists(responseLogin.hash)
                //Log.d(LOG_TAG, responseLists.toString())
                /*responseLists.lists.let {
                    var responseItems = RetrofitApi.retrofitService.getListItems(responseLists.lists!![0].id, responseLogin.hash)
                    Log.d(LOG_TAG, responseItems.toString()+responseItems.items!![0].isChecked)
                    // add new item WORKS
                    //responseItems = RetrofitApi.retrofitService.addNewItemToList(responseLists.lists!![0].id, "ItemT1", responseLogin.hash)
                    //Log.d(LOG_TAG, responseItems.toString())
                    // delete recently added item WORKS
                    //responseItems = RetrofitApi.retrofitService.deleteItemOfList(responseLists.lists!![0].id, responseItems.item!!.id, responseLogin.hash)
                    //responseItems = RetrofitApi.retrofitService.getListItems(responseLists.lists!![0].id, responseLogin.hash)
                    //Log.d(LOG_TAG, responseItems.toString())

                }*/
                Log.d(LOG_TAG, "Fin du test Retrofit")
            } catch(_: Exception) {

            }

        }

        data = loadData(filename=getString(R.string.dataFile))
        Api.setApiUrl(data.getApiUrl())

        if(data.getPseudo() != null && data.getHash() != null) {
            // Validate pseudo and hash
            coroutineScope.launch {
                val response = Api.getLists(data.getHash() ?: "")
                if(response.success) {
                    // Redirects to ChoixListActivity
                    val dataJson = data.toJson()
                    startActivityForResult(Intent(applicationContext, ChoixListActivity::class.java).apply {
                        val bundle = Bundle()
                        bundle.putString("data", dataJson)
                        putExtras(bundle)
                    }, 1)
                }
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

        pseudoET = findViewById(R.id.pseudo)
        passET = findViewById(R.id.pass)
        okButton = findViewById(R.id.buttonOKpseudo)
        signupButton = findViewById(R.id.buttonSignup)

        // Setting the login listener
        okButton.setOnClickListener {
            val pseudo = pseudoET.text.toString()
            val pass = passET.text.toString()

            if(pseudo.isNotEmpty() && pass.isNotEmpty()) {
                coroutineScope.launch {
                    val response = Api.login(pseudo, pass)

                    if(response.success) {
                        data.login(pseudo, response.hash)
                        saveData(data, getString(R.string.dataFile))
                        val dataJson = data.toJson()
                        startActivityForResult(Intent(applicationContext, ChoixListActivity::class.java).apply {
                            val bundle = Bundle()
                            bundle.putString("data", dataJson)
                            putExtras(bundle)
                        }, 1)
                    } else {
                        toastAlerter("Pseudo ou mot de passe incorrect")
                    }
                }
            } else {
                toastAlerter("Saisissez un pseudo et un mot de passe valides")
            }
        }

        // Setting Signup Listener
        signupButton.setOnClickListener {
            val dataJson = data.toJson()
            startActivityForResult(Intent(applicationContext, SignupActivity::class.java).apply {
                val bundle = Bundle()
                bundle.putString("data", dataJson)
                putExtras(bundle)
            }, 1)
        }

        // Change ActionBar's name (optional)
        supportActionBar?.title = getString(R.string.app_name)
    }

    override fun onResume() {
        super.onResume()

        pseudoET.setText(data.getPseudo() ?: "")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dataAct: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataAct)

        if(resultCode == Activity.RESULT_OK) {
            dataAct?.extras?.getString("data")?.let {
                data = Gson().fromJson(it, Data::class.java)
            }
        }
    }

    override fun navigateToSettings() {
        val dataJson = data.toJson()
        startActivityForResult(Intent(applicationContext, SettingsActivity::class.java).apply {
            val bundle = Bundle()
            bundle.putString("data", dataJson)
            putExtras(bundle)
        }, 1)
    }

    override fun disconnect() {
        data.disconnect()
        saveData(data, getString(R.string.dataFile))

        val dataJson = data.toJson()
        startActivity(Intent(applicationContext, MainActivity::class.java).apply {
            val bundle = Bundle()
            bundle.putString("data", dataJson)
            putExtras(bundle)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}