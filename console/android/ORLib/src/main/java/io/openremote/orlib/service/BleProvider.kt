package io.openremote.orlib.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat
import io.openremote.orlib.R
import java.util.*

class BleProvider(val context: Context) {
    interface BleCallback {
        fun accept(responseData: Map<String, Any>)
    }

    companion object {
        private const val bleDisabledKey = "bleDisabled"
        private const val version = "ble"

        // BLE UUIDs according to
        // https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf?v=1706541022964

        private val SERVICE_GENERIC_ACCESS = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
        private val SERVICE_GENERIC_ATTRIBUTE =
            UUID.fromString("00001801-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_SERVICE_CHANGED =
            UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_DEVICE_NAME =
            UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_APPEARANCE =
            UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_CENTRAL_ADDRESS_RESOLUTION =
            UUID.fromString("00002aa6-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_DATABASE_HASH =
            UUID.fromString("00002b2a-0000-1000-8000-00805f9b34fb")

        // This is the max MTU size supported by Android
        private const val GATT_MAX_MTU_SIZE = 517

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
    private val devices = mutableSetOf<BluetoothDevice>()
    private var currentGatt: BluetoothGatt? = null
    private val deviceCharacteristics = mutableListOf<BleAttribute>()
    private var readableDeviceIndex = 0
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d("BleProvider", "onScanResult: ${result.device.name} - ${result.device.address}")
            if (result.device != null && result.device.name != null) {
                devices.add(result.device)
            }
        }
    }
    private var sendToDeviceCallback: BleCallback? = null

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
            "enabled" to false,
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
            .putBoolean(bleDisabledKey, true)
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
        bleCallback: BleCallback
    ) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            val hasPermission = hasPermission()
            if (hasPermission) {
                if (!bluetoothAdapter.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    activity.startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
                } else {
                    scanLeDevice(bleCallback)
                }
            }
        } else if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE) {
            if (bluetoothAdapter.isEnabled) {
                scanLeDevice(bleCallback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startBLEScan(activity: Activity, bleCallback: BleCallback) {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        } else if (!hasPermission()) {
            requestPermissions(activity)
        } else {
            scanLeDevice(bleCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String, bleCallback: BleCallback) {
        currentGatt?.disconnect()
        devices.find { it.address == address }?.let { device ->
            device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {
                    val deviceAddress = gatt.device.address

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        currentGatt = gatt
                        readableDeviceIndex = 0
                        deviceCharacteristics.clear()
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.d(
                                "BluetoothGattCallback",
                                "Successfully connected to $deviceAddress"
                            )
                            gatt.requestMtu(GATT_MAX_MTU_SIZE)
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.w(
                                "BluetoothGattCallback",
                                "Successfully disconnected from $deviceAddress"
                            )
                            gatt.close()
                            bleCallback.accept(
                                hashMapOf(
                                    "action" to "CONNECT_TO_DEVICE",
                                    "provider" to "ble",
                                    "success" to false,
                                )
                            )
                        }
                    } else {
                        bleCallback.accept(
                            hashMapOf(
                                "action" to "CONNECT_TO_DEVICE",
                                "provider" to "ble",
                                "success" to false,
                            )
                        )
                        Log.w(
                            "BluetoothGattCallback",
                            "Error $status encountered for $deviceAddress! Disconnecting..."
                        )
                        gatt.close()
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    with(gatt) {
                        this?.services?.forEach { service ->
                            Log.d("BluetoothGattCallback", "Service: ${service.uuid}")
                            service.characteristics.forEach { characteristic ->
                                Log.d(
                                    "BluetoothGattCallback",
                                    "Characteristic: ${characteristic.uuid}"
                                )
                                if (characteristic.isReadable()) {
                                    deviceCharacteristics.add(
                                        BleAttribute(
                                            characteristic,
                                            isReadable = true,
                                            isWritable = false,
                                            value = null
                                        )
                                    )
                                }

                                if (characteristic.isWritable()) {
                                    deviceCharacteristics.find { it.characteristic.uuid == characteristic.uuid }?.isWritable =
                                        true
                                }
                            }
                            if (deviceCharacteristics.isNotEmpty()) {
                                readCharacteristic(deviceCharacteristics[readableDeviceIndex].characteristic)
                            }
                        }
                    }
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?,
                    status: Int
                ) {
                    characteristic?.let { readCharacteristic ->
                        handleCharacteristicStatus(gatt, readCharacteristic, status)
                    } ?: run {
                        Log.e("BluetoothGattCallback", "Characteristic was null!")
                    }
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int
                ) {
                    handleCharacteristicStatus(gatt, characteristic, status, value)
                }

                private fun handleCharacteristicStatus(
                    gatt: BluetoothGatt?,
                    readCharacteristic: BluetoothGattCharacteristic,
                    status: Int,
                    value: ByteArray? = null
                ) {
                    when (status) {
                        BluetoothGatt.GATT_SUCCESS -> Log.d(
                            "BluetoothGattCallback",
                            "Characteristic read success for ${readCharacteristic.uuid}"
                        )

                        BluetoothGatt.GATT_READ_NOT_PERMITTED -> Log.e(
                            "BluetoothGattCallback",
                            "Read not permitted for ${readCharacteristic.uuid}!"
                        )

                        else -> Log.e(
                            "BluetoothGattCallback",
                            "Characteristic read failed for ${readCharacteristic.uuid}, error: $status"
                        )
                    }
                    val deviceCharacteristic =
                        deviceCharacteristics.find { it.characteristic.uuid == readCharacteristic.uuid }
                    deviceCharacteristic?.also { attribute ->
                        if (deviceCharacteristic.characteristic.uuid == CHARACTERISTIC_DATABASE_HASH) {
                            attribute.value = value?.toHexString()
                        } else {
                            (value ?: readCharacteristic.value)?.let {
                                attribute.value = String(it).substringBefore('\u0000')
                            }
                        }
                    }
                    Log.d(
                        "BluetoothGattCallback",
                        "Read characteristic ${readCharacteristic.uuid}:\n${
                            value?.let {
                                String(it)
                            } ?: "null"
                        }"
                    )
                    handleDeviceIndex(gatt)
                }

                private fun handleDeviceIndex(gatt: BluetoothGatt?) {
                    if (readableDeviceIndex < deviceCharacteristics.size - 1) {
                        gatt?.readCharacteristic(deviceCharacteristics[++readableDeviceIndex].characteristic)
                    } else {
                        bleCallback.accept(
                            hashMapOf(
                                "action" to "CONNECT_TO_DEVICE",
                                "provider" to "ble",
                                "success" to true,
                                "data" to hashMapOf("attributes" to deviceCharacteristics.map {
                                    createAttributeMap(
                                        it
                                    )
                                })
                            )
                        )
                    }
                }

                private fun createAttributeMap(deviceCharacteristic: BleAttribute) =
                    hashMapOf(
                        "attributeId" to deviceCharacteristic.characteristic.uuid,
                        "isReadable" to deviceCharacteristic.isReadable,
                        "isWritable" to deviceCharacteristic.isWritable,
                        "value" to deviceCharacteristic.value
                    )

                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?,
                    status: Int
                ) {
                    super.onCharacteristicWrite(gatt, characteristic, status)
                    Log.d(
                        "BluetoothGattCallback",
                        "Write characteristic ${characteristic?.uuid}:\n${status}"
                    )
                    sendToDeviceCallback?.accept(
                        hashMapOf(
                            "action" to "SEND_TO_DEVICE",
                            "provider" to "ble",
                            "success" to (status == BluetoothGatt.GATT_SUCCESS)
                        )
                    )
                }

                override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
                    super.onReliableWriteCompleted(gatt, status)
                }

                override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                    Log.d(
                        "BluetoothGattCallback",
                        "ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}"
                    )
                    gatt?.discoverServices()
                }

            }, BluetoothDevice.TRANSPORT_LE)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendToDevice(characteristicID: String, value: Any, callback: BleCallback) {
        val characteristic =
            deviceCharacteristics.find { it.characteristic.uuid.toString() == characteristicID }?.characteristic
        if (characteristic == null) {
            callback.accept(
                hashMapOf(
                    "action" to "SEND_TO_DEVICE",
                    "provider" to "ble",
                    "success" to false,
                    "error" to "Characteristic $characteristicID not found"
                )
            )
            return
        }
        val writeType = when {
            characteristic.isSignedWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
            characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }

            else -> {
                callback.accept(
                    hashMapOf(
                        "action" to "SEND_TO_DEVICE",
                        "provider" to "ble",
                        "success" to false,
                        "error" to "Characteristic $characteristicID not found"
                    )
                )
                return
            }
        }

        val jsonString = value.toString()
        val payload = jsonString.toByteArray(Charsets.UTF_8)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            currentGatt?.writeCharacteristic(characteristic, payload, writeType)
                ?: error("Not connected to a BLE device!")
        } else {
            currentGatt?.let { gatt ->
                characteristic.writeType = writeType
                characteristic.value = payload
                gatt.writeCharacteristic(characteristic)
            } ?: error("Not connected to a BLE device!")
        }
        sendToDeviceCallback = callback
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
        devices.clear()
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

    private data class BleAttribute(
        var characteristic: BluetoothGattCharacteristic,
        var isReadable: Boolean,
        var isWritable: Boolean,
        var value: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BleAttribute

            if (characteristic != other.characteristic) return false
            if (isReadable != other.isReadable) return false
            if (isWritable != other.isWritable) return false
            if (value?.equals(other.value) == false) return false

            return true
        }

        override fun hashCode(): Int {
            var result = characteristic.hashCode()
            result = 31 * result + isReadable.hashCode()
            result = 31 * result + isWritable.hashCode()
            result = 31 * result + value.hashCode()
            return result
        }
    }
}

fun BluetoothGattCharacteristic.isReadable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

fun BluetoothGattCharacteristic.isWritable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

fun BluetoothGattCharacteristic.isSignedWritable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE)

fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
    return properties and property != 0
}

fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}
