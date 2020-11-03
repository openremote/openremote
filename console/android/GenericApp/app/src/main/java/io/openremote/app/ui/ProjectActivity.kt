package io.openremote.app.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.openremote.app.R
import io.openremote.app.network.ApiManager
import kotlinx.android.synthetic.main.activity_project.*

class ProjectActivity : Activity() {
    var sharedPreferences: SharedPreferences? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        realm_input.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                if (!project_input.text.isNullOrBlank() && !realm_input.text.isNullOrBlank()) {
                    requestAppConfig(project_input.text.toString(), realm_input.text.toString())
                }
                true
            } else {
                false
            }
        }

        connect_button.setOnClickListener {
            if (!project_input.text.isNullOrBlank() && !realm_input.text.isNullOrBlank()) {
                requestAppConfig(project_input.text.toString(), realm_input.text.toString())
            }
        }
    }

    private fun requestAppConfig(project: String, realm: String) {
        progressBar.visibility = View.VISIBLE
        val apiManager = ApiManager("https://${project}.openremote.io/api/${realm}")
        apiManager.getAppConfig { statusCode, appConfig, error ->
            progressBar.visibility = View.INVISIBLE
            if (statusCode in 200..299) {
                sharedPreferences?.edit()?.putString("project", project)?.apply()
                sharedPreferences?.edit()?.putString("realm", realm)?.apply()
                val intent = Intent(this@ProjectActivity, MainActivity::class.java)
                intent.putExtra(MainActivity.APP_CONFIG_KEY, jacksonObjectMapper().writeValueAsString(appConfig))
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Error occurred getting app config. Check your input and try again",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}