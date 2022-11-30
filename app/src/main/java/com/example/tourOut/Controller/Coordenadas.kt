package com.example.tourOut.Controller

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.tourOut.R

class Coordenadas : Service(), LocationListener {
    private var stop = false
    fun setStop(stop: Boolean) {
        this.stop = stop
        if (stop) {
            Process.killProcess(Process.myPid())
        }
    }

    fun setMessage(message: String) {
        Companion.message = message
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        message = getString(R.string.localizando)
        Thread {
            while (!stop) {
                creaNotification(
                    getString(R.string.localidade_mais_proxima),
                    message,
                    "lat: " + location.latitude + ", long: " + location.longitude
                )
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()
            startForeground(1, notification)
        }
    }

    private fun creaNotification(title: String, simpleText: String?, subText: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_touourt_logo)
            .setContentTitle(title)
            .setContentText(simpleText)
            .setSubText(subText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(1, builder.build())
    }

    override fun onLocationChanged(location: Location) {
        Companion.location = location
    }

    companion object {
        private const val CHANNEL_ID = "TourOut"

        @JvmStatic
        var location = Location("TourOut")
        private var message: String? = ""
    }
}