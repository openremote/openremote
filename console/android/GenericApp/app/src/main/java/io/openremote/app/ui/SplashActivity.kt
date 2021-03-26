package io.openremote.app.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.openremote.app.databinding.ActivitySplashBinding
import io.openremote.app.network.ApiManager
import io.openremote.app.ui.MainActivity.Companion.APP_CONFIG_KEY

class SplashActivity : Activity() {

    private lateinit var binding: ActivitySplashBinding

    var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val project = sharedPreferences!!.getString("project", null)
        val realm = sharedPreferences!!.getString("realm", null)

        if (!project.isNullOrBlank() && !realm.isNullOrBlank()) {
            val apiManager = ApiManager("https://${project}.openremote.io/api/$realm")
            apiManager.getAppConfig { statusCode, appConfig, error ->
                if (statusCode in 200..299) {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    intent.putExtra(APP_CONFIG_KEY, jacksonObjectMapper().writeValueAsString(appConfig))
                    startActivity(intent)
                    finish()
                } else {
                    startActivity(Intent(this@SplashActivity, ProjectActivity::class.java))
                    finish()
                }
            }
        } else {
            startActivity(Intent(this@SplashActivity, ProjectActivity::class.java))
            finish()
        }
    }
}