package io.openremote.app.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.openremote.app.databinding.ActivitySplashBinding
import io.openremote.orlib.network.ApiManager
import io.openremote.orlib.ui.OrMainActivity
import io.openremote.orlib.ui.OrPrivacyPolicyActivity

class SplashActivity : Activity() {

    private val SHOW_CONDITIONS_REQUEST = 34563

    private lateinit var binding: ActivitySplashBinding

    var sharedPreferences: SharedPreferences? = null
    var host: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val privacyPolicyAccepted = sharedPreferences!!.getBoolean("privacyPolicyAccepted", false)

        if (!privacyPolicyAccepted) {
            val intent = Intent(this@SplashActivity, OrPrivacyPolicyActivity::class.java)
            startActivityForResult(intent, SHOW_CONDITIONS_REQUEST)
        } else {
            startNextActivity()
        }
    }

    private fun startNextActivity() {
        val host = sharedPreferences!!.getString("host", null)
        val realm = sharedPreferences!!.getString("realm", null)

        if (!host.isNullOrBlank() && !realm.isNullOrBlank()) {
            val url = host.plus("/api/${realm}")
            val apiManager = ApiManager(url)
            apiManager.getAppConfig(realm) { statusCode, appConfig, error ->
                if (statusCode in 200..299) {
                    val intent = Intent(this@SplashActivity, OrMainActivity::class.java)
                    intent.putExtra(OrMainActivity.APP_CONFIG_KEY, jacksonObjectMapper().writeValueAsString(appConfig))
                    intent.putExtra(OrMainActivity.BASE_URL_KEY, host)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == SHOW_CONDITIONS_REQUEST) {
            if (resultCode == RESULT_OK) {
                sharedPreferences!!.edit().putBoolean("privacyPolicyAccepted", true).apply()
                startNextActivity()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}