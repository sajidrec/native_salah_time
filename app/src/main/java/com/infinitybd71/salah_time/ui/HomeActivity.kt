package com.infinitybd71.salah_time.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.infinitybd71.salah_time.R
import com.infinitybd71.salah_time.data.LocationPrefs
import com.infinitybd71.salah_time.data.remote.RetrofitInstance
import com.infinitybd71.salah_time.data.remote.model.Timings
import com.infinitybd71.salah_time.databinding.ActivityHomeBinding
import com.infinitybd71.salah_time.ui.components.setup
import com.infinitybd71.salah_time.utils.formatPrayerTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var locationPrefs: LocationPrefs

    private val selectedDate = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("EEEE, dd MMMM", Locale.ENGLISH)
    private val apiDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH) // Aladhan format

    /** True once at least one fetch has succeeded, so we know whether stale times are on screen. */
    private var hasLoadedData = false

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
            icon = "calendar_today",
            title = dateFormat.format(selectedDate.time),
            subtitle = selectedDate.get(Calendar.YEAR).toString()
        )

        binding.dateRow.fullRowId.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate.set(year, month, day)

                    binding.dateRow.title.text = dateFormat.format(selectedDate.time)
                    binding.dateRow.subtitle.text = year.toString()
                    loadPrayerTimes()
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

        binding.btnRefresh.setOnClickListener {
            loadPrayerTimes()
        }

        binding.btnRetry.setOnClickListener {
            loadPrayerTimes()
        }
    }

    override fun onResume() {
        super.onResume()
        bindLocation()
        loadPrayerTimes()
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

    private fun loadPrayerTimes() {
        val lat = if (locationPrefs.isSet) locationPrefs.lat else 23.8103   // Dhaka fallback
        val lng = if (locationPrefs.isSet) locationPrefs.lng else 90.4125
        val date = apiDateFormat.format(selectedDate.time)   // Aladhan expects dd-MM-yyyy

        hideFetchErrorBanner()
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getPrayerTimes(date, lat, lng)
                }
                hasLoadedData = true
                bindPrayerTimes(response.data.timings)
                hideFetchErrorBanner()
            } catch (e: Exception) {
                e.printStackTrace()
                showFetchErrorBanner()
            } finally {
                setLoading(false)
            }
        }
    }

    /** Shows a persistent banner explaining that the last fetch failed, until the next attempt. */
    private fun showFetchErrorBanner() {
        binding.bannerMessage.setText(
            if (hasLoadedData) R.string.fetch_failed_stale else R.string.fetch_failed_first
        )
        binding.fetchErrorBanner.visibility = View.VISIBLE
    }

    private fun hideFetchErrorBanner() {
        binding.fetchErrorBanner.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (loading) View.GONE else View.VISIBLE
        binding.btnRefresh.isEnabled = !loading
    }

    private fun bindPrayerTimes(t: Timings) {
        val fajr = t.Fajr.substringBefore(" ").trim()       // "05:42 (BST)" -> "05:42"
        val dhuhr = t.Dhuhr.substringBefore(" ").trim()
        val asr = t.Asr.substringBefore(" ").trim()
        val maghrib = t.Maghrib.substringBefore(" ").trim()
        val isha = t.Isha.substringBefore(" ").trim()

        val prayers = listOf(
            "Fajr" to fajr,
            "Dhuhr" to dhuhr,
            "Asr" to asr,
            "Magrib" to maghrib,
            "Isha" to isha
        )

        val next = nextPrayer(prayers)

        binding.fajr.setup(name = "Fajr", time = fajr, isNext = next?.first == "Fajr")
        binding.dhuhr.setup(name = "Dhuhr", time = dhuhr, isNext = next?.first == "Dhuhr")
        binding.asr.setup(name = "Asr", time = asr, isNext = next?.first == "Asr")
        binding.magrib.setup(name = "Magrib", time = maghrib, isNext = next?.first == "Magrib")
        binding.isha.setup(name = "Isha", time = isha, isNext = next?.first == "Isha")

        binding.nextPrayerName.text = next?.first ?: "--"
        binding.nextPrayerTime.text = formatPrayerTime(next?.second ?: "--")
    }

    /** First prayer later than the current clock time; wraps to Fajr (tomorrow) if all have passed. */
    private fun nextPrayer(prayers: List<Pair<String, String>>): Pair<String, String>? {
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        return prayers.firstOrNull { (_, time) ->
            val minutes = toMinutes(time)
            minutes != null && minutes > nowMinutes
        } ?: prayers.firstOrNull()
    }

    private fun toMinutes(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
    }

}