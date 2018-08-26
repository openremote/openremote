package org.openremote.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.net.HttpURLConnection
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger

class GeofenceTransitionsIntentService : BroadcastReceiver() {

    private val LOG = Logger.getLogger(GeofenceTransitionsIntentService::class.java.name)

    override fun onReceive(context: Context?, intent: Intent?) {

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        LOG.info("Geofence event received: transition=${geofencingEvent.geofenceTransition}")

        if (geofencingEvent.hasError()) {
            LOG.warning("Geofence event error : ${geofencingEvent.errorCode}")
            return
        }

        val geofenceDefinitions = GeofenceProvider.getGeofences(context!!)

        if (geofenceDefinitions.isEmpty()) {
            LOG.fine("No stored geofence definitions so ignoring triggered geofence")
            return
        }

        val baseUrl = intent!!.getStringExtra(GeofenceProvider.baseUrlKey)
        val geofenceTransition = geofencingEvent.geofenceTransition

        geofencingEvent.triggeringGeofences.forEach {
            geofence ->
                val geofenceDefinition = geofenceDefinitions.firstOrNull { it.id == geofence.requestId }

                if (geofenceDefinition != null) {

                    LOG.info("Triggered geofence: id=${geofenceDefinition.id}")

                    val url = URL("$baseUrl${geofenceDefinition.url}")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.requestMethod = geofenceDefinition.httpMethod
                    connection.connectTimeout = 10000
                    connection.doInput = false

                    val postJson = when (geofenceTransition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> {
                            LOG.info("Sending location 'lat=${geofenceDefinition.lat}/lng=${geofenceDefinition.lng}' to server: HTTP ${geofenceDefinition.httpMethod} $url")
                            hashMapOf(
                                    "type" to "Point",
                                    "coordinates" to arrayOf(geofenceDefinition.lng, geofenceDefinition.lat)
                            )
                        }
                        else -> {
                            LOG.info("Sending location 'null' to server: HTTP ${geofenceDefinition.httpMethod} $url")
                            null
                        }
                    }

                    Thread {
                        try {
                            connection.doOutput = true
                            connection.setChunkedStreamingMode(0)
                            connection.outputStream
                            ObjectMapper().writeValue(connection.outputStream, postJson)

                            connection.outputStream.flush()
                            val responseCode = connection.responseCode
                            LOG.info("Send location success: response=$responseCode")

                        } catch (exception: Exception) {
                            LOG.log(Level.SEVERE, "Send location failed", exception)
                            print(exception)
                        } finally {
                            connection.disconnect()
                        }
                    }.start()
                }
            }
    }
}