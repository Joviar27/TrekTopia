package com.example.trektopia.ui.main

import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.trektopia.R
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.databinding.ActivityMainBinding
import com.example.trektopia.ui.record.RecordFragment
import com.example.trektopia.utils.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewModel: AuthViewModel

    private lateinit var navController:NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.bottomAppBar)

        viewModel = this.obtainViewModel()

        setupNavigation()
    }


    private fun setupNavigation(){
        /*val navView : BottomNavigationView = binding.bottomNavView
        navController = Navigation.findNavController(this, R.id.nav_host_fragment) */

        // Alternative to get nav controller
        val navView : BottomNavigationView = binding.bottomNavView
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration.Builder(
            R.id.homeFragment,
            R.id.historyFragment,
            R.id.rankFragment,
            R.id.profileFragment
        ).build()

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupToolbarAndBottomBar()

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
                navController.navigate(R.id.recordFragment)
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

            if (isPermissionGranted) navController.navigate(R.id.recordFragment)
            else "Permission request denied".showToast(this)
        }


    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupToolbarAndBottomBar() {
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            binding.bottomAppBar.visibility =
                if (listFragmentBottomBar.contains(destination.id)) View.VISIBLE else View.GONE
            binding.fabRecord.visibility =
                if (listFragmentBottomBar.contains(destination.id)) View.VISIBLE else View.GONE

            binding.materialToolbar.apply {
                if(destination.id == R.id.profileFragment){
                    menu.clear()
                    inflateMenu(R.menu.menu_profile)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.logout -> {
                                viewModel.logout()
                                navController.navigate(R.id.loginFragment)
                                true
                            }
                            else -> false
                        }
                    }
                } else{
                    menu.clear()
                }

                visibility =
                    if (listFragmentTopBar.contains(destination.id)) View.VISIBLE
                    else View.GONE
                title = destination.label
                isTitleCentered = true

                if (listFragmentTopBarWithBackNavigation.contains(destination.id)) {
                    navigationIcon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_24)
                    setNavigationOnClickListener {
                        controller.navigateUp()
                    }
                } else {
                    navigationIcon = null
                    setNavigationOnClickListener(null)
                }
            }

            if(listFragmentNoSystemBar.contains(destination.id)){
                window.statusBarColor = ContextCompat.getColor(this, R.color.white)
            } else{
                window.statusBarColor = ContextCompat.getColor(this, R.color.secondary_container)
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

        val listFragmentNoSystemBar =
            listOf(
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.registerFragment
            )
    }
}