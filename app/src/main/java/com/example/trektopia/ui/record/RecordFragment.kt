package com.example.trektopia.ui.record

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.example.trektopia.R
import com.example.trektopia.core.model.Activity
import com.example.trektopia.databinding.FragmentRecordBinding
import com.example.trektopia.service.RecordService
import com.example.trektopia.utils.LatLngWrapper
import com.example.trektopia.utils.getParcelableExtra
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.utils.safeNavigate
import com.example.trektopia.utils.showToast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.lang.Exception
import java.util.concurrent.TimeUnit

class RecordFragment : Fragment(), OnMapReadyCallback{

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: RecordViewModel

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationRequest : LocationRequest

    private var allLatLng = ArrayList<LatLng>()
    private var boundsBuilder  = LatLngBounds.builder()

    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                RecordService.LIVE_COUNT_ACTION -> {
                    val liveCount = intent.getDoubleExtra(RecordService.EXTRA_LIVE_COUNT, 0.0)
                    binding?.apply {
                        liveSteps.tvLiveInfo.text = String.format("%.1f", liveCount)
                        liveSteps.tvLiveType.text = resources.getString(R.string.live_steps)
                    }
                }
                RecordService.FINAL_RESULT_ACTION -> {
                    handleFinalResult(intent)
                    clearMaps()
                    //TODO: Stop service to prevent memory leak
                }
                RecordService.COORDINATE_ACTION -> {
                    handleCoordinateAction(intent)
                }
                RecordService.LIVE_TIMER_ACTION ->{
                    handleTimer(intent)
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            val fineLocationPermission =
                permission[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationPermission =
                permission[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            val isPermissionGranted =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    val activityRecognitionPermission =
                        permission[Manifest.permission.ACTIVITY_RECOGNITION] ?: false

                    fineLocationPermission && activityRecognitionPermission ||
                            coarseLocationPermission && activityRecognitionPermission
                } else {
                    fineLocationPermission || coarseLocationPermission
                }

            if (isPermissionGranted) getMyLastLocation()
            else "Permission request denied".showToast(requireContext())
        }


    private val resolutionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ){result ->
        when (result.resultCode){
            AppCompatActivity.RESULT_OK ->{
                Log.i(TAG, "onActivityResult: All location settings are satisfied.")
            }
            AppCompatActivity.RESULT_CANCELED -> {
                "GPS are required to use this application".showToast(requireContext())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = this.obtainViewModel()
        localBroadcastManager = LocalBroadcastManager.getInstance(requireContext())
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(RecordService.LIVE_COUNT_ACTION)
            addAction(RecordService.FINAL_RESULT_ACTION)
            addAction(RecordService.CHECK_LOCATION_SETTING_ACTION)
            addAction(RecordService.COORDINATE_ACTION)
            addAction(RecordService.LIVE_TIMER_ACTION)
        }
        localBroadcastManager.registerReceiver(broadcastReceiver, filter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        createLocationRequest()
        setupActionView()
    }

    private fun setupActionView() {
        binding?.apply {
            btnStartRecord.setOnClickListener {
                btnPauseRecord.visibility = View.VISIBLE
                btnStopRecord.visibility = View.VISIBLE
                btnStartRecord.visibility = View.GONE

                val createCallback = Intent(requireContext(), RecordService::class.java).apply {
                    action = RecordService.ACTION_SETUP_LOCATION_CALLBACK
                    putExtra(RecordService.EXTRA_LOCATION_REQUEST, locationRequest)
                }
                requireContext().startService(createCallback)

                handleServiceAction(RecordService.ACTION_START_RECORDING)
            }
            btnPauseRecord.setOnClickListener {
                btnPauseRecord.visibility = View.GONE
                btnStopRecord.visibility = View.GONE
                btnResumeRecord.visibility = View.VISIBLE
                btnStopRecordSmall.visibility = View.VISIBLE
                handleServiceAction(RecordService.ACTION_PAUSE_RECORDING)
            }
            btnStopRecord.setOnClickListener {
                handleServiceAction(RecordService.ACTION_STOP_RECORDING)
            }
            btnResumeRecord.setOnClickListener {
                btnPauseRecord.visibility = View.VISIBLE
                btnStopRecord.visibility = View.VISIBLE
                btnResumeRecord.visibility = View.GONE
                btnStopRecordSmall.visibility = View.GONE
                handleServiceAction(RecordService.ACTION_RESUME_RECORDING)
            }
            btnStopRecordSmall.setOnClickListener{
                handleServiceAction(RecordService.ACTION_STOP_RECORDING)
            }

            liveSteps.tvLiveType.text = resources.getString(R.string.live_steps)
            liveDistance.tvLiveType.text = resources.getString(R.string.km)
            liveSpeed.tvLiveType.text = resources.getString(R.string.km_h)
        }
    }

    private fun handleServiceAction(action: String) {
        val recordingIntent = Intent(requireContext(), RecordService::class.java)
        recordingIntent.action = action
        requireContext().startService(recordingIntent)
    }

    private fun clearMaps(){
        mMap.clear()
        allLatLng.clear()
        boundsBuilder = LatLngBounds.builder()
    }

    private fun createLocationRequest(){
        val interval = TimeUnit.SECONDS.toMillis(1)
        val priority = Priority.PRIORITY_HIGH_ACCURACY
        locationRequest = LocationRequest.Builder(priority, interval).build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(requireActivity())
        client.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                getMyLastLocation()
            }
            .addOnFailureListener {
                handleLocationSettingsFailure(it)
            }
    }

    private fun handleLocationSettingsFailure(exception: Exception) {
        if (exception is ResolvableApiException) {
            try {
                resolutionLauncher.launch(IntentSenderRequest.Builder(exception.resolution).build())
            } catch (sendEx: IntentSender.SendIntentException) {
                sendEx.message?.showToast(requireContext())
            }
        }
    }

    private fun getMyLastLocation() {
        if(checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            && checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ){
            fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
                if(location!=null) showMarker(location)
                else "Location is not found. Try Again".showToast(requireContext())
            }
        }
        else{
            requestPermissionLauncher.launch(
                REQUIRED_PERMISSIONS
            )
        }
    }

