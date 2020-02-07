package com.example.trackertest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.content.ComponentName
import android.os.IBinder
import android.content.ServiceConnection
import android.view.View
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private var mService: BgLocation ? = null;
    private var mRequestLocationUpdatesButton: Button? = null

    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as BgLocation.LocalBinder
            mService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissionAccessCoarseLocationApproved = ActivityCompat
            .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (permissionAccessCoarseLocationApproved) {
            val backgroundLocationPermissionApproved = ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

            if (backgroundLocationPermissionApproved) {
                // App can access location both in the foreground and in the background.
                // Start your service that doesn't have a foreground service type
                // defined.
            } else {
                // App can only access location in the foreground. Display a dialog
                // warning the user that your app must have all-the-time access to
                // location in order to function properly. Then, request background
                // location.
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                     14
                )
            }
        } else {
            // App doesn't have access to the device's location at all. Make full request
            // for permission.
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                12
            )
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, BgLocation::class.java), mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
        mRequestLocationUpdatesButton = findViewById(R.id.button) as Button;

        mRequestLocationUpdatesButton!!.setOnClickListener(View.OnClickListener {
            mService!!.requestLocationUpdates();
        });
    }

    override fun onResume() {
        super.onResume()
    }
}
