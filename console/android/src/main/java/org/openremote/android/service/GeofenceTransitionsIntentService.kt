package org.openremote.android.service

import android.app.IntentService
import android.content.ContentValues.TAG
import android.content.Intent
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.text.Charsets.UTF_8

class GeofenceTransitionsIntentService : IntentService("or-geofence") {

    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Error handling geofence event")
            return
        }

        val baseUrl = intent!!.getStringExtra(GeofenceProvider.baseUrlKey)
        val geofenceTransition = geofencingEvent.geofenceTransition
        val geofence = geofencingEvent.triggeringGeofences.first()
        val postUrl = GeofenceProvider.geoPostUrls[geofence.requestId]

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

            val postJson = {
                "objectValue" to {
                    "type" to "Point"
                    "coordinates" to arrayOf(geofencingEvent.triggeringLocation.longitude, geofencingEvent.triggeringLocation.latitude)
                }
            }

            val url = URL("$baseUrl$postUrl")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 100000
            connection.doOutput = true

            val postData: ByteArray = ObjectMapper().writeValueAsString(postJson).toByteArray(UTF_8)

            connection.setRequestProperty("charset", "utf-8")
            connection.setRequestProperty("Content-lenght", postData.size.toString())
            connection.setRequestProperty("Content-Type", "application/json")

            try {
                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.write(postData)
                outputStream.flush()
            } catch (exception: Exception) {
                print(exception)
            }
        }

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val postJson = hashMapOf<String, Any>()

            val url = URL("$baseUrl$postUrl")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 100000
            connection.doOutput = true

            val postData: ByteArray = ObjectMapper().writeValueAsString(postJson).toByteArray(UTF_8)

            connection.setRequestProperty("charset", "utf-8")
            connection.setRequestProperty("Content-lenght", postData.size.toString())
            connection.setRequestProperty("Content-Type", "application/json")

            try {
                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.write(postData)
                outputStream.flush()
            } catch (exception: Exception) {
                print(exception)
            }
        }
    }
}