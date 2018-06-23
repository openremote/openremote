package org.openremote.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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

        if (geofencingEvent.hasError()) {
            LOG.warning("Error handling geofence event")
            return
        }

        LOG.fine("Geofence '" + geofencingEvent.geofenceTransition + "' occurred")

        val baseUrl = intent!!.getStringExtra(GeofenceProvider.baseUrlKey)
        val geofenceTransition = geofencingEvent.geofenceTransition
        val geofenceDefinitions = GeofenceProvider.getGeofences(context!!)

        if (geofenceDefinitions.isEmpty()) {
            LOG.fine("No stored geofence definitions so ignoring triggered geofence")
            return
        }

        geofencingEvent.triggeringGeofences.forEach {
            geofence ->
                val geofenceDefinition = geofenceDefinitions.firstOrNull { it.id == geofence.requestId }

                if (geofenceDefinition != null) {
                    val url = URL("$baseUrl${geofenceDefinition.url}")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.requestMethod = geofenceDefinition.httpMethod
                    connection.connectTimeout = 10000
                    connection.doInput = false

                    val postJson = when (geofenceTransition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> {
                            hashMapOf(
                                    "type" to "Point",
                                    "coordinates" to arrayOf(geofencingEvent.triggeringLocation.longitude, geofencingEvent.triggeringLocation.latitude)
                            )
                        }
                        else -> {
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
                            LOG.info("Location posted to server: response=" + responseCode)

                        } catch (exception: Exception) {
                            LOG.log(Level.SEVERE, exception.message, exception)
                            print(exception)
                        } finally {
                            connection.disconnect()
                        }
                    }.start()
                }
            }
    }
}