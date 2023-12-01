package com.example.trektopia.ui.main

import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.trektopia.R
import com.example.trektopia.core.AuthState
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.utils.safeNavigate
import com.example.trektopia.databinding.ActivityMainBinding
import com.example.trektopia.ui.record.RecordFragment
import com.example.trektopia.utils.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var _viewModel: AuthViewModel? = null
    private val viewModel get() = _viewModel

    private lateinit var navController:NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        _viewModel = this.obtainViewModel()

        setupNavigation()
        observeAuthState()
    }

    private fun observeAuthState(){
        viewModel?.authState?.observe(this){ authState ->
            val destination = when(authState){
                is AuthState.Authenticated -> {
                    R.id.homeFragment
                }
                is AuthState.UnAuthenticated ->{
                    R.id.loginFragment
                }
            }
            navController.safeNavigate(destination)
        }
    }

    private fun setupNavigation(){
        val navView : BottomNavigationView = binding.bottomNavView
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        /* Alternative to get nav controller
            val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
        */

        val appBarConfiguration = AppBarConfiguration.Builder(
            R.id.homeFragment,
            R.id.historyFragment,
            R.id.rankFragment,
            R.id.profileFragment
        ).build()

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setBottomNavigation(navController)
        handleRecordButton()
    }

    private fun handleRecordButton() {
        binding.fabRecord.setOnClickListener {
            val fineLocationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
            val coarseLocationPermission = android.Manifest.permission.ACCESS_COARSE_LOCATION

            val locationPermissionGranted =
                checkPermission(fineLocationPermission) && checkPermission(coarseLocationPermission)

            val activityPermissionGranted =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    val activityRecognitionPermission = android.Manifest.permission.ACTIVITY_RECOGNITION
                    checkPermission(activityRecognitionPermission)
                } else {
                    true // No need to check activity recognition permission for older versions
                }

            if (locationPermissionGranted && activityPermissionGranted) {
                requestPermissionLauncher.launch(RecordFragment.REQUIRED_PERMISSIONS)
            } else {
                navController.safeNavigate(R.id.recordFragment)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            val fineLocationPermission = permission[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationPermission = permission[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            val isPermissionGranted =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    val activityRecognitionPermission =
                        permission[android.Manifest.permission.ACTIVITY_RECOGNITION] ?: false
                    fineLocationPermission && activityRecognitionPermission ||
                            coarseLocationPermission && activityRecognitionPermission
                } else {
                    fineLocationPermission || coarseLocationPermission
                }

            if (isPermissionGranted) navController.safeNavigate(R.id.recordFragment)
            else "Permission request denied".showToast(this)
        }


    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setBottomNavigation(navController: NavController) {
        navController.addOnDestinationChangedListener { controller, destination, _ ->
            binding.bottomNavView.visibility =
                if (listFragmentBottomBar.contains(destination.id)) View.VISIBLE else View.GONE

            binding.materialToolbar.apply {
                visibility =
                    if (listFragmentTopBar.contains(destination.id)) View.VISIBLE
                    else View.GONE
                title = destination.label
                if(listFragmentTopBarWithBackNavigation.contains(destination.id)){
                    navigationIcon =ContextCompat.getDrawable(context,R.drawable.ic_arrow_back_24)
                    setNavigationOnClickListener {
                        controller.navigateUp()
                    }
                }
            }
        }
    }

    companion object {
        val listFragmentBottomBar =
            listOf(
                R.id.homeFragment,
                R.id.historyFragment,
                R.id.rankFragment,
                R.id.profileFragment
            )

        val listFragmentTopBar =
            listOf(
                R.id.historyFragment,
                R.id.historyDetailFragment,
                R.id.recordFragment,
                R.id.recapFragment,
                R.id.profileFragment,
                R.id.fullAchievementsFragment
            )

        val listFragmentTopBarWithBackNavigation =
            listOf(
                R.id.historyDetailFragment,
                R.id.recordFragment,
                R.id.fullAchievementsFragment
            )
    }
}