package io.openremote.app.ui

import android.R
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.preference.PreferenceManager
import io.openremote.app.databinding.ActivityProjectBinding
import io.openremote.orlib.ORConstants
import io.openremote.orlib.models.ORAppInfo
import io.openremote.orlib.models.ORConsoleConfig
import io.openremote.orlib.network.ApiManager
import io.openremote.orlib.ui.OrMainActivity

class ProjectActivity : Activity() {

    private lateinit var binding: ActivityProjectBinding
    private lateinit var sharedPreferences: SharedPreferences

    private var host: String? = null
    var app: String? = null
    var realm: String? = null
    private var consoleProviders: List<String>? = null

    lateinit var appArrayAdapter: ArrayAdapter<String>
    lateinit var realmArrayAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        binding.hostInput.doAfterTextChanged {
            app = null
            realm = null
            consoleProviders = null
            binding.appSpinner.visibility = View.GONE
            binding.appInputLayout.visibility = View.GONE
            binding.realmSpinner.visibility = View.GONE
            binding.realmInputLayout.visibility = View.GONE
        }

        binding.connectButton.setOnClickListener {
            if (app == null && !binding.hostInput.text.isNullOrBlank()) {
                connectToHost(binding.hostInput.text.toString())
            } else if (realm == null && (binding.realmSpinner.isVisible || binding.realmInputLayout.isVisible)) {
                showToastMessage("Please specify a realm")
            } else {
                goToMainActivity(app!!, realm, consoleProviders)
            }
        }

        binding.appSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                app = appArrayAdapter.getItem(position)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                app = null
            }
        }

        binding.realmSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                realm = realmArrayAdapter.getItem(position)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                realm = null
            }
        }

        binding.realmInput.doAfterTextChanged {
            realm = it.toString()
        }
    }

    private fun connectToHost(project: String) {
        binding.progressBar.visibility = View.VISIBLE
        host = if (URLUtil.isValidUrl(project)) project else "https://${project}.openremote.app/"
        val url =
            if (URLUtil.isValidUrl(project)) project.plus("/api/master") else "https://${project}.openremote.app/api/master"
        val apiManager = ApiManager(url)
        apiManager.getConsoleConfig { statusCode, consoleConfig, error ->
            when (statusCode) {
                in 200..299 -> {
                    consoleConfig?.let { config ->
                        if (config.app != null) {
                            app = config.app
                            val appInfo = config.apps?.get(config.app)
                            if (appInfo != null) {
                                binding.progressBar.visibility = View.INVISIBLE
                                processAppInfo(config, appInfo)
                            } else {
                                apiManager.getAppInfos { statusCode, appInfoMap, error ->
                                    binding.progressBar.visibility = View.INVISIBLE
                                    when (statusCode) {
                                        in 200..299 -> {
                                            processAppInfo(config, appInfoMap?.get(config.app!!))
                                        }
                                        404 -> { //no appconfig
                                            goToMainActivity(app!!, "master", consoleProviders)
                                        }
                                        else -> {
                                            showToastMessage("Error getting app info. Check app name.")
                                        }
                                    }
                                }
                            }
                        } else if (config.showAppTextInput) {
                            binding.progressBar.visibility = View.INVISIBLE
                            binding.appInputLayout.visibility = View.VISIBLE
                        } else if (config.allowedApps?.any() == true) {
                            binding.progressBar.visibility = View.INVISIBLE
                            appArrayAdapter = ArrayAdapter(
                                this,
                                R.layout.simple_spinner_dropdown_item,
                                config.allowedApps!!
                            )
                            binding.appSpinner.adapter = realmArrayAdapter
                            binding.appSpinner.visibility = View.VISIBLE
                        } else {
                            binding.progressBar.visibility = View.INVISIBLE
                            showToastMessage("Console config");
                        }
                    }
                }
                404 -> { //no console_config.json
                    apiManager.getAppInfos { statusCode, appInfoMap, error ->
                        binding.progressBar.visibility = View.INVISIBLE
                        when (statusCode) {
                            in 200..299 -> {
                                appArrayAdapter = ArrayAdapter(
                                    this,
                                    R.layout.simple_spinner_dropdown_item,
                                    appInfoMap!!.keys.toList()
                                )
                                binding.appSpinner.adapter = realmArrayAdapter
                                binding.appSpinner.visibility = View.VISIBLE
                            }
                            else -> {
                                goToMainActivity()
                            }
                        }
                    }
                }
                else -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    showToastMessage("Couldn't reach host");
                }
            }
        }
    }

    private fun processAppInfo(config: ORConsoleConfig, appInfo: ORAppInfo?) {
        if (appInfo == null) {
            if (app != null) goToMainActivity(app!!) else goToMainActivity()
        } else {
            consoleProviders = appInfo.providers
            if (appInfo.consoleAppIncompatible) {
                showToastMessage("Console app isn't compatible with the Generic OR app");
            } else if (app != null) {
                goToMainActivity(app!!, consoleProviders = appInfo.providers)
            } else {
                if (config.showRealmTextInput) {
                    binding.realmInputLayout.visibility = View.VISIBLE
                } else if (appInfo.realms.any()) {
                    realmArrayAdapter = ArrayAdapter(
                        this,
                        R.layout.simple_spinner_dropdown_item,
                        appInfo.realms
                    )
                    binding.realmSpinner.adapter = realmArrayAdapter
                    binding.realmSpinner.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun goToMainActivity(
        appName: String = "manager",
        realm: String? = null,
        consoleProviders: List<String>? = null
    ) {
        val intent = Intent(this@ProjectActivity, OrMainActivity::class.java)
        var url = if (realm != null) "$host/${appName}/?realm=$realm" else "$host/$appName"
        if (consoleProviders?.any() == true) {
            url = if (url.contains("?")) url.plus("&") else url.plus("?")
            url = url.plus("consoleProviders=${consoleProviders.joinToString(" ")}")
        }
        intent.putExtra(ORConstants.BASE_URL_KEY, url)
        runOnUiThread {
            startActivity(intent)
            finish()
        }
    }

    private fun showToastMessage(message: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
