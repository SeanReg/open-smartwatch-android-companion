package com.openwatch.companion

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.util.Log
import com.google.gson.Gson
import java.util.*
import kotlin.properties.Delegates

class WatchDevice: BluetoothGattCallback() {
    var gatt: BluetoothGatt? = null

    private var connectionState: Int by Delegates.observable(BluetoothGatt.STATE_DISCONNECTED) { _, oldValue, newValue ->
        if (newValue == BluetoothGatt.STATE_CONNECTED && oldValue != newValue) {
            onConnected()
        }
    }

    private var servicesDiscovered = false

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)

        servicesDiscovered = true

        Log.d(TAG, "Found new services: ${gatt?.services}")
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        connectionState = newState


        Log.d(TAG, "Connection State $connectionState")
    }

    fun sendNotification(details: NotificationDetails): Boolean {
        if (connectionState == BluetoothGatt.STATE_CONNECTED && servicesDiscovered) {
            gatt?.let { gatt ->
                Log.d(TAG, "Services: ${gatt.services.map { s -> s.uuid }}")
                Log.d(TAG, "Characteristics: ${gatt.services.map { s -> s.characteristics.map { c -> c.uuid } }}")

                gatt.getService(WATCH_SERVICE)?.getCharacteristic(NOTIFICATION_CHAR)?.let { char ->
                    char.setValue(Gson().toJson(details))
                    gatt.writeCharacteristic(char)

                    return true
                } ?: Log.d(TAG, "Service or Characteristic not found!")
            }
        }

        return false
    }

    private fun onConnected() {
        gatt?.discoverServices()
    }

    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
    }

    companion object {
        const val TAG  = "WatchDevice"

        val WATCH_SERVICE = UUID.fromString("30412632-6339-46e3-ad9e-bcfa9a766854")
        val NOTIFICATION_CHAR = UUID.fromString("23dac1dc-ca00-47ed-a5fa-e3b9da959685")
    }
}