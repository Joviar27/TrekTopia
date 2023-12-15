package com.example.trektopia.service

import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.example.trektopia.core.model.Activity
import com.example.trektopia.utils.LatLngWrapper
import com.example.trektopia.utils.getParcelableExtra
import com.example.trektopia.utils.getStaticMapUri
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.maps.model.LatLng
import  com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Random

class RecordService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager

    private var _stepSensor: Sensor? = null
    private val stepSensor get() = _stepSensor
    private var stepCount = 0.0

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationRequest : LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var allLatLng: ArrayList<LatLng>
    private var boundsBuilder  = LatLngBounds.builder()

    private var allSpeed = 0.0
    private var averageSpeed = 0.0
    private var speedCounter = 1.0

    private var totalDistance: Double = 0.0
    private var previousLocation: Location? = null

    private var startTimeInMilis: Long = 0L
    private var elapsedTime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private lateinit var startTime: Timestamp
    private lateinit var endTime: Timestamp

    private var isRecording = false

    private lateinit var localBroadcastManager : LocalBroadcastManager

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
        localBroadcastManager = LocalBroadcastManager.getInstance(this@RecordService)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_START_RECORDING -> {
                    allLatLng = arrayListOf()

                    startForegroundService()
                    startLocationUpdate()

                    startTimeInMilis = System.currentTimeMillis()
                    startTime = Timestamp.now()
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
                    endTime = Timestamp.now()

                    isRecording = false

                    finalRecordResult()

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
                ACTION_SETUP_LOCATION_CALLBACK ->{
                    locationRequest = getParcelableExtra(intent, EXTRA_LOCATION_REQUEST)
                    createLocationCallback()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Walking activity")
            .setContentText("Recording your walk...")
            .setSmallIcon(R.drawable.ic_walk_small)
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
            liveStepCount(stepCount/100.0)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Nothing
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

                    allSpeed+= currentSpeedInKmph
                    averageSpeed = allSpeed / speedCounter
                    speedCounter++

                    if (previousLocation != null) {
                        val distance = previousLocation!!.distanceTo(location)
                        val distanceInKm = distance / 1000
                        totalDistance += distanceInKm
                    }
                    previousLocation = location

                    latestMapInfo(latLngWrapper, bounds, averageSpeed, totalDistance)
                }
            }
        }
    }

    private fun startTimer() {
        runnable = object : Runnable {
            override fun run() {
                elapsedTime = System.currentTimeMillis() - startTimeInMilis
                duration()
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

    private fun duration(){
        val intent = Intent(LIVE_TIMER_ACTION)
        intent.putExtra(EXTRA_ELAPSED_TIME, elapsedTime)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun liveStepCount(count: Double) {
        val intent = Intent(LIVE_COUNT_ACTION)
        intent.putExtra(EXTRA_LIVE_COUNT, count)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun finalRecordResult() {
        val markerLatLng = mutableListOf<LatLng>()
        val randomId = generateRandomId(stepCount.toInt(), elapsedTime, averageSpeed)
        val finalActivity = Activity(
            id = randomId,
            timeStamp = endTime,
            duration = elapsedTime,
            startTime = startTime,
            stepCount = stepCount.toInt(),
            distance = totalDistance,
            speed = averageSpeed,
            route = if(allLatLng.size>=280){
                val segmentSize = allLatLng.size/5
                for (i in 0 until 5) {
                    val index = i * segmentSize
                    if (index < allLatLng.size) {
                        markerLatLng.add(allLatLng[index])
                    }
                }
                markerLatLng.add(allLatLng.last())
                markerLatLng.getStaticMapUri()
            }else{
                markerLatLng.add(allLatLng.first())
                markerLatLng.add(allLatLng.last())
                markerLatLng.getStaticMapUri()
            }
        )

        val intent = Intent(FINAL_RESULT_ACTION)
        intent.putExtra(EXTRA_FINAL_ACTIVITY, finalActivity)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun latestMapInfo(allLatLng: LatLngWrapper, allBounds: LatLngBounds, averageSpeed: Double, distance: Double) {
        val intent = Intent(COORDINATE_ACTION)
        intent.putExtra(EXTRA_LATEST_COORDINATE, allLatLng)
        intent.putExtra(EXTRA_LATEST_BOUND,allBounds)
        intent.putExtra(EXTRA_LATEST_SPEED,averageSpeed)
        intent.putExtra(EXTRA_TOTAL_DISTANCE,distance)
        localBroadcastManager.sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    private fun generateRandomId(totalCount: Int, elapsed: Long, averageSpeed: Double): String {
        val dateFormat = DateTimeFormatter.ofPattern("MMdd", Locale.getDefault())
        val part1 = LocalDate.now().format(dateFormat)
        val part2 = totalCount.toString().substring(0, 1)
        val part3 = elapsed.toString().substring(0, 2)
        val part4 =
            String.format("%.2f", averageSpeed).replace(".", "").substring(0, 2)
        val random = Random()

        return "$part1$part2$part3$part4${random.nextInt(100)}"
    }

    companion object {
        //Send action to fragment
        const val LIVE_COUNT_ACTION = "live-count-update"
        const val FINAL_RESULT_ACTION = "total-count-update"
        const val CHECK_LOCATION_SETTING_ACTION = "check-location-setting"
        const val COORDINATE_ACTION = "lat-lng-update"
        const val LIVE_TIMER_ACTION = "elapsed-time-update"

        //Send data to fragment
        const val EXTRA_LIVE_COUNT = "liveStepCount"
        const val EXTRA_LATEST_COORDINATE = "latLng"
        const val EXTRA_LATEST_BOUND = "latLngBounds"
        const val EXTRA_ELAPSED_TIME = "elapsedTime"
        const val EXTRA_LATEST_SPEED = "latestSpeed"
        const val EXTRA_TOTAL_DISTANCE = "totalDistance"
        const val EXTRA_FINAL_ACTIVITY = "finalActivity"

        //Receive data from fragment
        const val EXTRA_LOCATION_REQUEST = "locationRequest"

        //Receive action from fragment
        const val ACTION_RESUME_RECORDING= "resumeRecording"
        const val ACTION_START_RECORDING = "startRecording"
        const val ACTION_STOP_RECORDING = "stopRecording"
        const val ACTION_PAUSE_RECORDING = "pauseRecording"
        const val ACTION_ERROR_RECORDING = "errorRecording"
        const val ACTION_SETUP_LOCATION_REQUEST = "createLocationRequest"
        const val ACTION_SETUP_LOCATION_CALLBACK = "createLocationCallback"

        //Notification related
        private const val NOTIFICATION_CHANNEL_ID = "RecordChannel"
        private const val NOTIFICATION_ID = 1
    }
}