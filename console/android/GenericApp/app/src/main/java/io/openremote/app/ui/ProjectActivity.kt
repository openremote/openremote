package io.openremote.app.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.openremote.app.databinding.ActivityProjectBinding
import io.openremote.orlib.network.ApiManager
import io.openremote.orlib.ui.OrMainActivity

class ProjectActivity : Activity() {

    private lateinit var binding: ActivityProjectBinding

    var sharedPreferences: SharedPreferences? = null
    var host: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        binding.realmInput.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                if (!binding.projectInput.text.isNullOrBlank() && !binding.realmInput.text.isNullOrBlank()) {
                    requestAppConfig(binding.projectInput.text.toString(), binding.realmInput.text.toString())
                }
                true
            } else {
                false
            }
        }

        binding.connectButton.setOnClickListener {
            if (!binding.projectInput.text.isNullOrBlank() && !binding.realmInput.text.isNullOrBlank()) {
                requestAppConfig(binding.projectInput.text.toString(), binding.realmInput.text.toString())
            }
        }
    }

    private fun requestAppConfig(project: String, realm: String) {
        binding.progressBar.visibility = View.VISIBLE
        host = if (URLUtil.isValidUrl(project)) project else "https://${project}.openremote.io/"
        val url = if (URLUtil.isValidUrl(project)) project.plus("/api/${realm}") else "https://${project}.openremote.io/api/${realm}"
        val apiManager = ApiManager(url)
        apiManager.getAppConfig(realm) { statusCode, appConfig, error ->
            binding.progressBar.visibility = View.INVISIBLE
            if (statusCode in 200..299) {
                sharedPreferences?.edit()?.putString("host", host)?.apply()
                sharedPreferences?.edit()?.putString("realm", realm)?.apply()
                val intent = Intent(this@ProjectActivity, OrMainActivity::class.java)
                intent.putExtra(OrMainActivity.APP_CONFIG_KEY, jacksonObjectMapper().writeValueAsString(appConfig))
                startActivity(intent)
                finish()
            } else {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Error occurred getting app config. Check your input and try again",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}