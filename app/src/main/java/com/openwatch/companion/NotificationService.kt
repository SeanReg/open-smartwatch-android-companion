package com.openwatch.companion

import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

data class NotificationDetails(val uid: Int, val contents: String, val app: String)

class NotificationService : NotificationListenerService(), ServiceConnection {
    private var pm: PackageManager? = null

    private var btService: BluetoothService? = null

    override fun onCreate() {
        super.onCreate()

        pm = packageManager

        bindService(Intent(this, BluetoothService::class.java), this, Context.BIND_AUTO_CREATE)
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(TAG, "Got notification ${sbn.uid}")

        try {
            val uid = sbn.id
            val contents = sbn.notification.extras.getString(Notification.EXTRA_TEXT, "")

            val appName = pm?.getApplicationInfo(sbn.packageName, 0)?.let { info ->
                 pm?.getApplicationLabel(info).toString()
            } ?: ""

            val details = NotificationDetails(uid, contents, appName)

            btService?.sendNotification(details)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("TAG", "Unknown package")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {

    }

    override fun onServiceDisconnected(name: ComponentName?) {
        btService = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        service.apply {
            btService = (this as? BluetoothService.BluetoothServiceBinder)?.service
        }
    }

    companion object {
        const val TAG = "NotificationService"
    }
}
