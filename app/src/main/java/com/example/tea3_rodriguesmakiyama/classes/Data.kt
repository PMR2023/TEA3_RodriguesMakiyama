package com.example.TEA3_RodriguesMakiyama.classes

import com.google.gson.Gson

class Data(apiUrl: String? = null, data: String? = null) {
    private var pseudosHistory: MutableList<String> = mutableListOf()
    private var currentPseudo: String? = null
    private var currentHash: String? = null
    private var API_URL: String = "http://tomnab.fr/todo-api"

    init {
        if(data != null) {
            val savedData = Gson().fromJson(data, Data::class.java)
            pseudosHistory = savedData.getHistory()
            currentPseudo = savedData.getPseudo()
            currentHash = savedData.getHash()
            setApiUrl(savedData.getApiUrl())
        }
        if(apiUrl != null) {
            setApiUrl(apiUrl)
        }
    }

    fun getApiUrl() : String = API_URL

    fun setApiUrl(url: String) {
        API_URL = url
    }

    fun login(pseudo: String, hash: String) {
        if(pseudosHistory.contains(pseudo)) {
            pseudosHistory.remove(pseudo)
        }
        pseudosHistory.add(pseudo)
        currentPseudo = pseudo
        currentHash = hash
    }

    fun disconnect() {
        currentPseudo = null
        currentHash = null
    }

    fun getPseudo() : String? = currentPseudo

    fun getHash() : String? = currentHash

    fun getHistory() : MutableList<String> = pseudosHistory

    fun clearHistory() {
        pseudosHistory = mutableListOf()
        currentPseudo?.let {
            pseudosHistory.add(it)
        }
    }

    fun toJson() : String {
        return Gson().toJson(this)
    }
}