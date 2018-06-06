package org.openremote.android.service

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import org.openremote.android.R
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.logging.Logger

class GeofenceTransitionsIntentService : IntentService("or-geofence") {

    private val LOG = Logger.getLogger(GeofenceTransitionsIntentService::class.java.name)
    private val restApiResource: RestApiResource by lazy {
        Retrofit.Builder()
                .baseUrl(getString(R.string.OR_BASE_SERVER))
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build().create(RestApiResource::class.java);
    }

    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent.hasError()) {
            Log.w("Geofence", "Error handling geofence event")
            return
        }

        LOG.fine("Geofence '" + geofencingEvent.geofenceTransition + "' occurred")

        val consoleId = intent!!.getStringExtra(GeofenceProvider.consoleIdKey)
        val geofenceTransition = geofencingEvent.geofenceTransition

        val postJson = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                hashMapOf(
                        "type" to "Point",
                        "coordinates" to arrayOf(geofencingEvent.triggeringLocation.longitude, geofencingEvent.triggeringLocation.latitude)
                )
            }
            else -> {
                hashMapOf(
                        "type" to "Point",
                        "coordinates" to arrayOf(0, 0)
                )
            }
        }

        try {
            val response = restApiResource
                    .updateAssetAction(getString(R.string.OR_REALM), null, consoleId, "location", ObjectMapper().writeValueAsString(postJson))
                    .execute()
            Log.i("Geofence", "Location posted to server: response=" + response.code());
        } catch (exception: Exception) {
            Log.e("Geofence", exception.message, exception)
        }
    }
}