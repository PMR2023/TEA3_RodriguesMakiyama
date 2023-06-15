package com.example.tea3_rodriguesmakiyama.activities

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.TEA3_RodriguesMakiyama.R
import com.example.tea3_rodriguesmakiyama.classes.Data
import com.example.tea3_rodriguesmakiyama.data.network.retrofit.BASE_URL
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val D_TAG = "Settings"

class SettingsActivity : TEAActivity() {

    private var history: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_settings)
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

        supportActionBar?.title = getString(R.string.settings_action_bar_title)

        coroutineScope.launch {
            history = dataProvider.getHistory()
            // Updating the preference 'history' values
            with(sharedPref.edit()) {
                putString(getString(R.string.settings_key_history), getHistoryString())
                apply()
            }
        }

        findViewById<Button>(R.id.buttonClearHist).setOnClickListener {
            onClearHistory()
        }

    }

    private fun onClearHistory() {
        val size = history.size

        coroutineScope.launch {
            dataProvider.clearCache()
        }

        for (i in 0 until size) {
            history.removeAt(0)
        }

        // Clear the settings "pseudo", "baseUrl" and "history"
        with (sharedPref.edit()) {
            putString(getString(R.string.settings_key_apiurl), BASE_URL)
            putString(getString(R.string.settings_key_last_pseudo), "")
            putString(getString(R.string.settings_key_history), "")
            apply()
        }

        toastAlerter(getString(R.string.settings_message_clean_history))
    }

    override fun onBackPressed() {
        finish()
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
}

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_preferences, rootKey)
        preferenceManager.sharedPreferencesName = getString(R.string.shared_preferences_name)
        val sharedPref = (activity as SettingsActivity).sharedPref

        // Get a reference for the baseApiUrl EditTextPreference
        val baseUrl : EditTextPreference? = findPreference(getString(R.string.settings_key_apiurl))
        baseUrl?.setOnBindEditTextListener { editText ->
            editText.setText(sharedPref.getString(baseUrl.key, ""))
        }
        //TODO make the editText Preference alters the SharedPreferences

        val history: Preference? = findPreference(getString(R.string.settings_key_history))
        val pseudo: Preference? = findPreference(getString(R.string.settings_key_last_pseudo))

        // first set of summaries
        pseudo?.summary = sharedPref.getString(pseudo?.key, "")
        baseUrl?.summary = getString(R.string.settings_api_summary)
        history?.summary = sharedPref.getString(history?.key, "")

        // Set onChangeListener
        if (pseudo != null && history != null && baseUrl != null) {
            val listener = ChangeListener(pseudo = pseudo, url = baseUrl, history)
            sharedPref.registerOnSharedPreferenceChangeListener(listener)
            baseUrl.onPreferenceChangeListener = (EditTextChangeListener(sharedPref, requireActivity()))
        }
    }

    class ChangeListener(private val pseudo : Preference,
                         private val url : EditTextPreference,
                         private val history : Preference):
        SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            Log.d(D_TAG, "entrou no changelistener")
            // Showing the updated values
            when (key) {
                pseudo.key -> {
                    pseudo.summary = sharedPreferences.getString(key, "")
                }
                history.key -> {
                    history.summary = sharedPreferences.getString(key, "")
                }
                else -> {}
            }
            Log.d("$D_TAG fim", "$key : ${sharedPreferences.getString(key, "")}")
        }

    }
    class EditTextChangeListener(private val sharedPref : SharedPreferences, private val activity: Activity) : Preference.OnPreferenceChangeListener {
        override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
            val newURl = (preference as EditTextPreference).text

            if (newURl == "") {
                with(sharedPref.edit()) {
                    putString(activity.getString(R.string.settings_key_apiurl), BASE_URL)
                }
            }

            with(sharedPref.edit()) {
                putString(activity.getString(R.string.settings_key_apiurl), newURl)
            }
            return true
        }
    }

}