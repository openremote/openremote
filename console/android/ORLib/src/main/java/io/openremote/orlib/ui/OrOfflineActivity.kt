package io.openremote.orlib.ui

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import io.openremote.orlib.R
import io.openremote.orlib.service.ConnectivityChangeReceiver

open class OrOfflineActivity : ComponentActivity() {

    private val connectivityChangeReceiver: ConnectivityChangeReceiver =
        ConnectivityChangeReceiver(onConnectivityChanged = ::onConnectivityChanged)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(connectivityChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(connectivityChangeReceiver)
    }

    private fun onConnectivityChanged(isConnected: Boolean) {
        if (isConnected) {
            finish()
        }
    }
}
