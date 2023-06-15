package com.example.tea3_rodriguesmakiyama.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.TEA3_RodriguesMakiyama.R
import com.example.tea3_rodriguesmakiyama.api.Api
import com.example.tea3_rodriguesmakiyama.classes.Data
import com.example.tea3_rodriguesmakiyama.data.database.entities.ListEntity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChoixListActivity: TEAActivity() {
    lateinit var okButton : Button
    lateinit var listNameET : EditText
    private lateinit var listRV_adapter : ListAdapter

    lateinit var lists: MutableList<ListEntity>

    var pseudo: String = ""
    var hash = ""
    var isCache = false

    private val mContext = this

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_choix_list)
        super.onCreate(savedInstanceState)

        // Setup RecyclerView
        val listRV = findViewById<RecyclerView>(R.id.listRV)

        okButton = findViewById(R.id.buttonOKlist)
        listNameET = findViewById(R.id.newListET)

        val loading = findViewById<ProgressBar>(R.id.loading)

        // Get data from other activity
        intent.extras?.getBoolean(getString(R.string.key_is_cache))?.let {
            isCache = it
        }
        intent.extras?.getString(getString(R.string.key_pseudo))?.let {
            pseudo = it
        }
        intent.extras?.getString(getString(R.string.key_hash))?.let {
            hash = it
        }
        // Get data from database and show on RecyclerView
        coroutineScope.launch {
            loading.visibility = View.VISIBLE
            try {
                Log.d("getDataChoix", "beginning: iscache:${intent.extras?.getBoolean(getString(R.string.key_is_cache))}")
                lists = if (!intent.extras?.getBoolean(getString(R.string.key_is_cache))!!) {
                    Log.d("getDataChoix", "entered getListFromApi")
                    dataProvider.getListsFromAPI(hash, pseudo).toMutableList()
                } else {
                    Log.d("getDataChoix", "entered getCacheList")
                    dataProvider.getCacheLists(pseudo).toMutableList()
                }
                Log.d("getDataChoix", "lists: $lists iscache:$isCache")
                listRV_adapter = ListAdapter(lists)
                listRV.adapter = listRV_adapter
                loading.visibility = View.GONE
            } catch (e: Exception) {
                Log.e("get data", "Error: ${e.message}")
            }
        }

        // Monitors internet connection
        coroutineScope.launch {
            var backUpDone = false
            while(true) {
                if(!Api.checkNetwork(mContext) && !isCache) {
                    backUpDone = false
                    isCache = true
                } else if(Api.checkNetwork(mContext) && isCache && !backUpDone) {
                    Log.d("showList", "chama uploaddifferences iscache:$isCache")
                    uploadCacheDifferences(pseudo, hash)
                    reloadList()
                    backUpDone = true
                    isCache = false
                }
                delay(1000)
            }
        }

        // Setup callback to add new list
        okButton.setOnClickListener {
            val newListName = listNameET.text.toString()

            if(newListName.isNotEmpty()) {
                coroutineScope.launch {
                    if(lists.find { it -> it.label == newListName } == null) {
                        listNameET.setText("")
                        val idApi = dataProvider.addList(newListName, pseudo, hash)
                        val newList = ListEntity(idAPI = idApi, label = newListName, fromPseudo = pseudo)
                        lists.add(newList)
                        Log.d("addnovalista", "lista antes de notificar o adapter $lists.size")
                        listRV_adapter.notifyItemInserted(lists.size - 1)
                    } else {
                        toastAlerter("Cette liste existe déjà")
                    }
                }
            } else {
                toastAlerter("Saisissez un label valide")
            }
        }

        // Change ActionBar's name (optional)
        supportActionBar?.title = "Listes de $pseudo"
    }

    private suspend fun reloadList() {
        val newLists = dataProvider.getListsFromAPI(pseudo = pseudo, hash = hash)
        if (!lists.containsAll(newLists)) {
            lists.removeAll(lists)
            lists.addAll(newLists)
            listRV_adapter.notifyDataSetChanged()
        }
    }

    override fun onBackPressed() {
        finish()
    }

    private fun deleteList(position: Int) {
        coroutineScope.launch {
            if (Api.checkNetwork(mContext)) {
                val isDeleted = dataProvider.deleteList(lists[position].label, lists[position].idAPI!!, hash)
                if (isDeleted) {
                    lists.removeAt(position)
                    listRV_adapter.notifyItemRemoved(position)
                    Log.d("deleteItem", "$isDeleted = delete item at $position")
                } else {
                    toastAlerter(getString(R.string.delete_error_message))
                }
            } else {
                toastAlerter(getString(R.string.network_error_message))
            }
        }
    }

    // RecyclerView
    inner class ListAdapter(
        private val lists : MutableList<ListEntity>,
    ) : RecyclerView.Adapter<ViewHolder>() {
        inner class ListViewHolder(itemView : View) : ViewHolder(itemView) {
            fun bind(lists: MutableList<ListEntity>, position: Int) {
                val listName = itemView.findViewById<TextView>(R.id.listName)
                listName.text = lists[position].label

                itemView.setOnClickListener {
                    onNavigateToShowList(lists[position].label)
                }

                // ClickListener to delete the list
                itemView.findViewById<ImageView>(R.id.deleteList).setOnClickListener {
                    deleteList(position)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ListViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.list,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder as ListViewHolder).bind(lists, position)
        }

        override fun getItemCount(): Int = lists.size
    }

    private fun onNavigateToShowList(label: String) {
        coroutineScope.launch {
            val listId = lists.find { it -> it.label==label }?.idAPI ?: -1
            startActivity(Intent(applicationContext, ShowListActivity::class.java).apply {
                val bundle = Bundle()
                bundle.putString(getString(R.string.key_list_label), label)
                bundle.putInt(getString(R.string.key_list_id), listId)
                bundle.putString(getString(R.string.key_hash), hash)
                bundle.putBoolean(getString(R.string.key_is_cache), isCache)
                bundle.putString(getString(R.string.key_pseudo), pseudo)
                putExtras(bundle)
            })
        }
    }
}