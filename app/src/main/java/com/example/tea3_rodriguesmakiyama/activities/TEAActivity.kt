package com.example.tea3_rodriguesmakiyama.activities

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.TEA3_RodriguesMakiyama.R
import com.example.tea3_rodriguesmakiyama.classes.Data
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

open class TEAActivity : AppCompatActivity() {
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

    open fun navigateToSettings() {}

    open fun disconnect() {}

    fun loadData(apiUrl: String? = null, filename: String? = null) : Data {
        if(filename.isNullOrEmpty()) {
            return if(apiUrl.isNullOrEmpty()) {
                Data()
            } else {
                Data(apiUrl=apiUrl)
            }
        }

        try {
            val fileInputStream: FileInputStream = openFileInput(filename)

            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val stringBuilder: StringBuilder = StringBuilder()

            var text: String?
            while (run {
                    text = bufferedReader.readLine()
                    text
                } != null) {
                stringBuilder.append(text)
            }

            inputStreamReader.close()
            return if(apiUrl.isNullOrEmpty()) {
                Data(data=stringBuilder.toString())
            } else {
                Data(apiUrl=apiUrl, data=stringBuilder.toString())
            }
        } catch (e : Exception) {
            return if(apiUrl.isNullOrEmpty()) {
                Data()
            } else {
                Data(apiUrl=apiUrl)
            }
        }
    }

    fun saveData(data: Data, filename: String) {
        openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(data.toJson().toByteArray())
        }
    }

    fun toastAlerter(msg: String, long: Boolean = false) {
        if (long) Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
        else Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }
}