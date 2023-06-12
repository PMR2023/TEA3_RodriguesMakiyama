package com.example.TEA3_RodriguesMakiyama.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.TEA3_RodriguesMakiyama.R
import com.example.TEA3_RodriguesMakiyama.api.Api
import com.example.TEA3_RodriguesMakiyama.classes.Data
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SignupActivity : TEAActivity() {
    lateinit var data: Data

    lateinit var pseudoET: EditText
    lateinit var passET: EditText
    lateinit var passConfirmET: EditText
    lateinit var signupButton: Button
    lateinit var warningTV: TextView

    val mContext = this
    private val coroutineScope = CoroutineScope(
        Dispatchers.Main
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        intent?.extras?.getString("data")?.let {
            data = Gson().fromJson(it, Data::class.java)
        }

        pseudoET = findViewById(R.id.pseudoET)
        passET = findViewById(R.id.pass)
        passConfirmET = findViewById(R.id.passConfirm)
        signupButton = findViewById(R.id.buttonSignup)
        warningTV = findViewById(R.id.warningTV)

        // Monitors internet connection
        coroutineScope.launch {
            while(true) {
                if(!Api.checkNetwork(mContext) && signupButton.isClickable) {
                    signupButton.isClickable = false
                    signupButton.setBackgroundColor(Color.rgb(93,93,93))
                } else if(Api.checkNetwork(mContext) && !signupButton.isClickable) {
                    signupButton.isClickable = true
                    signupButton.setBackgroundColor(Color.rgb(103,80,164))
                }
                delay(1000)
            }
        }

        var originalPseudoETBackground = pseudoET.background
        var originalPassETBackground = passET.background
        signupButton.setOnClickListener {
            val pseudo = pseudoET.text.toString()
            val pass = passET.text.toString()
            val passConfirm = passConfirmET.text.toString()

            pseudoET.setTextColor(Color.BLACK)
            passET.setTextColor(Color.BLACK)
            passConfirmET.setTextColor(Color.BLACK)

            pseudoET.background = originalPseudoETBackground
            passET.background = originalPassETBackground
            passConfirmET.background = originalPassETBackground

            warningTV.text = ""

            if(!pseudo.isNullOrEmpty() && !pass.isNullOrEmpty() && !passConfirm.isNullOrEmpty()) {
                coroutineScope.launch {
                    // Check user
                    if(Api.checkUserAvailable(pseudo))
                    {
                        // Check password
                        if(pass == passConfirm) {
                            val response = Api.signup(pseudo, pass)

                            pseudoET.setText("")
                            passET.setText("")
                            passConfirmET.setText("")
                            if(response.success) {
                                toastAlerter("Vous avez été enregistré, veuillez vous connecter !", true)
                            } else {
                                warningTV.text = "Une erreur s'est produite, veuillez réessayer !"
                            }
                        } else {
                            passET.setTextColor(Color.rgb(176,0,32))
                            passET.setBackgroundColor(Color.argb(30,176,0,32))
                            passConfirmET.setTextColor(Color.rgb(176,0,32))
                            passConfirmET.setBackgroundColor(Color.argb(30,176,0,32))
                            warningTV.text = "Les mots de passe ne correspondent pas"
                        }
                    } else {
                        pseudoET.setTextColor(Color.rgb(176,0,32))
                        pseudoET.setBackgroundColor(Color.argb(30,176,0,32))
                        warningTV.text = "Ce pseudo n'est pas disponible"
                    }
                }
            } else {
                warningTV.text = "Veuillez remplir tous les champs"

                if(pseudo.isNullOrEmpty()) {
                    pseudoET.setBackgroundColor(Color.argb(30,176,0,32))
                }
                if(pass.isNullOrEmpty()) {
                    passET.setBackgroundColor(Color.argb(30,176,0,32))
                }
                if(passConfirm.isNullOrEmpty()) {
                    passConfirmET.setBackgroundColor(Color.argb(30,176,0,32))
                }
            }
        }

        pseudoET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                warningTV.text = ""
                pseudoET.setTextColor(Color.BLACK)
                pseudoET.background = originalPseudoETBackground
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val pseudo = pseudoET.text.toString()
                coroutineScope.launch {
                    if(!Api.checkUserAvailable(pseudo)) {
                        pseudoET.setTextColor(Color.rgb(176,0,32))
                        pseudoET.setBackgroundColor(Color.argb(30,176,0,32))
                        warningTV.text = "Ce pseudo n'est pas disponible"
                    }
                }
            }
        })

        // Set up the ActionBar and change its name(optional)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Enregistrement"
    }

    override fun onResume() {
        super.onResume()

        intent?.extras?.getString("data")?.let {
            data = Gson().fromJson(it, Data::class.java)
        }
    }

    override fun onBackPressed() {
        val int = Intent()
        int.putExtra("data", data.toJson())
        setResult(Activity.RESULT_OK, int)

        finish()
    }
}