package com.openwatch.companion

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.*

class BluetoothService : Service() {
    private val binder = BluetoothServiceBinder()

    private var watchDevice: WatchDevice? = null

    private var btManager: BluetoothManager? = null
    private val sharedPref by lazy { getSharedPreferences(getString(R.string.bluetooth_prefs), Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()

        btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        connectToWatch()
    }

    fun scanForWatches(onResult: (ScanResult) -> Boolean, timeout: Long = 10_000, onStopScanning: () -> Unit = {}) {
        btManager?.adapter?.bluetoothLeScanner?.apply {
            val filter = ScanFilter.Builder().apply {
                setServiceUuid(ParcelUuid(WatchDevice.WATCH_SERVICE))
            }.build()

            val settings = ScanSettings.Builder().apply {
                setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                setReportDelay(0L)
            }.build()

            var scanCallback: ScanCallback? = null
            var timeoutRoutine: Job? = null

            fun stop() {
                if (timeoutRoutine?.isActive == true) {
                    timeoutRoutine?.cancel()
                }

                scanCallback?.apply {
                    stopScan(this)
                }

                onStopScanning()
            }

            if (timeout > 0) {
                timeoutRoutine = GlobalScope.launch {
                    delay(timeout)
                    stop()
                }
            }

            scanCallback = object: ScanCallback() {
                override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                    super.onBatchScanResults(results)
                    Log.d(TAG, "Found batch")
                }
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    Log.d(TAG, "Found device")
                    result?.let {
                        if (!onResult(it)) {
                            stop()
                        }
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.d(TAG, "Scanning Failed $errorCode")
                    stop()
                }
            }

            //startScan(scanCallback)
            startScan(listOf(filter), settings, scanCallback)
        }
    }

    fun setPreferredWatch(device: BluetoothDevice) {
        sharedPref.edit().apply {
            putString("watch", device.address)
            apply()
        }

        connectToWatch()
    }

    fun hasPreferredDevice(): Boolean {
        return sharedPref.getString("watch", null) != null
    }

    fun sendNotification(notification: NotificationDetails) {
        val sent = watchDevice?.sendNotification(notification) ?: false

        if (!sent) {
            Log.d(TAG, "Faild to send notification ${notification.uid}")
        }
    }

    private fun connectToWatch() {
        watchDevice?.disconnect()

        sharedPref.getString("watch", null)?.let { mac ->
            watchDevice = WatchDevice()
            val gatt = btManager?.adapter?.getRemoteDevice(mac)?.connectGatt(this, true, watchDevice)

            watchDevice?.gatt = gatt
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    inner class BluetoothServiceBinder : Binder() {
        val service: BluetoothService
            get() = this@BluetoothService
    }

    companion object {
        const val TAG = "BluetoothService"
    }
}
