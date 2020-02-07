package com.example.trackertest

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import java.text.DateFormat
import java.util.*


class BgLocation : Service() {

    private val PACKAGE_NAME =
        "com.example.trackertest"
    private val mBinder = LocalBinder()
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocationCallback: LocationCallback? = null
    private var mLocation: Location? = null
    private val CHANNEL_ID = "channel_01"
    private var mLocationRequest: LocationRequest? = null
    private var mServiceHandler: Handler? = null

    internal val ACTION_BROADCAST = PACKAGE_NAME + ".broadcast"

    internal val EXTRA_LOCATION = PACKAGE_NAME + ".location"
    private var mNotificationManager: NotificationManager? = null
    private val EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME + ".started_from_notification"

    override fun onCreate() {
        super.onCreate()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult!!.lastLocation)
            }
        }

        mLocationRequest = LocationRequest()
        mLocationRequest!!.setInterval(10000)
        mLocationRequest!!.setFastestInterval(5000)
        mLocationRequest!!.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        getLastLocation()

        val handlerThread = HandlerThread("asdf")
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            // Create the channel for the notification
            val mChannel =
                NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager!!.createNotificationChannel(mChannel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("service", "Service started")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
//        Log.i(TAG, "in onBind()")
        stopForeground(true)
//        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent) {
//        Log.i(TAG, "in onRebind()")
        stopForeground(true)
//        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.i("Unbinddddd..........", "Last client unbound from service")
        startForeground(1234567, getNotification())
        return true // Ensures onRebind() is called when a client re-binds.
    }

    inner class LocalBinder : Binder() {
        fun getService() : BgLocation { return this@BgLocation }
    }

    private fun onNewLocation(location: Location) {
        Log.i("service", "New location: ${location.latitude}")

        mLocation = location

        Toast.makeText(this@BgLocation, "(${location.latitude}, ${location.longitude})", Toast.LENGTH_LONG).show()
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager!!.notify(1234567, getNotification())
        }
    }

    private fun getLastLocation() {
        try {
            mFusedLocationClient!!.getLastLocation()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        mLocation = task.result
                    } else {
                        Log.w("service", "Failed to get location.")
                    }
                }
        } catch (unlikely: SecurityException) {
            Log.e("service", "Lost location permission.$unlikely")
        }

    }

    private fun getNotification(): Notification {
        val intent = Intent(this, BgLocation::class.java)

        val text = mLocation.toString()

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)


        // The PendingIntent to launch activity.
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java), 0
        )

        val builder = NotificationCompat.Builder(this)
            .addAction(
                0, "Launch activity",
                activityPendingIntent
            )
//            .addAction(
//                R.drawable.ic_cancel, getString(R.string.remove_location_updates),
//                servicePendingIntent
//            )
            .setContentText(text)
            .setContentTitle("Location Updated :${DateFormat.getDateTimeInstance().format(Date())}")
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(text)
            .setWhen(System.currentTimeMillis())

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID) // Channel ID
        }

        return builder.build()
    }

    private fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager
        for (service in manager.getRunningServices(
            Integer.MAX_VALUE
        )) {
            if (javaClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }

    fun requestLocationUpdates() {
        Log.i("servicee", "Requesting location updates")
        startService(Intent(applicationContext, BgLocation::class.java))
        try {
            Log.i("testing", "inside try")
            mFusedLocationClient!!.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback, Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e("servicee", "Lost location permission. Could not request updates. $unlikely")
        }

    }
}