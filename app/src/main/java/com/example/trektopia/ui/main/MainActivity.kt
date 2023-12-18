package com.example.trektopia.ui.main

import android.Manifest
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
import com.example.trektopia.service.AlarmReceiver
import com.example.trektopia.ui.record.RecordFragment
import com.example.trektopia.utils.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewModel: AuthViewModel

    private lateinit var navController:NavController

    private lateinit var receiver: AlarmReceiver

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            val fineLocationPermission = permission[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationPermission = permission[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            val isPermissionGranted =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    val activityRecognitionPermission =
                        permission[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
                    fineLocationPermission && activityRecognitionPermission ||
                            coarseLocationPermission && activityRecognitionPermission
                } else {
                    fineLocationPermission || coarseLocationPermission
                }

            if (isPermissionGranted) navController.navigate(R.id.recordFragment)
            else "Permission request denied".showToast(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        receiver = AlarmReceiver()

        setSupportActionBar(binding.bottomAppBar)

        viewModel = this.obtainViewModel(this)

        setupNavigation()
    }

    private fun setupNavigation(){
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
            val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
            val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

            val locationPermissionGranted =
                checkPermission(fineLocationPermission) && checkPermission(coarseLocationPermission)

            val activityPermissionGranted =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    val activityRecognitionPermission = Manifest.permission.ACTIVITY_RECOGNITION
                    checkPermission(activityRecognitionPermission)
                } else {
                    true
                }

            if (locationPermissionGranted && activityPermissionGranted) {
                navController.navigate(R.id.recordFragment)
            } else {
                requestPermissionLauncher.launch(RecordFragment.REQUIRED_PERMISSIONS)
            }
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupToolbarAndBottomBar() {
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            val isListFragmentBottomBar = listFragmentBottomBar.contains(destination.id)
            binding.bottomAppBar.visibility = if (isListFragmentBottomBar) View.VISIBLE else View.GONE
            binding.fabRecord.visibility = if (isListFragmentBottomBar) View.VISIBLE else View.GONE

            binding.materialToolbar.apply {
                if (destination.id == R.id.profileFragment) {
                    setupProfileFragmentMenu()
                } else {
                    menu.clear()
                }
                visibility = if (listFragmentTopBar.contains(destination.id)) View.VISIBLE else View.GONE
                title = destination.label
                isTitleCentered = true

                setupToolbarNavigation(destination.id, controller)
            }
            setupStatusBarColor(destination.id)
        }
    }

    private fun setupProfileFragmentMenu() {
        binding.materialToolbar.menu.clear()
        binding.materialToolbar.inflateMenu(R.menu.menu_profile)
        binding.materialToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.logout -> {
                    handleLogoutMenuItemClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun handleLogoutMenuItemClick() {
        if (viewModel.getNotifStatus()) {
            cancelNotificationAlarms()
            viewModel.setNotifStatus(false)
        }

        if (viewModel.getResetStatus()) {
            cancelResetAlarm()
            viewModel.setResetStatus(false)
        }

        viewModel.logout()
        navController.navigate(R.id.loginFragment)
    }

    private fun cancelNotificationAlarms() {
        val receiver = AlarmReceiver()
        receiver.cancelAlarm(this@MainActivity, AlarmReceiver.REMINDER_ALARM_ID_1, AlarmReceiver.SHOW_NOTIF_ACTION)
        receiver.cancelAlarm(this@MainActivity, AlarmReceiver.REMINDER_ALARM_ID_2, AlarmReceiver.SHOW_NOTIF_ACTION)
    }

    private fun cancelResetAlarm() {
        val receiver = AlarmReceiver()
        receiver.cancelAlarm(this, AlarmReceiver.RESET_ALARM_ID, AlarmReceiver.DAILY_RESET_ACTION)
    }

    private fun setupToolbarNavigation(destinationId: Int, controller: NavController) {
        binding.materialToolbar.navigationIcon =
            if (listFragmentTopBarWithBackNavigation.contains(destinationId)) {
                ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_arrow_back_24)
            } else {
                null
            }

        binding.materialToolbar.setNavigationOnClickListener {
            if (listFragmentTopBarWithBackNavigation.contains(destinationId)) {
                controller.navigateUp()
            }
        }
    }

    private fun setupStatusBarColor(destinationId: Int) {
        window.statusBarColor =
            if (listFragmentNoSystemBar.contains(destinationId)) {
                ContextCompat.getColor(this, R.color.white)
            } else {
                ContextCompat.getColor(this, R.color.secondary_container)
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