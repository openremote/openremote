package io.openremote.app.service

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.location.*
import io.openremote.app.ui.MainActivity
import io.openremote.app.R
import java.net.URL
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.schedule

class GeofenceProvider(val context: Context) : ActivityCompat.OnRequestPermissionsResultCallback {

    interface GeofenceCallback {
        fun accept(responseData: Map<String, Any>)
    }

    class GeofenceDefinition {
        lateinit var id: String
        var lat: Double = 0.0
        var lng: Double = 0.0
        var radius: Float = 0.0F
        lateinit var httpMethod: String
        lateinit var url: String

        override fun toString(): String {
            return "GeofenceDefinition(id='$id', lat=$lat, lng=$lng, radius=$radius, httpMethod='$httpMethod', url='$url')"
        }
    }

    companion object {
        val locationPermissionAskedKey = "LocationPermissionAsked"
        val baseUrlKey = "baseUrl"
        val consoleIdKey = MainActivity.CONSOLE_ID_KEY
        val geofencesKey = "geofences"
        val geofenceDisabledKey = "geofenceDisabled"
        val locationReponseCode = 101

        val JSON = ObjectMapper()
        val LOG = Logger.getLogger(GeofenceProvider::class.java.name)

        fun getGeofences(context: Context): Array<GeofenceDefinition> {
            val sharedPreferences = context.getSharedPreferences(context.getString(R.string.OR_CONSOLE_NAME), Context.MODE_PRIVATE)
            val existingGeofencesJson = sharedPreferences.getString(geofencesKey, null)
            var geofences: Array<GeofenceDefinition> = arrayOf()
            if (existingGeofencesJson != null) {
                geofences = JSON.readValue(existingGeofencesJson, Array<GeofenceDefinition>::class.java)
            }

            LOG.info("Found geofences=${geofences.size}")
            return geofences
        }

        fun startGeofences(context: Context) {

            LOG.info("Starting geofences")

            val geofences = getGeofences(context)
            val baseUrl = getBaseUrl(context)
            if (geofences.isEmpty() || baseUrl == null) {
                return
            }


            val intent = getGeofencePendingIntent(context, baseUrl)
            val geofencingClient = LocationServices.getGeofencingClient(context)

            geofences.forEach {
                addGeofence(geofencingClient, intent, it)
            }
        }

        @SuppressLint("MissingPermission")
        private fun addGeofence(geofencingClient: GeofencingClient, geofencePendingIntent: PendingIntent, geofenceDefinition: GeofenceDefinition) {

            LOG.info("Adding geofence: $geofenceDefinition")

            val geofence = Geofence.Builder()
                    .setRequestId(geofenceDefinition.id)
                    .setCircularRegion(geofenceDefinition.lat, geofenceDefinition.lng, geofenceDefinition.radius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(5000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()

            val geofencingRequest = GeofencingRequest.Builder()
                    .addGeofence(geofence)
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .build()

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    LOG.info("Added geofence: ${geofenceDefinition.id}")
                }

                addOnFailureListener { exception ->
                    LOG.log(Level.SEVERE, "Add geofence failed: ${geofenceDefinition.id}", exception)
                }
            }
        }

        private fun getGeofencePendingIntent(context: Context, baseURL: String): PendingIntent {
            val intent = Intent(context, GeofenceTransitionsIntentService::class.java)
            intent.putExtra(baseUrlKey, baseURL)
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
            // addGeofences() and removeGeofences().
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private fun getBaseUrl(context: Context): String? {
            val sharedPreferences = context.getSharedPreferences(context.getString(R.string.OR_CONSOLE_NAME), Context.MODE_PRIVATE)
            return sharedPreferences.getString(baseUrlKey, null)
        }

        private fun getConsoleId(context: Context): String? {
            val sharedPreferences = context.getSharedPreferences(context.getString(R.string.OR_CONSOLE_NAME), Context.MODE_PRIVATE)
            return sharedPreferences.getString(consoleIdKey, null)
        }
    }

    val version = "ORConsole"
    val geofenceFetchEndpoint = "rules/geofences/"

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    private val locationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val locationUpdateCallback: LocationCallback by lazy {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let {
                    locationClient.removeLocationUpdates(this)
                    locationCallback?.accept(hashMapOf(
                            "action" to "GET_LOCATION",
                            "provider" to "geofence",
                            "data" to hashMapOf(
                                    "latitude" to it.latitude,
                                    "longitude" to it.longitude
                            )
                    ))
                    locationCallback = null
                }
            }
        }
    }

    private lateinit var locationRequest: LocationRequest

    private var enableCallback: GeofenceCallback? = null
    private var locationCallback: GeofenceCallback? = null

    fun initialize(): Map<String, Any> {
        val sharedPreferences = context.getSharedPreferences(context.getString(R.string.OR_CONSOLE_NAME), Context.MODE_PRIVATE)

        return hashMapOf(
                "action" to "PROVIDER_INIT",
                "provider" to "geofence",
                "version" to version,
                "requiresPermission" to true,
                "hasPermission" to (ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED),
                "success" to true,
                "enabled" to false, // Always require enabling to ensure geofences are refresh at startup
                "disabled" to sharedPreferences.contains(geofenceDisabledKey)
        )
    }

    @SuppressLint("MissingPermission")
    fun enable(activity: Activity, baseUrl: String, consoleId: String, callback: GeofenceCallback) {
        val sharedPreferences = context.getSharedPreferences(context.getString(R.string.OR_CONSOLE_NAME), Context.MODE_PRIVATE)

        sharedPreferences.edit()
                .putString(baseUrlKey, baseUrl)
                .putString(consoleIdKey, consoleId)
                .remove(geofenceDisabledKey)
                .apply()

        val hasPermission = when(android.os.Build.VERSION.SDK_INT)  {
            android.os.Build.VERSION_CODES.Q -> {
                ContextCompat.checkSelfPermission(
                    context,
                    ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        if (!hasPermission && !sharedPreferences.getBoolean(
                locationPermissionAskedKey, false)) {
            sharedPreferences.edit().putBoolean(locationPermissionAskedKey, true).apply()
            enableCallback = callback
            registerPermissions(activity)
            return
        }

        return onEnable(callback)
    }

    fun disable() {
        val sharedPreferences = context.getSharedPreferences(context.getString(R.string.OR_CONSOLE_NAME), Context.MODE_PRIVATE)

        removeGeofences(getGeofences(context))

        sharedPreferences.edit()
                .remove(baseUrlKey)
                .remove(consoleIdKey)
                .remove(geofencesKey)
                .putBoolean(geofenceDisabledKey, true)
                .apply()

        //context.stopService(Intent(context, LocationService::class.java))
    }

    @SuppressLint("MissingPermission")
    fun getLocation(activity: Activity, callback: GeofenceCallback?) {
        locationCallback = callback


        val hasPermission = ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.numUpdates = 1

            locationClient.requestLocationUpdates(locationRequest, locationUpdateCallback, null)
        } else {
            registerPermissions(activity)
            locationCallback?.accept(hashMapOf(
                    "action" to "GET_LOCATION",
                    "provider" to "geofence",
                    "data" to hashMapOf<String, Any>()
            ))
            locationCallback = null
        }
    }

    fun refreshGeofences() {
        LOG.info("Refresh geofences")

        val sharedPreferences = context.getSharedPreferences(context.getString(R.string.OR_CONSOLE_NAME), Context.MODE_PRIVATE)
        val url = "${getBaseUrl(context)}/$geofenceFetchEndpoint${getConsoleId(context)}"
        LOG.info("Fetching geofences from server: ${url}")

        try {
            val geofencesJson = URL(url).readText()
            val geofences = JSON.readValue(geofencesJson, Array<GeofenceDefinition>::class.java)

            LOG.info("Fetched geofences=${geofences.size}")

            // Remove previous fences that no longer exist
            val oldFences = getGeofences(context)
            val remainingFences: ArrayList<GeofenceDefinition> = arrayListOf()

            oldFences.forEach { oldFence ->
                if (geofences.none { Objects.equals(it.id, oldFence.id) }) {
                    LOG.info("Geofence now obsolete: $oldFence")
                    removeGeofence(oldFence.id)
                } else {
                    remainingFences.add(oldFence)
                    LOG.info("Geofence unchanged: $oldFence")
                }
            }

            geofences.forEach { geofence ->
                if (remainingFences.none { Objects.equals(it.id, geofence.id) }) {
                    addGeofence(geofencingClient, getGeofencePendingIntent(context, getBaseUrl(context).orEmpty()), geofence)
                }
            }

            sharedPreferences.edit()
                    .putString(geofencesKey, geofencesJson)
                    .apply()
        } catch (e: Exception) {
            LOG.log(Level.SEVERE, "Failed to refresh geofences", e)
        }
    }

    private fun onEnable(callback: GeofenceCallback?) {

        LOG.info("Enabling geofence provider")

        Thread {

            val hasPermission = when(android.os.Build.VERSION.SDK_INT)  {
                android.os.Build.VERSION_CODES.Q -> {
                    ContextCompat.checkSelfPermission(
                        context,
                        ACCESS_BACKGROUND_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                } else -> {
                    ContextCompat.checkSelfPermission(
                        context,
                        ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }

            callback?.accept(hashMapOf(
                    "action" to "PROVIDER_ENABLE",
                    "provider" to "geofence",
                    "hasPermission" to hasPermission,
                    "success" to true
            ))

            if (hasPermission) {
                LOG.info("Has permission so fetching geofences")

                if (getGeofences(context).isEmpty()) {
                    // Could be first time getting geofences so wait a few seconds for backend to catch up
                    Timer().schedule(10000) {
                        refreshGeofences()
                    }
                } else {
                    refreshGeofences()
                }
            }
        }.start()
    }

    private fun registerPermissions(activity: Activity) {
        LOG.info("Requesting geofence permissions")
        val permissions = mutableListOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissions.add(ACCESS_BACKGROUND_LOCATION)
        }
        ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), locationReponseCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (enableCallback != null) {
            onEnable(enableCallback!!)
            enableCallback = null
        }
    }

    private fun removeGeofence(id: String) {
        geofencingClient.removeGeofences(listOf(id)).run {
            addOnSuccessListener {
                LOG.info("Removed geofence: $id")
            }

            addOnFailureListener { exception ->
                LOG.log(Level.SEVERE, "Failed to remove geofence: $id", exception)
            }
        }
    }

    private fun removeGeofences(geofences: Array<GeofenceDefinition>) {
        removeGeofences(geofences.map { it.id })
    }

    private fun removeGeofences(ids: List<String>) {
        if (ids.isEmpty()) {
            return
        }

        LOG.info("Removing existing geofences: $ids")

        geofencingClient.removeGeofences(ids).run {
            addOnSuccessListener {
                LOG.info("Geofences removed")
            }

            addOnFailureListener { exception ->
                LOG.log(Level.SEVERE, "Geofences remove failed", exception)
            }
        }
    }
}