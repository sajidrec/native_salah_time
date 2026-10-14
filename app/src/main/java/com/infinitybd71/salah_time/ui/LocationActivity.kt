package com.infinitybd71.salah_time.ui

import android.Manifest
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.infinitybd71.salah_time.R
import com.infinitybd71.salah_time.databinding.ActivityLocationBinding
import androidx.lifecycle.lifecycleScope
import com.infinitybd71.salah_time.data.DeviceLocationProvider
import com.infinitybd71.salah_time.data.DeviceLocationResult
import com.infinitybd71.salah_time.data.LocationPrefs
import com.infinitybd71.salah_time.data.repository.CitiesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationActivity : AppCompatActivity() {

    lateinit var binding: ActivityLocationBinding
    private lateinit var locationPrefs: LocationPrefs

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            useCurrentLocation()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        locationPrefs = LocationPrefs(this@LocationActivity)

        binding.appbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                CitiesRepository.ensureLoaded(this@LocationActivity)
            }
            setupSearch()
        }

        binding.btnUseCurrent.setOnClickListener {
            if (DeviceLocationProvider.hasPermission(this)) {
                useCurrentLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

    }

    fun setupSearch() {
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false

            androidx.core.view.WindowCompat.getInsetsController(window, binding.searchInput)
                .hide(WindowInsetsCompat.Type.ime())

            val query = binding.searchInput.text.toString().trim()
            val city = CitiesRepository.findCityByName(query)

            if (city != null) {
                locationPrefs.save(city.lat, city.lng, city.label, LocationPrefs.Source.MANUAL)
                Toast.makeText(
                    binding.root.context, "${city.label} selected", Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                Toast.makeText(
                    binding.root.context, "No city found", Toast.LENGTH_SHORT
                ).show()
            }

            true
        }
    }

    private fun useCurrentLocation() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                DeviceLocationProvider.getCurrentLocation(this@LocationActivity)
            }

            when (result) {
                is DeviceLocationResult.Success -> {
                    val label = withContext(Dispatchers.IO) {
                        CitiesRepository.ensureLoaded(this@LocationActivity)
                        CitiesRepository.nearestCity(result.lat, result.lng)?.label
                    }
                    locationPrefs.save(
                        result.lat,
                        result.lng,
                        label ?: "Unknown City",
                        LocationPrefs.Source.GPS
                    )
                    Toast.makeText(
                        this@LocationActivity, "Location updated", Toast.LENGTH_SHORT
                    ).show()
                    finish()   // back to Home → onResume re-binds the row
                }

                DeviceLocationResult.PermissionDenied -> Toast.makeText(
                    this@LocationActivity, "Permission denied", Toast.LENGTH_SHORT
                ).show()

                DeviceLocationResult.Unavailable -> Toast.makeText(
                    this@LocationActivity,
                    "Couldn't get location. Check GPS is on.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}