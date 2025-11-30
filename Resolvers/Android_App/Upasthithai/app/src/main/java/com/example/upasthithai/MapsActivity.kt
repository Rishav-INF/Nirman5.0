package com.example.upasthithai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.upasthithai.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
private const val CHANNEL_ID = "location_updates"
private const val FIXED_LOCATION_RADIUS_METERS = 30f

//private val FIXED_LOCATION = LatLng(22.260365, 84.889434) // Example office location  22.260365
private val FIXED_LOCATION = LatLng(20.350313, 85.805916)

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var database: DatabaseReference

    private var isInsideRadius = false
    private var intime: Long = 0
    private var outime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        database = FirebaseDatabase.getInstance().getReference("organizations")

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationRequest()
        setupLocationCallback()
        checkLocationPermissions()

        createNotificationChannel()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (checkLocationPermission()) {
            mMap.isMyLocationEnabled = true
        }

        mMap.addMarker(MarkerOptions().position(FIXED_LOCATION).title("Office Location"))
        drawCircleAroundFixedLocation()
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(FIXED_LOCATION, 17f))
    }

    private fun drawCircleAroundFixedLocation() {
        val circle_1 = CircleOptions()
            .center(FIXED_LOCATION)
            .radius(FIXED_LOCATION_RADIUS_METERS.toDouble())
            .strokeColor(0xFFFF0000.toInt())
            .fillColor(0x40FF0000)
            .strokeWidth(5f)
        mMap.addCircle(circle_1)
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000
        ).build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    updateMapWithLocation(location)
                    checkProximityToFixedLocation(location)
                }
            }
        }
    }

    private fun updateMapWithLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun checkProximityToFixedLocation(location: Location) {
        val distance = FloatArray(1)
        Location.distanceBetween(
            location.latitude, location.longitude,
            FIXED_LOCATION.latitude, FIXED_LOCATION.longitude,
            distance
        )

        if (distance[0] < FIXED_LOCATION_RADIUS_METERS) {
            if (!isInsideRadius) {
                isInsideRadius = true
                intime = System.currentTimeMillis()
                sendNotification("You are inside the fixed location radius.")
            }
        } else {
            if (isInsideRadius) {
                isInsideRadius = false
                outime = System.currentTimeMillis()
                sendNotification("You exited the fixed location radius.")
            }
        }
    }

    private fun sendNotification(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_upasthit_hai) // your app icon
            .setContentTitle("Location Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(1002, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for location-based alerts"
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun checkLocationPermissions() {
        if (!checkLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkLocationPermission()) {
                        mMap.isMyLocationEnabled = true
                        startLocationUpdates()
                    }
                } else {
                    Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
                }
            }
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notification permission is required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
