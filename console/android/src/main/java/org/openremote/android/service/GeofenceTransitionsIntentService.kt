package org.openremote.android.service

import android.app.IntentService
import android.content.Intent
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.net.HttpURLConnection
import java.net.URL
import java.util.logging.Logger

class GeofenceTransitionsIntentService : IntentService("or-geofence") {

    private val LOG = Logger.getLogger(GeofenceTransitionsIntentService::class.java.name)

    override fun onHandleIntent(intent: Intent?) {

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent.hasError()) {
            LOG.warning("Error handling geofence event")
            return
        }

        LOG.fine("Geofence '" + geofencingEvent.geofenceTransition + "' occurred")

        val baseUrl = intent!!.getStringExtra(GeofenceProvider.baseUrlKey)
        val geofenceTransition = geofencingEvent.geofenceTransition
        val geofence = geofencingEvent.triggeringGeofences.first()
        val postUrl = GeofenceProvider.geoPostUrls[geofence.requestId]

        val url = URL("$baseUrl$postUrl")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Content-Type", "application/json")
        connection.requestMethod = "POST"
        connection.connectTimeout = 10000
        connection.doInput = false

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

            val postJson = hashMapOf(
                    "objectValue" to hashMapOf(
                            "type" to "Point",
                            "coordinates" to arrayOf(geofencingEvent.triggeringLocation.longitude, geofencingEvent.triggeringLocation.latitude)
                    )
            )

            connection.doOutput = true
            connection.setChunkedStreamingMode(0)
            connection.outputStream
            ObjectMapper().writeValue(connection.outputStream, postJson)
        }

        try {
            connection.outputStream.flush()
            val responseCode = connection.responseCode
            LOG.fine("Location posted to server '" + postUrl + "': response=" + responseCode);
        } catch (exception: Exception) {
            print(exception)
        } finally {
            connection.disconnect()
        }
    }
}