package io.openremote.orlib.ui

import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import io.openremote.orlib.ORConstants
import io.openremote.orlib.R
import io.openremote.orlib.service.NotificationResource

class NotificationActivity : AppCompatActivity() {
    private var notificationResource: NotificationResource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        notificationResource = NotificationResource(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            this.sendBroadcast(it)
        }
        val notificationId = intent!!.getLongExtra("notificationId", 0L)
        val acknowledgement = intent.getStringExtra("acknowledgement")
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(java.lang.Long.hashCode(notificationId))
        val consoleId = getSharedPreferences(
            applicationContext.getString(R.string.app_name),
            MODE_PRIVATE
        ).getString(ORConstants.CONSOLE_ID_KEY, "")
        if (!TextUtils.isEmpty(consoleId)) {
            notificationResource!!.notificationAcknowledged(
                notificationId,
                consoleId,
                acknowledgement
            )
        }
        val appUrl = intent.getStringExtra("appUrl")
        if (!appUrl.isNullOrBlank()) {
            val openInBrowser = intent.getBooleanExtra("openInBrowser", false)
            val silent = intent.getBooleanExtra("silent", false)
            val data = intent.getStringExtra("data")
            var httpMethod = intent.getStringExtra("httpMethod")
            httpMethod = if (TextUtils.isEmpty(httpMethod)) "GET" else httpMethod

            when {
                openInBrowser -> {
                    // Don't load the app just send straight to browser
                    val i = Intent(Intent.ACTION_VIEW)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.data = Uri.parse(appUrl)
                    startActivity(i)
                }

                silent -> {
                    // Do silent HTTP request
                    notificationResource!!.executeRequest(httpMethod!!, appUrl, data)
                }

                else -> {
                    val pm: PackageManager = packageManager
                    val launchIntent: Intent =
                        pm.getLaunchIntentForPackage(applicationContext.packageName) ?: Intent(
                            applicationContext,
                            OrMainActivity::class.java
                        )
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    launchIntent.putExtra("appUrl", appUrl)
                    startActivity(launchIntent)
                }
            }
        }
    }
}
