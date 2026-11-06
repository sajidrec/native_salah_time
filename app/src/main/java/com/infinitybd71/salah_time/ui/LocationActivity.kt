package com.infinitybd71.salah_time.ui

import android.Manifest
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import com.infinitybd71.salah_time.utils.ToastUtils
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
            ToastUtils.show(this, "Permission denied")
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
                ToastUtils.show(binding.root.context, "${city.label} selected")
                finish()
            } else {
                ToastUtils.show(binding.root.context, "No city found")
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
                    ToastUtils.show(this@LocationActivity, "Location updated")
                    finish()   // back to Home → onResume re-binds the row
                }

                DeviceLocationResult.PermissionDenied -> ToastUtils.show(this@LocationActivity, "Permission denied")

                DeviceLocationResult.Unavailable -> ToastUtils.show(this@LocationActivity, "Couldn't get location. Check GPS is on.")
            }
        }
    }
}