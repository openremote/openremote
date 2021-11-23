package io.openremote.orlib.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import io.openremote.orlib.R
import io.openremote.orlib.ui.QrScannerActivity

class QrScannerProvider(val context: Context) {
    interface ScannerCallback {
        fun accept(responseData: Map<String, Any>)
    }

    companion object {
        private const val cameraPermissionAskedKey = "CameraPermissionAsked"
        private const val qrDisabledKey = "qrDisabled"
        private const val version = "qr"
        const val REQUEST_CAMERA_PERMISSION = 201
        const val REQUEST_SCAN_QR = 222
    }

    private var enableCallback: ScannerCallback? = null

    fun initialize(): Map<String, Any> {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

        return hashMapOf(
            "action" to "PROVIDER_INIT",
            "provider" to "qr",
            "version" to version,
            "requiresPermission" to true,
            "hasPermission" to (
                    context.checkSelfPermission(
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED),
            "success" to true,
            "enabled" to false, // Always require enabling to ensure geofences are refresh at startup
            "disabled" to sharedPreferences.contains(qrDisabledKey)
        )
    }

    fun enable(callback: ScannerCallback?) {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

        sharedPreferences.edit()
            .remove(qrDisabledKey)
            .apply()

        val hasPermission =
            context.checkSelfPermission(
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED



        if (!hasPermission && !sharedPreferences.getBoolean(
                cameraPermissionAskedKey, false
            )
        ) {
            sharedPreferences.edit().putBoolean(cameraPermissionAskedKey, true).apply()
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            enableCallback = callback
        } else {
            callback?.accept(
                hashMapOf(
                    "action" to "PROVIDER_ENABLE",
                    "provider" to "qr",
                    "hasPermission" to hasPermission,
                    "success" to true
                )
            )
        }
    }

    fun disable(): Map<String, Any> {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean(cameraPermissionAskedKey, true)
            .apply()

        return hashMapOf(
            "action" to "PROVIDER_DISABLE",
            "provider" to "qr"
        )
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableCallback?.accept(
                hashMapOf(
                    "action" to "PROVIDER_ENABLE",
                    "provider" to "qr",
                    "hasPermission" to true,
                    "success" to true
                )
            )
        } else {
            AlertDialog.Builder(context)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.camera_needed_alert_title)
                .setMessage(R.string.camera_needed_alert_body)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes) { dialog, which ->
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_PERMISSION
                    )
                }
                .show()
        }
    }

    fun startScanner(activity: Activity) {
        val intent = Intent(activity, QrScannerActivity::class.java)
        activity.startActivityForResult(intent, REQUEST_SCAN_QR)
    }
}