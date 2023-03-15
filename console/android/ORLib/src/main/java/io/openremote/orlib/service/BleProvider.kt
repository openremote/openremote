package io.openremote.orlib.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import androidx.core.app.ActivityCompat
import io.openremote.orlib.R

class BleProvider(val context: Context) {
    interface BleCallback {
        fun accept(responseData: Map<String, Any>)
    }

    companion object {
        private const val bluetoothPermissionAskedKey = "BluetoothPermissionAsked"
        private const val bleDisabledKey = "bleDisabled"
        private const val version = "ble"
        const val ENABLE_BLUETOOTH_REQUEST_CODE = 555
        const val BLUETOOTH_PERMISSION_REQUEST_CODE = 556
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val scanPeriod: Long = 3000
    private var scanning = false
    private val handler = Handler(context.mainLooper)
    private val devices = mutableListOf<BluetoothDevice>()
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
            super.onScanResult(callbackType, result)
            if (result.device != null && result.device.name != null) {
                devices.add(result.device)
            }
        }
    }

    fun initialize(): Map<String, Any> {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

        return hashMapOf(
            "action" to "PROVIDER_INIT",
            "provider" to "ble",
            "version" to version,
            "requiresPermission" to true,
            "hasPermission" to hasPermission(),
            "success" to true,
            "enabled" to false, // Always require enabling to ensure geofences are refresh at startup
            "disabled" to sharedPreferences.contains(bleDisabledKey)
        )
    }

    @SuppressLint("MissingPermission")
    fun enable(callback: BleCallback?) {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

        sharedPreferences.edit()
            .remove(bleDisabledKey)
            .apply()

        callback?.accept(
            hashMapOf(
                "action" to "PROVIDER_ENABLE",
                "provider" to "ble",
                "hasPermission" to hasPermission(),
                "success" to true,
                "enabled" to true,
                "disabled" to sharedPreferences.contains(bleDisabledKey)
            )
        )
    }

    fun disable(): Map<String, Any> {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean(bluetoothPermissionAskedKey, true)
            .apply()

        return hashMapOf(
            "action" to "PROVIDER_DISABLE",
            "provider" to "ble"
        )
    }

    @SuppressLint("MissingPermission")
    fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        bleCallback: BleCallback?
    ) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            val hasPermission = hasPermission()
            if (hasPermission) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activity.startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
            }
        } else if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE) {
            val enabled = bluetoothAdapter.isEnabled
            if (enabled && bleCallback != null) {
                scanLeDevice(bleCallback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startBLEScan(activity: Activity, bleCallback: BleCallback) {
        if (!bluetoothAdapter.isEnabled) {
            val hasPermission = hasPermission()
            if (hasPermission) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activity.startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
            } else {
                requestPermissions(activity)
            }
        } else {
            scanLeDevice(bleCallback)
        }
    }

    private fun requestPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun hasPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice(bleCallback: BleCallback) {
        bluetoothAdapter.bluetoothLeScanner?.let { bluetoothLeScanner ->
            if (!scanning) { // Stops scanning after a pre-defined scan period.
                handler.postDelayed({
                    scanning = false
                    bluetoothLeScanner.stopScan(scanCallback)
                    bleCallback.accept(
                        hashMapOf(
                            "action" to "SCAN_BLE_DEVICES",
                            "provider" to "ble",
                            "data" to hashMapOf("devices" to devices.map { device ->
                                hashMapOf(
                                    "name" to device.name,
                                    "address" to device.address
                                )
                            })
                        )
                    )
                }, scanPeriod)
                scanning = true
                bluetoothLeScanner.startScan(scanCallback)
            } else {
                scanning = false
                bluetoothLeScanner.stopScan(scanCallback)
                bleCallback.accept(
                    hashMapOf(
                        "action" to "SCAN_BLE_DEVICES",
                        "provider" to "ble",
                        "data" to hashMapOf("devices" to devices.map { device ->
                            hashMapOf(
                                "name" to device.name,
                                "address" to device.address
                            )
                        })
                    )
                )
            }
        }
    }
}
