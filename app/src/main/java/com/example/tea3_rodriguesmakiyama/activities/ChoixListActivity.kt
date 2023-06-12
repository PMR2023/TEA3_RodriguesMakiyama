package com.example.TEA3_RodriguesMakiyama.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.TEA3_RodriguesMakiyama.R
import com.example.TEA3_RodriguesMakiyama.api.Api
import com.example.TEA3_RodriguesMakiyama.classes.Data
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChoixListActivity: TEAActivity() {
    lateinit var data : Data

    lateinit var okButton : Button
    lateinit var listNameET : EditText
    lateinit var listRV_adapter : ListAdapter

    val mContext = this

    private val coroutineScope = CoroutineScope(
        Dispatchers.Main
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_choix_list)
        super.onCreate(savedInstanceState)

        okButton = findViewById(R.id.buttonOKlist)
        listNameET = findViewById(R.id.newListET)

        // Get data
        intent?.extras?.getString("data")?.let {
            data = Gson().fromJson(it, Data::class.java)
        }

        // Monitors internet connection
        coroutineScope.launch {
            while(true) {
                if(!Api.checkNetwork(mContext) && okButton.isClickable) {
                    okButton.isClickable = false
                    okButton.setBackgroundColor(Color.rgb(93,93,93))
                } else if(Api.checkNetwork(mContext) && !okButton.isClickable) {
                    okButton.isClickable = true
                    okButton.setBackgroundColor(Color.rgb(103,80,164))
                }
                delay(1000)
            }
        }

        // Change ActionBar's name (optional)
        supportActionBar?.title = "Listes de ${data.getPseudo()}"
    }

    override fun onResume() {
        super.onResume()

        // Setup RecyclerView
        val listRV = findViewById<RecyclerView>(R.id.listRV)
        var lists: MutableList<String> = mutableListOf()
        coroutineScope.launch {
            val listsApi = Api.getLists(data.getHash() ?: "").lists
            listsApi.forEach {
                lists.add(it.label)
            }

            listRV_adapter = ListAdapter(lists, mContext)
            listRV.adapter = listRV_adapter
        }
        listRV.layoutManager = LinearLayoutManager(this)

        // Setup callback to add new list
        okButton.setOnClickListener {
            val newListName = listNameET.text.toString()

            if(newListName.isNotEmpty()) {
                coroutineScope.launch {
                    val response = Api.getItems(newListName, data.getHash()?:"")
                    if(!response.success) {
                        listNameET.setText("")
                        Api.addList(newListName, data.getHash()?:"")
                        lists.add(newListName)

                        listRV_adapter.notifyItemInserted(lists.size-1)
                    } else {
                        toastAlerter("Cette liste existe déjà")
                    }
                }
            } else {
                toastAlerter("Saisissez un label valide")
            }
        }
    }

    override fun onBackPressed() {
        val int = Intent()
        int.putExtra("data", data.toJson())
        setResult(Activity.RESULT_OK, int)

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dataAct: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataAct)
        if(requestCode == 2 && resultCode == Activity.RESULT_OK) {
            dataAct?.extras?.getString("data")?.let {
                data = Gson().fromJson(it, Data::class.java)
            }
        }
    }

    // RecyclerView
    inner class ListAdapter(
        private val lists : MutableList<String>,
        private val mContext : Activity
    ) : RecyclerView.Adapter<ViewHolder>() {
        inner class ListViewHolder(itemView : View) : ViewHolder(itemView) {
            fun bind(lists: MutableList<String>, position: Int) {
                itemView.findViewById<TextView>(R.id.listName).text = lists[position]

                itemView.setOnClickListener {
                    if(Api.checkNetwork(mContext)) {
                        val dataJson = data.toJson()
                        mContext.startActivityForResult(
                            Intent(
                                applicationContext,
                                ShowListActivity::class.java
                            ).apply {
                                val bundle = Bundle()
                                bundle.putString(
                                    "list",
                                    itemView.findViewById<TextView>(R.id.listName).text.toString()
                                )
                                bundle.putString("data", dataJson)
                                putExtras(bundle)
                            }, 2
                        )
                    } else {
                        toastAlerter("Vérifiez votre connexion internet")
                    }
                }

                itemView.findViewById<ImageView>(R.id.deleteList).setOnClickListener {
                    if(Api.checkNetwork(mContext)) {
                        var deletedPosition = 0
                        lists.forEachIndexed { index, s ->
                            if (s == itemView.findViewById<TextView>(R.id.listName).text.toString()) {
                                deletedPosition = index
                            }
                        }

                        lists.removeAt(deletedPosition)
                        listRV_adapter.notifyItemRemoved(deletedPosition)
                        coroutineScope.launch {
                            Api.delList(
                                itemView.findViewById<TextView>(R.id.listName).text.toString(),
                                data.getHash() ?: ""
                            )
                        }
                    } else {
                        toastAlerter("Vérifiez votre connexion internet")
                    }
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