    private fun showMarker(location: Location){
        val startingLocation = LatLng(location.latitude, location.longitude)
        //TODO: Change marker title
        mMap.addMarker(
            MarkerOptions()
                .position(startingLocation)
                .title(getString(R.string.app_name))
        )
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startingLocation, 17f))
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun handleFinalResult(intent: Intent){
        val finalActivity = getParcelableExtra<Activity>(intent, RecordService.EXTRA_FINAL_ACTIVITY)
        localBroadcastManager.unregisterReceiver(broadcastReceiver)

        val toRecap = RecordFragmentDirections.actionRecordFragmentToRecapFragment(finalActivity)
        toRecap.activity = finalActivity
        findNavController().safeNavigate(toRecap)
    }

    fun handleTimer(intent: Intent){
        val elapsed = intent.getLongExtra(RecordService.EXTRA_ELAPSED_TIME, 0)

        val seconds = (elapsed / 1000).toInt()
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        binding?.tvTimer?.text =  String.format("%02d : %02d : %02d", hours, minutes, remainingSeconds)
    }

    fun handleCoordinateAction(intent: Intent) {
        try {
            val latLngWrapper = getParcelableExtra<LatLngWrapper>(intent, RecordService.EXTRA_LATEST_COORDINATE)
            val currentSpeed = intent.getDoubleExtra(RecordService.EXTRA_LATEST_SPEED, 0.0)
            val totalDistance = intent.getDoubleExtra(RecordService.EXTRA_TOTAL_DISTANCE, 0.0)

            allLatLng = latLngWrapper.latLngList

            mMap.addPolyline(
                PolylineOptions()
                    .color(Color.RED)
                    .width(10f)
                    .addAll(allLatLng)
            )
            val bounds = getParcelableExtra<LatLngBounds>(intent, RecordService.EXTRA_LATEST_BOUND)
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))

            binding?.liveDistance?.tvLiveInfo?.text = String.format("%.1f", totalDistance)
            binding?.liveDistance?.tvLiveType?.text = resources.getString(R.string.km)

            binding?.liveSpeed?.tvLiveInfo?.text = String.format("%.1f", currentSpeed)
            binding?.liveSpeed?.tvLiveType?.text = resources.getString(R.string.km_h)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun handleException(exception: Exception) {
        Log.e(TAG, exception.message.toString())
        handleServiceAction(RecordService.ACTION_ERROR_RECORDING)
    }

    companion object{
        val REQUIRED_PERMISSIONS = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

}