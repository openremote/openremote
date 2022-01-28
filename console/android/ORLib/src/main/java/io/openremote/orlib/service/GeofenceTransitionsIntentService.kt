package io.openremote.orlib.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Pair
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.schedule

class GeofenceTransitionsIntentService : BroadcastReceiver() {

    private val LOG = Logger.getLogger(GeofenceTransitionsIntentService::class.java.name)
    private var exitedLocation : Pair<GeofenceProvider.GeofenceDefinition, String?>? = null
    private var enteredLocation : Pair<GeofenceProvider.GeofenceDefinition, String?>? = null
    private var locationTimer : Timer = Timer()
    private var sendQueued = false

    override fun onReceive(context: Context?, intent: Intent?) {

        val geofencingEvent = GeofencingEvent.fromIntent(intent!!)

        if (geofencingEvent.hasError()) {
            LOG.warning("Geofence event error : ${geofencingEvent.errorCode}")
            return
        }

        val geofenceDefinitions = GeofenceProvider.getGeofences(context!!)

        if (geofenceDefinitions.isEmpty()) {
            LOG.fine("No stored geofence definitions so ignoring triggered geofence")
            return
        }

        val baseUrl = intent.getStringExtra(GeofenceProvider.baseUrlKey)
        val geofenceTransition = geofencingEvent.geofenceTransition
        val trans = if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) "ENTER" else "EXIT"

        geofencingEvent.triggeringGeofences.forEach { geofence ->
            val geofenceDefinition = geofenceDefinitions.firstOrNull { it.id == geofence.requestId }

            if (geofenceDefinition != null) {

                LOG.info("Triggered geofence '$trans': $geofenceDefinition")

                geofenceDefinition.url = baseUrl + geofenceDefinition.url

                val locationJson = when (geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        val location = hashMapOf(
                                "type" to "Point",
                                "coordinates" to arrayOf(geofenceDefinition.lng, geofenceDefinition.lat)
                        )
                        ObjectMapper().writeValueAsString(location)
                    }
                    else -> {
                        null
                    }
                }

                // Android often triggers an exit on one fence at the same time as triggering an enter
                // on another so we queue the sends to allow the server time to process the events
                queueSendLocation(geofenceDefinition, locationJson)
            } else {
                LOG.info("Triggered geofence '$trans': unknown")
            }
        }
    }

    @Synchronized
    fun queueSendLocation(geofenceDefinition : GeofenceProvider.GeofenceDefinition, locationJson : String?) {

        if (locationJson == null) {
            exitedLocation = Pair(geofenceDefinition, null)

            // If exit is for same geofence as queued enter then remove enter to avoid incorrectly setting location
            if (enteredLocation != null && enteredLocation!!.first.id == geofenceDefinition.id) {
                enteredLocation = null
            }
        } else {
            enteredLocation = Pair(geofenceDefinition, locationJson)
        }

        if (!sendQueued) {
            sendQueued = true
            LOG.info("Schedule send location")
            locationTimer.schedule(2000) {
                doSendLocation()
            }
        }
    }

    @Synchronized
    fun doSendLocation() {

        LOG.info("Do send location")
        var success = false

        if (exitedLocation != null) {
            if (sendLocation(exitedLocation!!.first, exitedLocation!!.second)) {
                exitedLocation = null
                success = true
            }
        } else if (enteredLocation != null) {
            if (sendLocation(enteredLocation!!.first, enteredLocation!!.second)) {
                enteredLocation = null
                success = true
            }
        }

        if (exitedLocation != null || enteredLocation != null) {

            if (!success) {
                LOG.info("Send failed so re-scheduling")
            } else {
                LOG.info("More locations to send so scheduling another run")
            }

            // Schedule another send
            val delay = if (success) 5000L else 10000L
            locationTimer.schedule(delay) {
                doSendLocation()
            }
        } else {
            sendQueued = false
        }
    }

    @Synchronized
    fun sendLocation(geofenceDefinition : GeofenceProvider.GeofenceDefinition, locationJson : String?): Boolean {
        val url = URL(geofenceDefinition.url)
        val connection = url.openConnection() as HttpURLConnection
        var success = false

        try {
            connection.setRequestProperty("Content-Type", "application/json")
            connection.requestMethod = geofenceDefinition.httpMethod
            connection.connectTimeout = 10000
            connection.doInput = false
            connection.doOutput = true
            connection.setChunkedStreamingMode(0)

            if (locationJson == null) {
                LOG.info("Sending location 'null' to server: HTTP ${geofenceDefinition.httpMethod} $url")
                connection.outputStream.write("null".toByteArray(Charsets.UTF_8))
            } else {
                LOG.info("Sending location 'lat=${geofenceDefinition.lat}/lng=${geofenceDefinition.lng}' to server: HTTP ${geofenceDefinition.httpMethod} $url")
                connection.outputStream.write(locationJson.toByteArray(Charsets.UTF_8))
            }

            connection.outputStream.flush()
            val responseCode = connection.responseCode
            success = responseCode == 204
            LOG.info("Send location success: response=$responseCode")

        } catch (exception: Exception) {
            LOG.log(Level.SEVERE, "Send location failed", exception)
            print(exception)
        } finally {
            connection.disconnect()
        }

        return success
    }
}