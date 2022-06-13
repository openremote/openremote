package io.openremote.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.openremote.app.R
import io.openremote.app.databinding.ActivityProjectWizardBinding
import io.openremote.app.model.ProjectItem
import io.openremote.app.util.Constants
import io.openremote.orlib.ORConstants
import io.openremote.app.network.ApiManager
import io.openremote.orlib.ui.OrMainActivity

class ProjectWizardActivity : FragmentActivity() {

    lateinit var binding: ActivityProjectWizardBinding
    lateinit var apiManager: ApiManager

    var host: String? = null
    var app: String? = null
    var realm: String? = null
    var consoleProviders: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProjectWizardBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, HostSelectionFragment.newInstance())
            .addToBackStack(TAG)
            .commit()
    }

    fun goToMainActivity(
        appName: String = "manager",
        realm: String? = null,
        consoleProviders: List<String>? = null
    ) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val projectItems = preferences.getString(Constants.PROJECT_LIST, null)?.let {
            jacksonObjectMapper().readValue<List<ProjectItem>>(it).toMutableList()
        } ?: mutableListOf()

        val intent = Intent(this, OrMainActivity::class.java)
        var url = if (realm != null) "$host/${appName}/?realm=$realm" else "$host/$appName"
        if (consoleProviders?.any() == true) {
            url = if (url.contains("?")) url.plus("&") else url.plus("?")
            url = url.plus("consoleProviders=${consoleProviders.joinToString(" ")}")
        }

        projectItems.add(ProjectItem(host!!, appName, realm, url))
        preferences
            .edit()
            .putString(Constants.PROJECT_LIST, jacksonObjectMapper().writeValueAsString(projectItems))
            .apply()

        intent.putExtra(ORConstants.BASE_URL_KEY, url)
        runOnUiThread {
            startActivity(intent)
            finish()
        }
    }

    fun showToastMessage(message: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        const val TAG = "ProjectWizardActivity"
    }
}
