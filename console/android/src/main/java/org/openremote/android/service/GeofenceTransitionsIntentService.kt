package org.openremote.android.service

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
    private var queuedLocations : MutableList<Pair<GeofenceProvider.GeofenceDefinition, String?>> = mutableListOf()
    private var locationTimer : Timer = Timer()
    private var nullLocation : GeofenceProvider.GeofenceDefinition? = null

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

        geofencingEvent.triggeringGeofences.forEach { geofence ->
            val geofenceDefinition = geofenceDefinitions.firstOrNull { it.id == geofence.requestId }

            if (geofenceDefinition != null) {

                LOG.info("Triggered geofence: id=${geofenceDefinition.id}")
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
            }
        }
    }

    @Synchronized
    fun queueSendLocation(geofenceDefinition : GeofenceProvider.GeofenceDefinition, locationJson : String?) {
        var scheduleSend = false

        if (locationJson == null) {
            if (nullLocation == null) {
                nullLocation = geofenceDefinition
                scheduleSend = true
            }
        } else {
            scheduleSend = queuedLocations.isEmpty()
            queuedLocations.add(Pair(geofenceDefinition, locationJson))
        }

        if (scheduleSend) {
            LOG.info("Schedule send location")
            locationTimer.schedule(2000) {
                doSendLocation()
            }
        }
    }

    @Synchronized
    fun doSendLocation() {

        LOG.info("Do send location")

        if (nullLocation != null) {
            sendLocation(nullLocation!!, null)
            nullLocation = null
        } else {
            val geofenceAndLocation = queuedLocations.removeAt(0)
            sendLocation(geofenceAndLocation.first, geofenceAndLocation.second)
        }

        if (queuedLocations.isNotEmpty()) {
            // Schedule another send
            locationTimer.schedule(3000) {
                doSendLocation()
            }
        }
    }

    @Synchronized
    fun sendLocation(geofenceDefinition : GeofenceProvider.GeofenceDefinition, locationJson : String?) {
        Thread {
            val url = URL(geofenceDefinition.url)
            val connection = url.openConnection() as HttpURLConnection

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