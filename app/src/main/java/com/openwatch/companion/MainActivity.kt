package com.openwatch.companion

import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ServiceConnection {
    private var btService: BluetoothService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
    }

    override fun onStart() {
        super.onStart()

        if (!bindService(Intent(this, BluetoothService::class.java), this, Context.BIND_AUTO_CREATE)) {
            Log.d(TAG, "Bind Failed")
        }
    }

    override fun onStop() {
        super.onStop()

        unbindService(this)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        btService = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.d(TAG, "Service Connected")

        btService = (service as? BluetoothService.BluetoothServiceBinder)?.service

        btService?.apply {
            if (!hasPreferredDevice()) {
                scanForWatches(::onWatchFound, onStopScanning = { Log.d(TAG, "Scan Stopped") })
            }
        }
    }

    private fun onWatchFound(result: ScanResult): Boolean {
        Log.d(TAG, "Found device ${result.device.name}")

        Log.d(TAG, "Services ${result.scanRecord?.serviceUuids}")

        btService?.setPreferredWatch(result.device)

        return false
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
