package org.openremote.android.service

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import org.openremote.android.R
import java.io.*
import java.net.URL


class GeofenceProvider(val activity: Activity) {

    companion object {
        val baseUrlKey = "baseUrl"
        val consoleIdKey = "consoleId"
        val geoPostUrlsKey = "geoPostUrls"
        var geoPostUrls = hashMapOf<String, String>()
        var locationReponseCode = 101
    }

    val version = "ORConsole"
    val geofenceFetchEndpoint = "rules/geofences/"

    val sharedPreferences = activity.getSharedPreferences(activity.getString(R.string.app_name), Context.MODE_PRIVATE)

    lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(activity, GeofenceTransitionsIntentService::class.java)
        intent.putExtra(baseUrlKey, baseURL)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getService(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    var baseURL: String = ""
    var consoleId: String = ""


    fun initialize(): Map<String, Any> {
        geofencingClient = LocationServices.getGeofencingClient(activity)

        var inputStream: ObjectInputStream? = null
        try {
            val file = File(activity.getDir("data", MODE_PRIVATE), geoPostUrlsKey)
            inputStream = ObjectInputStream(FileInputStream(file))
            geoPostUrls = inputStream.readObject() as? HashMap<String, String> ?: hashMapOf()
        } catch (e: Exception) {
            geoPostUrls = hashMapOf()
        } finally {
            inputStream?.close()
        }

        return hashMapOf(
                "action" to "PROVIDER_INIT",
                "provider" to "geofence",
                "version" to version,
                "requiresPermission" to true,
                "hasPermission" to true,
                "success" to true
        )
    }

    fun enable(baseUrl: String, consoleId: String): Map<String, Any> {
        baseURL = baseUrl
        this.consoleId = consoleId

        sharedPreferences.edit()
                .putString(baseUrlKey, baseURL)
                .putString(consoleIdKey, this.consoleId)
                .apply()


        fetchGeofences()

        return hashMapOf(
                "action" to "PROVIDER_ENABLE",
                "provider" to "geofence",
                "hasPermission" to true,
                "success" to true
        )
    }

    fun checkPermission(): Boolean {
        // Ask for permission if it wasn't granted yet
        return ContextCompat.checkSelfPermission(activity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun registerPermissions() {
        val permissions = arrayOf(ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(activity, permissions, locationReponseCode)
    }


    @SuppressLint("MissingPermission")
    fun addGeofence(geofenceDefinition: GeofenceDefinition) {
        val geofence = Geofence.Builder()
                .setRequestId(geofenceDefinition.id)
                .setCircularRegion(geofenceDefinition.lat, geofenceDefinition.lng, geofenceDefinition.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

        val geofencingRequest = GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                geoPostUrls.put(geofenceDefinition.id, geofenceDefinition.postUrl)
            }

            addOnFailureListener { exception ->
                print(exception)
            }
        }
    }

    fun removeGeofence(id: String) {
        geofencingClient.removeGeofences(listOf(id)).run {
            addOnSuccessListener {
                geoPostUrls.remove(id)
            }
        }
    }

    fun clearAllRegions() {
        geofencingClient.removeGeofences(geofencePendingIntent)
        geoPostUrls = hashMapOf()
    }

    fun refreshGeofences() {
        fetchGeofences()
    }

    protected fun fetchGeofences() {
        val response = URL("$baseURL/$geofenceFetchEndpoint$consoleId").readText()
        val geofences = ObjectMapper().readValue(response, Array<GeofenceDefinition>::class.java)
        clearAllRegions()
        geofences.forEach {
            addGeofence(it)
            geoPostUrls[it.id] = it.postUrl
        }
        val file = File(activity.getDir("data", MODE_PRIVATE), geoPostUrlsKey)
        val outputStream = ObjectOutputStream(FileOutputStream(file))
        outputStream.writeObject(geoPostUrls)
        outputStream.flush()
        outputStream.close()
    }

    class GeofenceDefinition {
        var id: String = ""
        var lat: Double = 0.0
        var lng: Double = 0.0
        var radius: Float = 0.0F
        var postUrl: String = ""
    }
}