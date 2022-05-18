package io.openremote.orlib.service

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import io.openremote.orlib.ORConstants
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.logging.Logger

class NotificationResource(context: Context) {
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun executeRequest(httpMethod: String, appUrl: String, data: String?) {
        Executors.newCachedThreadPool().execute {
            URL(appUrl)
                .openConnection()
                .let {
                    it as HttpURLConnection
                }.apply {
                    requestMethod = httpMethod
                    setRequestProperty("Accept", "application/json")

                    if (!data.isNullOrBlank()) {
                        setRequestProperty("Content-Type", "application/json")
                        doOutput = true

                        val outputWriter = outputStream.bufferedWriter()
                        outputWriter.write(data)
                        outputWriter.flush()
                    }
                }
        }
    }

    fun notificationDelivered(notificationId: Long, fcmToken: String?) {
        LOG.info("Notification status update 'delivered': $notificationId")

        val host = sharedPreferences.getString(ORConstants.HOST_KEY, null)
        val realm = sharedPreferences.getString(ORConstants.REALM_KEY, null)

        if (!host.isNullOrBlank() && !realm.isNullOrBlank()) {
            val url = host.plus("/api/${realm}")
            Executors.newCachedThreadPool().execute {
                URL("${url}/notification/${notificationId}/delivered?targetId=$fcmToken")
                    .openConnection()
                    .let {
                        it as HttpURLConnection
                    }.apply {
                        requestMethod = "PUT"
                        setRequestProperty("Accept", "application/json")


                        setRequestProperty("Content-Type", "application/json")
                        doOutput = true
                    }
            }
        }
    }

    fun notificationAcknowledged(
        notificationId: Long,
        fcmToken: String?,
        acknowledgement: String?
    ) {
        val host = sharedPreferences.getString(ORConstants.HOST_KEY, null)
        val realm = sharedPreferences.getString(ORConstants.REALM_KEY, null)

        if (!host.isNullOrBlank() && !realm.isNullOrBlank()) {
            val url = host.plus("/api/${realm}")
            Executors.newCachedThreadPool().execute {
                URL("${url}/notification/${notificationId}/acknowledged?targetId=$fcmToken")
                    .openConnection()
                    .let {
                        it as HttpURLConnection
                    }.apply {
                        requestMethod = "PUT"
                        setRequestProperty("Accept", "application/json")


                        setRequestProperty("Content-Type", "application/json")
                        doOutput = true

                        val outputWriter = outputStream.bufferedWriter()
                        outputWriter.write(
                            "{\"acknowledgement\": \"${acknowledgement ?: ""}\"}"
                        )
                        outputWriter.flush()
                    }
            }
        }
    }

    companion object {
        private val LOG = Logger.getLogger(
            NotificationResource::class.java.name
        )
    }
}
