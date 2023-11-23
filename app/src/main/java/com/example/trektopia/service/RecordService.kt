package com.example.trektopia.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.trektopia.R
import com.example.trektopia.ui.record.RecordFragment
import com.example.trektopia.utils.LatLngWrapper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.TimeUnit
import  com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLngBounds

class RecordService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager

    private var _stepSensor: Sensor? = null
    private val stepSensor get() = _stepSensor
    private var stepCount = 0

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationRequest : LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var allLatLng: ArrayList<LatLng>
    private var boundsBuilder  = LatLngBounds.builder()

    private var averageSpeed = 0.0
    private var counter = 1

    private var totalDistance: Double = 0.0
    private var previousLocation: Location? = null

    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private var isRecording = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        _stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        stepSensor?.let {sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_START_RECORDING -> {
                    startForegroundService()
                    startLocationUpdate()
                    startTime = System.currentTimeMillis()
                    startTimer()
                    isRecording = true
                }
                ACTION_RESUME_RECORDING -> {
                    startLocationUpdate()
                    startTimer()
                    isRecording = true
                }
                ACTION_STOP_RECORDING -> {
                    stopLocationUpdates()
                    stopTimer()
                    isRecording = false

                    val latLngWrapper = LatLngWrapper(allLatLng)
                    finalRecordResult(stepCount,latLngWrapper, averageSpeed, elapsedTime, totalDistance)

                    stopForegroundService()
                }
                ACTION_PAUSE_RECORDING -> {
                    stopLocationUpdates()
                    stopTimer()
                    isRecording = false
                }
                ACTION_ERROR_RECORDING ->{
                    stopLocationUpdates()
                    stopTimer()
                    isRecording = false
                    stopForegroundService()
                }
                ACTION_SETUP_LOCATION_REQUEST ->{
                    createLocationRequest()
                }
                ACTION_SETUP_LOCATION_CALLBACK ->{
                    createLocationCallback()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()

        //TODO: Add backstack to notification
        val notificationIntent = Intent(this, RecordFragment::class.java)

        val pendingFlags: Int = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingFlags
        )

        //TODO: Customize notification
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Walk recording")
            .setContentText("Recording you walk...")
            .setSmallIcon(R.drawable.ic_arrow_back_24)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Step Counter Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor == stepSensor && isRecording) {
            stepCount++
            liveStepCount(stepCount)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Nothing
    }

    private fun createLocationRequest(){
        val interval = TimeUnit.SECONDS.toMillis(1)
        val priority = Priority.PRIORITY_HIGH_ACCURACY

        locationRequest = LocationRequest.Builder(priority, interval).build()

        checkLocationSettings(locationRequest)
    }

    private fun createLocationCallback(){
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult) {
                for (location in p0.locations){
                    val latestLatLng = LatLng(location.latitude, location.longitude)
                    allLatLng.add(latestLatLng)

                    val latLngWrapper = LatLngWrapper(allLatLng)

                    boundsBuilder.include(latestLatLng)
                    val bounds: LatLngBounds = boundsBuilder.build()

                    val currentSpeedInMps = location.speed
                    val currentSpeedInKmph = currentSpeedInMps * 3.6 // Conversion factor

                    averageSpeed = currentSpeedInKmph / counter
                    counter++

                    if (previousLocation != null) {
                        val distance = previousLocation!!.distanceTo(location)
                        val distanceInKm = distance / 1000
                        totalDistance += distanceInKm
                    }
                    previousLocation = location

                    latestMapInfo(latLngWrapper, bounds, currentSpeedInKmph, totalDistance)
                }
            }
        }
    }

    private fun startTimer() {
        runnable = object : Runnable {
            override fun run() {
                elapsedTime = System.currentTimeMillis() - startTime
                duration(elapsedTime)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun stopTimer(){
        handler.removeCallbacks(runnable)
    }

    private fun startLocationUpdate(){
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e : SecurityException){
            Log.e(TAG, "Error : " + e.message.toString())
        }
    }

    private fun stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun duration(elapsed: Long){
        val intent = Intent(LIVE_TIMER_ACTION)
        intent.putExtra(EXTRA_ELAPSED_TIME, elapsed)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun liveStepCount(count: Int) {
        val intent = Intent(LIVE_COUNT_ACTION)
        intent.putExtra(EXTRA_LIVE_COUNT, count)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun finalRecordResult(totalCount: Int, allLatLng: LatLngWrapper, averageSpeed: Double, elapsed: Long, distance: Double) {
        //TODO: just send activity parceleable lol
        val intent = Intent(FINAL_RESULT_ACTION)
        intent.putExtra(EXTRA_TOTAL_COUNT, totalCount)
        intent.putExtra(EXTRA_ELAPSED_TIME, elapsed)
        intent.putExtra(EXTRA_LATEST_COORDINATE, allLatLng)
        intent.putExtra(EXTRA_AVERAGE_SPEED,averageSpeed)
        intent.putExtra(EXTRA_TOTAL_DISTANCE,distance)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun checkLocationSettings(locationRequest: LocationRequest) {
        val intent = Intent(CHECK_LOCATION_SETTING_ACTION)
        intent.putExtra(EXTRA_LOCATION_REQUEST, locationRequest)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun latestMapInfo(allLatLng: LatLngWrapper, allBounds: LatLngBounds, currentSpeed: Double, distance: Double) {
        val intent = Intent(COORDINATE_ACTION)
        intent.putExtra(EXTRA_LATEST_COORDINATE, allLatLng)
        intent.putExtra(EXTRA_LATEST_BOUND,allBounds)
        intent.putExtra(EXTRA_LATEST_SPEED,currentSpeed)
        intent.putExtra(EXTRA_TOTAL_DISTANCE,distance)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    companion object {
        const val LIVE_COUNT_ACTION = "live-count-update"
        const val FINAL_RESULT_ACTION = "total-count-update"
        const val CHECK_LOCATION_SETTING_ACTION = "check-location-setting"
        const val COORDINATE_ACTION = "lat-lng-update"
        const val LIVE_TIMER_ACTION = "elapsed-time-update"

        const val EXTRA_LIVE_COUNT = "liveStepCount"
        const val EXTRA_TOTAL_COUNT = "totalStepCount"
        const val EXTRA_LOCATION_REQUEST = "locationRequest"
        const val EXTRA_LATEST_COORDINATE = "latLng"
        const val EXTRA_LATEST_BOUND = "latLngBounds"
        const val EXTRA_ELAPSED_TIME = "elapsedTime"
        const val EXTRA_LATEST_SPEED = "latestSpeed"
        const val EXTRA_AVERAGE_SPEED = "averageSpeed"
        const val EXTRA_TOTAL_DISTANCE = "totalDistance"

        const val ACTION_RESUME_RECORDING= "resumeRecording"
        const val ACTION_START_RECORDING = "startRecording"
        const val ACTION_STOP_RECORDING = "stopRecording"
        const val ACTION_PAUSE_RECORDING = "pauseRecording"
        const val ACTION_ERROR_RECORDING = "errorRecording"
        const val ACTION_SETUP_LOCATION_REQUEST = "createLocationRequest"
        const val ACTION_SETUP_LOCATION_CALLBACK = "createLocationCallback"

        private const val NOTIFICATION_CHANNEL_ID = "RecordChannel"
        private const val NOTIFICATION_ID = 1
    }
}