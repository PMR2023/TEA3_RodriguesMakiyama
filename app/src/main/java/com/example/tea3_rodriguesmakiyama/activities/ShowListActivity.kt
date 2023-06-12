package com.example.TEA3_RodriguesMakiyama.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.TEA3_RodriguesMakiyama.R
import com.example.TEA3_RodriguesMakiyama.api.Api
import com.example.TEA3_RodriguesMakiyama.classes.Data
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShowListActivity: TEAActivity() {
    lateinit var list : String
    lateinit var data : Data

    lateinit var newItemET : EditText
    lateinit var buttonAdd : Button
    lateinit var itemsRV_adapter : ListItemAdapter

    val mContext = this

    private val coroutineScope = CoroutineScope(
        Dispatchers.Main
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_show_list)
        super.onCreate(savedInstanceState)

        newItemET = findViewById(R.id.newItemText)
        buttonAdd = findViewById(R.id.buttonOKitem)

        // Get data
        intent?.extras?.getString("list")?.let {
            list = it
        }
        intent?.extras?.getString("data")?.let {
            data = Gson().fromJson(it, Data::class.java)
        }

        // Monitors internet connection
        coroutineScope.launch {
            while(true) {
                if(!Api.checkNetwork(mContext) && buttonAdd.isClickable) {
                    buttonAdd.isClickable = false
                    buttonAdd.setBackgroundColor(Color.rgb(93,93,93))
                } else if(Api.checkNetwork(mContext) && !buttonAdd.isClickable) {
                    buttonAdd.isClickable = true
                    buttonAdd.setBackgroundColor(Color.rgb(103,80,164))
                }
                delay(1000)
            }
        }

        // Change ActionBar's name (optional)
        supportActionBar?.title = list
    }

    override fun onResume() {
        super.onResume()

        // Get a reference to RecyclerView and add the Adapter
        val itemsRV = findViewById<RecyclerView>(R.id.itemsRV)
        var items: MutableMap<String, Boolean> = mutableMapOf()
        coroutineScope.launch {
            val itemsApi = Api.getItems(list, data.getHash() ?: "").items
            itemsApi.forEach {
                items[it.label] = (it.checked=="1")
            }

            itemsRV_adapter = ListItemAdapter(items)
            itemsRV.adapter = itemsRV_adapter
        }
        itemsRV.layoutManager = LinearLayoutManager(this)

        // Setup callback to add new item
        buttonAdd.setOnClickListener {
            val newItemName = newItemET.text.toString()

            if(newItemName.isNotEmpty()) {
                coroutineScope.launch {
                    val response = Api.getItems(list, data.getHash()?:"")

                    var itemExists = false
                    response.items.forEach {
                        if(it.label == newItemName) {
                            itemExists = true
                        }
                    }

                    if(!itemExists) {
                        newItemET.setText("")

                        Api.addItem(list, newItemName, data.getHash() ?: "")

                        val itemsApi = Api.getItems(list, data.getHash() ?: "").items
                        itemsApi.forEach {
                            items[it.label] = (it.checked=="1")
                        }

                        itemsRV_adapter.notifyItemInserted(items.size-1)
                    } else {
                        toastAlerter("Cet item existe déjà")
                    }
                }
            } else {
                toastAlerter("Saisissez un label valide")
            }
        }
    }

    // Send back the data
    override fun onBackPressed() {
        val int = Intent()
        int.putExtra("data", data.toJson())
        setResult(Activity.RESULT_OK, int)

        finish()
    }

    // Adapter necessary for the interface between the RecyclerView and the data that will be shown
    inner class ListItemAdapter(private val itemsList: MutableMap<String, Boolean>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        inner class ListItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            fun bind(itemsList: MutableMap<String, Boolean>, position: Int) {
                val listItem: Pair<String, Boolean> = itemsList.toList()[position]
                val text = itemView.findViewById<TextView>(R.id.itemText)
                val checked = itemView.findViewById<CheckBox>(R.id.checkedBox)
                val deleteItem = itemView.findViewById<ImageView>(R.id.deleteItem)

                text.text = listItem.first
                checked.isChecked = listItem.second
                checked.setOnClickListener {
                    if(Api.checkNetwork(mContext)) {
                        coroutineScope.launch {
                            Api.checkItem(list, listItem.first, checked.isChecked, data.getHash() ?: "")
                        }
                    } else {
                        toastAlerter("Vérifiez votre connexion internet")
                    }
                }
                deleteItem.setOnClickListener {
                    if(Api.checkNetwork(mContext)) {
                        itemsList.remove(listItem.first)
                        itemsRV_adapter.notifyItemRemoved(position)
                        coroutineScope.launch {
                            Api.delItem(list, listItem.first, data.getHash() ?: "")
                        }
                    } else {
                        toastAlerter("Vérifiez votre connexion internet")
                    }
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
}