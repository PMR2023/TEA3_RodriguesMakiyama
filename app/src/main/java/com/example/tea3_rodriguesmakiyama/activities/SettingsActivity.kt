package com.example.TEA3_RodriguesMakiyama.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.TEA3_RodriguesMakiyama.R
import com.example.TEA3_RodriguesMakiyama.classes.Data
import com.example.TEA3_RodriguesMakiyama.retrofit.BASE_URL
import com.google.gson.Gson

private const val D_TAG = "Settings"
private var DISCONECT = false

class SettingsActivity : TEAActivity() {
    lateinit var data: Data

    private var history: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_settings)
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

        supportActionBar?.title = "Préférences"

        intent?.extras?.getString("data")?.let {
            data = Gson().fromJson(it, Data::class.java)
        }
        history = data.getHistory()

        findViewById<Button>(R.id.buttonClearHist).setOnClickListener {
            onClearHistory()
        }

        // Updating the preference values
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        with(sharedPref!!.edit()) {
            putString(getString(R.string.settings_key_last_pseudo), getLastPseudo())
            putString(getString(R.string.settings_key_history), getHistoryString())
            putString(getString(R.string.settings_key_last_hash), data.getHash())
            apply()
        }
    }

    private fun onClearHistory() {
        data.clearHistory()
        data.setApiUrl(BASE_URL)
        val size = history.size
        for (i in 0 until size) {
            history.removeAt(0)
        }

        // Clear the settings "pseudo", "baseUrl" and "history"
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        Log.d(D_TAG+"onClear", "${getString(R.string.settings_key_apiurl)} before : ${sharedPref.getString(getString(R.string.settings_key_apiurl), "")}")
        with (sharedPref!!.edit()) {
            putString(getString(R.string.settings_key_apiurl), BASE_URL)
            putString(getString(R.string.settings_key_last_pseudo), "")
            putString(getString(R.string.settings_key_history), "")
            apply()
        }
        Log.d(D_TAG+"onClear", "${getString(R.string.settings_key_apiurl)} after : ${sharedPref.getString(getString(R.string.settings_key_apiurl), "")}")
        saveData(Data(), getString(R.string.dataFile))
        toastAlerter(getString(R.string.settings_message_clean_history))
    }

    override fun onResume() {
        super.onResume()

        intent?.extras?.getString("data")?.let {
            data = Gson().fromJson(it, Data::class.java)
        }

        history = data.getHistory()
    }

    override fun onBackPressed() {
        Log.d(D_TAG+"BackPressed", "appUrl sent to other activity=${data.getApiUrl()}")
        val int = Intent()
        Log.d(D_TAG+"BackPressed", "appUrl sent to other activity=${data.getApiUrl()}")
        int.putExtra("data", data.toJson())
        setResult(Activity.RESULT_OK, int)

        finish()
    }

    override fun disconnect() {
        DISCONECT = true
        data.disconnect()
        saveData(data, getString(R.string.dataFile))

        val dataJson = data.toJson()

        Log.d(D_TAG, "Disconnect: appUrl sent to other activity=${data.getApiUrl()}")
        startActivity(Intent(applicationContext, MainActivity::class.java).apply {
            val bundle = Bundle()
            bundle.putString("data", dataJson)
            putExtras(bundle)
        })
}
    /**
     * Transforms the history in a String that can be saved as a Preference and automatically displayed
     */
    private fun getHistoryString(): String {
        return if (history.isEmpty()) {
            ""
        } else {
            var str = ""
            for (h in history.reversed()) {
                str += "• $h\n"
            }
            str
        }
    }

    private fun getLastPseudo(): String {
        return if (history.isEmpty()) {
            ""
        } else {
            history.last()
        }
    }
}

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_preferences, rootKey)
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)

        // Get a reference for the baseApiUrl EditTextPreference
        val baseUrl : EditTextPreference? = findPreference(getString(R.string.settings_key_apiurl))
        baseUrl?.setOnBindEditTextListener { editText ->
            editText.setText(sharedPref?.getString(baseUrl.key, ""))
        }
        // Setting a SummaryProvider that automatically sets the "summary" attribute
        baseUrl?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            val text = preference.text
            val activity = (activity as SettingsActivity)
            if (text.isNullOrEmpty()) {
                activity.data.setApiUrl(BASE_URL)
                with(sharedPref!!.edit()) {
                    putString(preference.key, BASE_URL)
                    apply()
                }
                BASE_URL
            } else {
                activity.data.setApiUrl(text)
                with(sharedPref!!.edit()) {
                    putString(preference.key, text)
                    apply()
                }
                text
            }
        }

        val history: Preference? = findPreference(getString(R.string.settings_key_history))
        val pseudo: Preference? = findPreference(getString(R.string.settings_key_last_pseudo))

        // first set of summaries
        pseudo?.summary = sharedPref?.getString(pseudo?.key, "")
        history?.summary = sharedPref?.getString(history?.key, "")

        // Set onChangeListener
        if (pseudo != null && history != null && baseUrl != null) {
            val listener = ChangeListener(pseudo = pseudo, url = baseUrl, history)
            sharedPref?.registerOnSharedPreferenceChangeListener(listener)
        }
    }

    class ChangeListener(private val pseudo : Preference,
                         private val url : EditTextPreference,
                         private val history : Preference):
        SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            Log.d(D_TAG, "entrou no changelistener")
            // Showing the updated values
            when (key) {
                pseudo.key -> {
                    pseudo.summary = sharedPreferences?.getString(key, "")
                }
                history.key -> {
                    history.summary = sharedPreferences?.getString(key, "")
                }
                url.key -> {
                    Log.d(D_TAG+"URLChangeListener", "Entererd and url.text is ${url.text} and its value=${sharedPreferences?.getString(key, "")}")
                    //url.text = sharedPreferences?.getString(key, "")
                    //url.summary = sharedPreferences?.getString(key, "")
                }
                else -> {}
            }
            Log.d(D_TAG+" fim", "$key : ${sharedPreferences?.getString(key, "")}")
        }

    }

}