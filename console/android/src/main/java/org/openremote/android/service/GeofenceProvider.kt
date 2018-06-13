package org.openremote.android.service

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.location.*
import org.openremote.android.R
import java.io.*
import java.net.URL


class GeofenceProvider(val activity: Activity) {

    companion object {
        val baseUrlKey = "baseUrl"
        val consoleIdKey = "consoleId"
        val geoPostUrlsKey = "geoPostUrls"
        var geoPostUrls = hashMapOf<String, List<String>>()
        var locationReponseCode = 101
    }

    val version = "ORConsole"
    val geofenceFetchEndpoint = "rules/geofences/"

    val sharedPreferences = activity.getSharedPreferences(activity.getString(R.string.app_name), Context.MODE_PRIVATE)

    lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent("org.openremote.android.geofence.ACTION_RECEIVE")
        intent.putExtra(baseUrlKey, baseURL)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    var baseURL: String? = null
    var consoleId: String? = null


    fun initialize(): Map<String, Any> {
        geofencingClient = LocationServices.getGeofencingClient(activity)

        var inputStream: ObjectInputStream? = null
        try {
            val file = File(activity.getDir("data", MODE_PRIVATE), geoPostUrlsKey)
            inputStream = ObjectInputStream(FileInputStream(file))
            geoPostUrls = inputStream.readObject() as? HashMap<String, List<String>> ?: hashMapOf()
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

    @SuppressLint("MissingPermission")
    fun enable(baseUrl: String, consoleId: String): Map<String, Any> {
        baseURL = baseUrl
        this.consoleId = consoleId

        sharedPreferences.edit()
                .putString(baseUrlKey, baseURL)
                .putString(consoleIdKey, this.consoleId)
                .apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(Intent(activity, LocationService::class.java))
        } else {
            activity.startService(Intent(activity, LocationService::class.java))
        }

        fetchGeofences()

        return hashMapOf(
                "action" to "PROVIDER_ENABLE",
                "provider" to "geofence",
                "hasPermission" to true,
                "success" to true
        )
    }

    fun disable() {
        sharedPreferences.edit()
                .remove(baseUrlKey)
                .remove(consoleIdKey)
                .apply()
        baseURL = null
        consoleId = null
        activity.stopService(Intent(activity, LocationService::class.java))
        clearAllRegions()
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
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.i("GeofenceProvider", "Geofence added: lat${geofenceDefinition.lat}/lng${geofenceDefinition.lng}/rad${geofenceDefinition.radius}")
                geoPostUrls.put(geofenceDefinition.id, listOf(geofenceDefinition.httpMethod, geofenceDefinition.url))
            }

            addOnFailureListener { exception ->
                Log.e("GeofenceProvider", exception.message, exception)
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
        if(geoPostUrls.keys.any()) {
            geofencingClient.removeGeofences(geoPostUrls.keys.toList()).run {
                addOnSuccessListener {
                    Log.i("GeofenceProvider", "Geofences removed")
                }

                addOnFailureListener { exception ->
                    Log.e("GeofenceProvider", exception.message, exception)
                }
            }
        }
        geoPostUrls = hashMapOf()
    }

    fun refreshGeofences() {
        Thread({
            fetchGeofences()
        }).start()
    }

    protected fun fetchGeofences() {
        val response = URL("$baseURL/$geofenceFetchEndpoint$consoleId").readText()
        val geofences = ObjectMapper().readValue(response, Array<GeofenceDefinition>::class.java)
        clearAllRegions()
        Log.i("fetchGeofences", "${geofences.size} geofences")
        geofences.forEach {
            addGeofence(it)
            geoPostUrls[it.id] = listOf(it.httpMethod, it.url)
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
        var httpMethod: String = ""
        var url: String = ""
    }

    class LocationService : Service() {

        private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
        private lateinit var locationCallback: LocationCallback

        override fun onCreate() {
            super.onCreate()

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    Log.i("GeofenceProvider", "Location received: lat${locationResult.lastLocation.latitude}/lng${locationResult.lastLocation.longitude}")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

            val notificationBuilder = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val channelId = createNotificationChannel()
                    NotificationCompat.Builder(this, channelId)
                }
                else -> {
                    NotificationCompat.Builder(this)
                }
            }

            val notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentTitle("Tracking location")
                    .build()

            notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE or Notification.FLAG_NO_CLEAR

            //Force location updates
            val locationRequest = LocationRequest().apply {
                interval = 60000//1min
                fastestInterval = 10000//10sec
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    null)

            startForeground(101, notification)

            return START_REDELIVER_INTENT
        }

        override fun onDestroy() {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)

            super.onDestroy()
        }

        override fun onBind(intent: Intent?): IBinder? {
            return null
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun createNotificationChannel(): String {
            val channelId = "OR_EIND"
            val channelName = "OR Eindhoven Location Service"
            val channel = NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH)
            channel.lightColor = Color.BLUE
            channel.importance = NotificationManager.IMPORTANCE_NONE
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            service!!.createNotificationChannel(channel)
            return channelId
        }
    }
}