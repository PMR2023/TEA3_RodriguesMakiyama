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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.TEA3_RodriguesMakiyama.R
import com.example.tea3_rodriguesmakiyama.api.Api
import com.example.tea3_rodriguesmakiyama.classes.Data
import com.example.tea3_rodriguesmakiyama.data.database.entities.ItemEntity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShowListActivity: TEAActivity() {
    lateinit var newItemET : EditText
    lateinit var buttonAdd : Button

    lateinit var items : MutableList<ItemEntity>

    lateinit var adapter: ListItemAdapter

    var listLabel: String = ""
    var listId: Int = -1
    var hash = ""
    var isCache = false
    lateinit var pseudo: String

    val mContext = this

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_show_list)
        super.onCreate(savedInstanceState)

        // Get a reference to RecyclerView and add the Adapter
        val itemsRV = findViewById<RecyclerView>(R.id.itemsRV)

        val loading = findViewById<ProgressBar>(R.id.loading)

        newItemET = findViewById(R.id.newItemText)
        buttonAdd = findViewById(R.id.buttonOKitem)

        // Get data from other activity
        intent?.extras?.getString(getString(R.string.key_list_label))?.let {
            listLabel = it
        }
        intent?.extras?.getInt(getString(R.string.key_list_id))?.let {
            listId = it
        }
        intent?.extras?.getString(getString(R.string.key_hash))?.let {
            hash = it
        }
        intent?.extras?.getBoolean(getString(R.string.key_is_cache))?.let {
            isCache = it
        }
        intent?.extras?.getString(getString(R.string.key_pseudo))?.let {
            pseudo = it
        }

        // Get data from database and show on RecyclerView
        coroutineScope.launch {
            loading.visibility = View.VISIBLE
            try {
                items = if (!isCache) {
                    dataProvider.getItemsFromAPI(listId = listId, listLabel = listLabel, hash = hash).toMutableList()
                } else {
                    dataProvider.getCacheItems(listLabel).toMutableList()
                }
                adapter = ListItemAdapter(items)
                Log.d("showGetData","adapter:$adapter items$items")
                itemsRV.adapter = adapter
                loading.visibility = View.GONE
            } catch (e: Exception) {
                Log.e("showList.getData", "Error: ${e.message}")
            }
        }

        // Monitors internet connection
        coroutineScope.launch {
            var backUpDone = false
            while(true) {
                if(!Api.checkNetwork(mContext) && !isCache) {
                    isCache = true
                } else if(Api.checkNetwork(mContext) && isCache && !backUpDone) {
                    backUpDone = true
                    isCache = false
                    reloadItems()
                }
                delay(1000)
            }
        }

        // Setup callback to add new item
        buttonAdd.setOnClickListener {
            val newItemName = newItemET.text.toString()
            if(newItemName.isNotEmpty()) {
                coroutineScope.launch {
                    // If newName is found in items
                    if (items.find { it -> it.label == newItemName} == null) {
                        newItemET.setText("")
                        val idApi = dataProvider.addItem(newItemName, listLabel, listId, hash)
                        Log.d("showList", "api returned id $idApi for item")
                        val recentItem = dataProvider.database.toDoDao.getItemsFromList(listLabel).last()
                        items.add(recentItem)
                        adapter.notifyItemInserted(items.size-1)
                    } else {
                        toastAlerter("Cet item existe déjà")
                    }
                }
            } else {
                toastAlerter("Saisissez un label valide")
            }
        }

        // Change ActionBar's name (optional)
        supportActionBar?.title = listLabel
    }

    private fun reloadItems() {
        coroutineScope.launch {
            val newItems = dataProvider.getItemsFromAPI(listLabel = listLabel, listId = listId, hash = hash)
            if (!items.containsAll(newItems)) {
                items.removeAll(items)
                items.addAll(newItems)
                adapter.notifyDataSetChanged()
            }
        }
    }


    // Send back the data
    override fun onBackPressed() {
        finish()
    }

    // Adapter necessary for the interface between the RecyclerView and the data that will be shown
    inner class ListItemAdapter(private val itemsList: MutableList<ItemEntity>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        inner class ListItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            fun bind(itemsList: MutableList<ItemEntity>, position: Int) {
                val item = itemsList[position]
                val text = itemView.findViewById<TextView>(R.id.itemText)
                val checked = itemView.findViewById<CheckBox>(R.id.checkedBox)
                val deleteItem = itemView.findViewById<ImageView>(R.id.deleteItem)

                text.text = item.label
                checked.isChecked = item.checked
                checked.setOnClickListener {
                    coroutineScope.launch {
                        item.checked = checked.isChecked
                        dataProvider.checkItemCache(item)
                        if(Api.checkNetwork(mContext)) {
                            if (item.idAPI != null) {
                                Log.d("checkItemApi", "$listId, ${item.idAPI}, ${item.isChecked()}")
                                dataProvider.checkItemApi(listId = listId, itemId = item.idAPI, check = item.isChecked(), hash = hash)
                            }
                        }
                    }
                }

                deleteItem.setOnClickListener {
                    deleteItem(itemsList, item, position)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ListItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as ListItemViewHolder).bind(itemsList, position)
        }

        override fun getItemCount(): Int {
            return itemsList.size
        }
    }

    private fun deleteItem(
        itemsList: MutableList<ItemEntity>,
        item: ItemEntity,
        position: Int) {
        coroutineScope.launch {
            if (Api.checkNetwork(mContext)) {
                val isDeleted = dataProvider.deleteItem(
                    label = item.label,
                    itemId = item.idAPI!!,
                    listId = listId,
                    hash = hash
                )
                if (isDeleted) {
                    itemsList.remove(item)
                    adapter.notifyItemRemoved(position)
                } else {
                    reloadItems()
                    toastAlerter(getString(R.string.delete_error_message))
                }
            } else {
                toastAlerter("Vérifiez votre connexion internet")
            }
        }
    }
}