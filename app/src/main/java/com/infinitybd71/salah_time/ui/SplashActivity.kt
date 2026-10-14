package com.infinitybd71.salah_time.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.infinitybd71.salah_time.R
import com.infinitybd71.salah_time.data.repository.CitiesRepository
import com.infinitybd71.salah_time.data.DeviceLocationProvider
import com.infinitybd71.salah_time.data.DeviceLocationResult
import com.infinitybd71.salah_time.data.LocationPrefs
import com.infinitybd71.salah_time.databinding.ActivitySplashBinding
import com.infinitybd71.salah_time.utils.Constants.STATE_NAVIGATED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var locationPrefs: LocationPrefs

    private var navigated: Boolean = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val anyGranted = grants.values.any { it }
        if (anyGranted) {
            fetchLocationAndContinue()
        } else {
            Toast.makeText(
                this,
                "Location permission denied. You can pick a city manually.",
                Toast.LENGTH_LONG
            ).show()
            goToHome()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        navigated = savedInstanceState?.getBoolean(STATE_NAVIGATED) ?: false
        if (navigated) return

        locationPrefs = LocationPrefs(this)

        decideNextStep()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_NAVIGATED, navigated)
    }

    private fun decideNextStep() {
        when {
            locationPrefs.isSet && locationPrefs.source == LocationPrefs.Source.MANUAL ->
                goToHome()

            DeviceLocationProvider.hasPermission(this) ->
                fetchLocationAndContinue()

            else ->
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
        }
    }

    private fun fetchLocationAndContinue() {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    DeviceLocationProvider.getCurrentLocation(this@SplashActivity)
                }

                when (result) {
                    is DeviceLocationResult.Success -> {
                        // Offline label from bundled cities.json — sub-millisecond
                        val label = withContext(Dispatchers.IO) {
                            CitiesRepository.ensureLoaded(this@SplashActivity)
                            CitiesRepository.nearestCity(result.lat, result.lng)?.label
                        }
                        locationPrefs.save(
                            lat = result.lat,
                            lng = result.lng,
                            label = label ?: "Unkown City",
                            source = LocationPrefs.Source.GPS
                        )
                        goToHome()
                    }

                    DeviceLocationResult.PermissionDenied -> goToHome()

                    DeviceLocationResult.Unavailable -> {
                        Toast.makeText(
                            this@SplashActivity,
                            "Couldn't get your location. Please pick a city.",
                            Toast.LENGTH_LONG
                        ).show()
                        goToHome()
                    }
                }
            } catch (e: Exception) {
                // Any unexpected error must NEVER trap the user on splash.
                goToHome()
            }
        }
    }

    private fun goToHome() {
        if (navigated) return
        navigated = true
        startActivity(Intent(this, HomeActivity::class.java))
        finish()   // so Back from Home doesn't return to splash
    }

}