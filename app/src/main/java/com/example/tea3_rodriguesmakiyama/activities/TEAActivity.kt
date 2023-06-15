package com.example.tea3_rodriguesmakiyama.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.TEA3_RodriguesMakiyama.R
import com.example.tea3_rodriguesmakiyama.data.DataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOG_TAG = "TEAActivity"

open class TEAActivity : AppCompatActivity() {
    // For data manipulation across the entire app
    val dataProvider : DataProvider by lazy {DataProvider(application)}
    val sharedPref: SharedPreferences by lazy {getSharedPreferences(getString(R.string.shared_preferences_name), Context.MODE_PRIVATE)}
    // Coroutine for the entire app
    val coroutineScope = CoroutineScope(
        Dispatchers.Main
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setup the ActionBar, change its name on each children (optional)
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> navigateToSettings()
            R.id.disconnect -> disconnect()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun navigateToSettings() {
        coroutineScope.launch {
            try {
                val lastPseudo = dataProvider.getLastPseudo()
                // updates the last pseudo preference
                sharedPref.edit().putString(getString(R.string.settings_key_last_pseudo), lastPseudo.pseudo).apply()
            } catch (e: Exception) {
                Log.e("$LOG_TAG click Settings:", "Error: ${e.message}")
            } finally {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
            }
        }
    }

    private fun disconnect() {
        coroutineScope.launch {
            try {
                val lastPseudo = dataProvider.getLastPseudo()
                lastPseudo.hash = null
                lastPseudo.password = null
                dataProvider.updateCachedPseudo(lastPseudo)
            } catch (e: Exception) {
                Log.e("$LOG_TAG click Disconnect:", "Error: ${e.message}")
            } finally {
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }
        }
    }

    fun toastAlerter(msg: String, long: Boolean = false) {
        if (long) Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
        else Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    suspend fun uploadCacheDifferences(pseudo: String, hash: String) {
        dataProvider.updateOfflineDifferencesToApi(pseudo, hash)
        toastAlerter("${getString(R.string.alert_connexion_reestablished_title)}${getString(R.string.alert_connexion_reestablished_message)}")
    }
}