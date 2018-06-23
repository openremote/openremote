package org.openremote.android.service

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import org.openremote.android.R
import java.net.URL

class GeofenceProvider(val context: Context) : ActivityCompat.OnRequestPermissionsResultCallback {

    interface EnableCallback {
        fun accept(responseData: Map<String, Any>)
    }

    class GeofenceDefinition {
        lateinit var id: String
        var lat: Double = 0.0
        var lng: Double = 0.0
        var radius: Float = 0.0F
        lateinit var httpMethod: String
        lateinit var url: String
    }

//    class LocationService : Service() {
//
//        private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
//        private lateinit var locationCallback: LocationCallback
//
//        override fun onCreate() {
//            super.onCreate()
//
//            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
//
//            locationCallback = object : LocationCallback() {
//                override fun onLocationResult(locationResult: LocationResult?) {
//                    locationResult ?: return
//                    Log.i("GeofenceProvider", "Location received: lat${locationResult.lastLocation.latitude}/lng${locationResult.lastLocation.longitude}")
//                }
//            }
//        }
//
//        @SuppressLint("MissingPermission", "NewApi")
//        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//
//            val notificationBuilder = when {
//                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
//                    val channelId = createNotificationChannel()
//                    NotificationCompat.Builder(this, channelId)
//                }
//                else -> {
//                    NotificationCompat.Builder(this)
//                }
//            }
//
//            val notification = notificationBuilder.setOngoing(true)
//                    .setSmallIcon(R.drawable.ic_notification)
//                    .setCategory(Notification.CATEGORY_SERVICE)
//                    .setContentTitle("Tracking location")
//                    .build()
//
//            notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE or Notification.FLAG_NO_CLEAR
//
//            //Force location updates
//            val locationRequest = LocationRequest().apply {
//                interval = 60000//1min
//                fastestInterval = 10000//10sec
//                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//            }
//
//            fusedLocationProviderClient.requestLocationUpdates(locationRequest,
//                    locationCallback,
//                    null)
//
//            startForeground(101, notification)
//
//            return START_REDELIVER_INTENT
//        }
//
//        override fun onDestroy() {
//            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
//
//            super.onDestroy()
//        }
//
//        override fun onBind(intent: Intent?): IBinder? {
//            return null
//        }
//
//        @RequiresApi(Build.VERSION_CODES.O)
//        private fun createNotificationChannel(): String {
//            val channelId = "${BuildConfig.APPLICATION_ID}.OREindhovenLocationService"
//            val channelName = "OR Eindhoven Location Service"
//            val channel = NotificationChannel(channelId,
//                    channelName, NotificationManager.IMPORTANCE_LOW)
//            channel.lightColor = Color.BLUE
//            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
//            channel.setShowBadge(false)
//            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
//            service!!.createNotificationChannel(channel)
//            return channelId
//        }
//    }

    companion object {
        val baseUrlKey = "baseUrl"
        val consoleIdKey = "consoleId"
        val geofencesKey = "geofences"
        var locationReponseCode = 101
        var JSON = ObjectMapper()

        fun getGeofences(context: Context): Array<GeofenceDefinition> {
            val sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
            val existingGeofencesJson = sharedPreferences.getString(geofencesKey, null)
            if (existingGeofencesJson != null) {
                return JSON.readValue(existingGeofencesJson, Array<GeofenceDefinition>::class.java)
            } else {
                return arrayOf()
            }
        }

        fun startGeofences(context: Context) {
            val geofences = getGeofences(context)
            val baseUrl = getBaseUrl(context)
            if (geofences.isEmpty() || baseUrl == null) {
                return
            }

            Log.i("GeofenceProvider", "Starting geofences count=${geofences.size}")
            val intent = getGeofencePendingIntent(context, baseUrl)
            val geofencingClient = LocationServices.getGeofencingClient(context)

            geofences.forEach {
                addGeofence(geofencingClient, intent, it)
            }
        }


        @SuppressLint("MissingPermission")
        private fun addGeofence(geofencingClient: GeofencingClient, geofencePendingIntent: PendingIntent, geofenceDefinition: GeofenceDefinition) {
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
                }

                addOnFailureListener { exception ->
                    Log.e("GeofenceProvider", "Failed to add geofence", exception)
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
            val sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
            return sharedPreferences.getString(baseUrlKey, null)
        }

        private fun getConsoleId(context: Context): String? {
            val sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
            return sharedPreferences.getString(consoleIdKey, null)
        }
    }

    val version = "ORConsole"
    val geofenceFetchEndpoint = "rules/geofences/"

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    var enableCallback: EnableCallback? = null

    fun initialize(): Map<String, Any> {
        return hashMapOf(
                "action" to "PROVIDER_INIT",
                "provider" to "geofence",
                "version" to version,
                "requiresPermission" to true,
                "hasPermission" to (ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED),
                "success" to true
        )
    }

    @SuppressLint("MissingPermission")
    fun enable(activity: Activity, baseUrl: String, consoleId: String, callback: EnableCallback) {
        val sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

        sharedPreferences.edit()
                .putString(baseUrlKey, baseUrl)
                .putString(consoleIdKey, consoleId)
                .apply()

        if(ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            enableCallback = callback
            registerPermissions(activity)
            return
        }

        return onEnable(callback)
    }

    fun disable() {
        val sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

        removeGeofences(getGeofences(context))

        sharedPreferences.edit()
                .remove(baseUrlKey)
                .remove(consoleIdKey)
                .remove(geofencesKey)
                .apply()

        //context.stopService(Intent(context, LocationService::class.java))
    }

    fun refreshGeofences() {
        Log.i("GeofenceProvider", "refreshGeofences")
        onEnable(null)
    }

    private fun onEnable(callback: EnableCallback?) {
        Thread {
            if (ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(Intent(context, LocationService::class.java))
//            } else {
//                context.startService(Intent(context, LocationService::class.java))
//            }

                fetchGeofences()
            }

            callback?.accept(hashMapOf(
                    "action" to "PROVIDER_ENABLE",
                    "provider" to "geofence",
                    "hasPermission" to (ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED),
                    "success" to true
            ))
        }.start()
    }

    private fun registerPermissions(activity: Activity) {
        val permissions = arrayOf(ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(activity, permissions, locationReponseCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (enableCallback != null) {
            onEnable(enableCallback!!)
            enableCallback = null
        }
    }

    private fun fetchGeofences() {
        val sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        val geofencesJson = URL("${getBaseUrl(context)}/$geofenceFetchEndpoint${getConsoleId(context)}").readText()
        val geofences = JSON.readValue(geofencesJson, Array<GeofenceDefinition>::class.java)
        removeGeofences(getGeofences(context))

        Log.i("GeofenceProvider", "fetchGeofences: count=${geofences.size}")

        geofences.forEach {
            addGeofence(geofencingClient, getGeofencePendingIntent(context, getBaseUrl(context).orEmpty()), it)
        }

        sharedPreferences.edit()
                .putString(geofencesKey, geofencesJson)
                .apply()
    }

    private fun removeGeofence(id: String) {
        geofencingClient.removeGeofences(listOf(id)).run {
            addOnSuccessListener {
                Log.i("GeofenceProvider", "Geofence removed: $id")
            }

            addOnFailureListener { exception ->
                Log.e("GeofenceProvider", "Failed to remove geofence", exception)
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

        geofencingClient.removeGeofences(ids).run {
            addOnSuccessListener {
                Log.i("GeofenceProvider", "Geofences removed")
            }

            addOnFailureListener { exception ->
                Log.e("GeofenceProvider", exception.message, exception)
            }
        }
    }
}