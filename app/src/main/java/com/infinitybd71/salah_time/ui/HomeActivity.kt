package com.infinitybd71.salah_time.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.infinitybd71.salah_time.R
import com.infinitybd71.salah_time.data.LocationPrefs
import com.infinitybd71.salah_time.data.remote.RetrofitInstance
import com.infinitybd71.salah_time.databinding.ActivityHomeBinding
import com.infinitybd71.salah_time.ui.components.setup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var locationPrefs: LocationPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        locationPrefs = LocationPrefs(this@HomeActivity)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.dateRow.setup(
            icon = "calendar_today", title = "Wednesday, 07 October", subtitle = "2026"
        )

        binding.fajr.setup(
            name = "Fajr", time = "4:38 AM", isNext = true
        )
        binding.dhuhr.setup(
            name = "Dhuhr", time = "11:11 AM", isNext = false
        )
        binding.asr.setup(
            name = "Asr", time = "4:38 AM", isNext = false
        )
        binding.magrib.setup(
            name = "Magrib", time = "4:38 AM", isNext = false
        )
        binding.esha.setup(
            name = "Esha", time = "4:38 AM", isNext = false
        )

        val selectedDate = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM", Locale.ENGLISH)

        fun loadPrayerTimes() {
            val lat = if (locationPrefs.isSet) locationPrefs.lat else 23.8103   // Dhaka fallback
            val lng = if (locationPrefs.isSet) locationPrefs.lng else 90.4125
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getPrayerTimes(
                            selectedDate.time.toString(), lat, lng
                        )
                    }
                    Log.d("testing", "sajid testing ${response.data.timings}")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@HomeActivity, "Couldn't load prayer times", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        loadPrayerTimes()

        binding.dateRow.title.text = dateFormat.format(selectedDate.time)
        binding.dateRow.subtitle.text = selectedDate.get(Calendar.YEAR).toString()

        binding.dateRow.fullRowId.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate.set(year, month, day)

                    binding.dateRow.title.text = dateFormat.format(selectedDate.time)
                    binding.dateRow.subtitle.text = year.toString()
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.locationRow.fullRowId.setOnClickListener {
            val intent = Intent(this@HomeActivity, LocationActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        bindLocation()
    }

    private fun bindLocation() {
        val label = when {
            locationPrefs.isSet && !locationPrefs.label.isNullOrBlank() -> locationPrefs.label

            locationPrefs.isSet -> "Lat ${"%.4f".format(locationPrefs.lat)}, " + "Lng ${
                "%.4f".format(
                    locationPrefs.lng
                )
            }"

            else -> "Set your location"
        }
        binding.locationRow.setup(
            icon = "location_on", title = label ?: ""
        )
    }

}