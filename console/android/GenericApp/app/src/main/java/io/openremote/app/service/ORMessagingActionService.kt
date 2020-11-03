package io.openremote.app.service

import android.app.IntentService
import android.content.Intent
import android.app.NotificationManager
import android.net.Uri
import io.openremote.app.R
import android.text.TextUtils
import io.openremote.app.ui.MainActivity

class ORMessagingActionService : IntentService("org.openremote.android.ORMessagingActionService") {
    private var notificationResource: NotificationResource? = null
    override fun onCreate() {
        super.onCreate()
        notificationResource = NotificationResource(applicationContext)
    }

    override fun onHandleIntent(intent: Intent?) {
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        this.sendBroadcast(it)
        val notificationId = intent!!.getLongExtra("notificationId", 0L)
        val acknowledgement = intent.getStringExtra("acknowledgement")
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(java.lang.Long.hashCode(notificationId))
        val consoleId = getSharedPreferences(
            applicationContext.getString(R.string.OR_CONSOLE_NAME),
            MODE_PRIVATE
        ).getString("consoleId", "")
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
                    val activityIntent = Intent(applicationContext, MainActivity::class.java)
                    activityIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    activityIntent.putExtra("appUrl", appUrl)
                    startActivity(activityIntent)
                }
            }
        }
    }
}