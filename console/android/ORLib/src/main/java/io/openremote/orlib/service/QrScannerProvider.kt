package io.openremote.orlib.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        const val REQUEST_SCAN_QR = 222
    }
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

        callback?.accept(
            hashMapOf(
                "action" to "PROVIDER_ENABLE",
                "provider" to "qr",
                "hasPermission" to hasPermission,
                "success" to true,
                "enabled" to true,
                "disabled" to sharedPreferences.contains(qrDisabledKey)
            )
        )
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

    fun startScanner(activity: Activity) {
        val intent = Intent(activity, QrScannerActivity::class.java)
        activity.startActivityForResult(intent, REQUEST_SCAN_QR)
    }
}
