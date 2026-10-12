package com.infinitybd71.salah_time.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.infinitybd71.salah_time.R
import com.infinitybd71.salah_time.databinding.ActivityLocationBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class LocationActivity : AppCompatActivity() {

    lateinit var binding: ActivityLocationBinding

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


        binding.appbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        lifecycleScope.launch {
            val cities = withContext(Dispatchers.IO) {
                val json = assets.open("cities.json")
                    .bufferedReader()
                    .use { it.readText() }

                JSONArray(json)
            }

            binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                    androidx.core.view.WindowCompat.getInsetsController(window, binding.searchInput)
                        .hide(WindowInsetsCompat.Type.ime())

                    val searchBoxTxtLowerCase = binding.searchInput.text
                        .toString()
                        .trim()
                        .lowercase()

                    var left = 0
                    var right = cities.length() - 1
                    var foundIndex = -1

                    while (left <= right) {
                        val mid = left + (right - left) / 2
                        val cityName = cities.getJSONArray(mid).getString(0)

                        val comparison = cityName.compareTo(searchBoxTxtLowerCase)

                        when {
                            comparison == 0 -> {
                                foundIndex = mid
                                break
                            }

                            comparison < 0 -> left = mid + 1
                            else -> right = mid - 1
                        }
                    }

                    val message = if (foundIndex != -1) {
                        val city = cities.getJSONArray(foundIndex)
                        val lat = city.getDouble(1)
                        val lng = city.getDouble(2)
                        "Latitude: $lat\nLongitude: $lng"
                    } else {
                        "No city found"
                    }

                    Toast.makeText(
                        binding.root.context,
                        message,
                        Toast.LENGTH_LONG
                    ).show()

                    true
                } else {
                    false
                }
            }
        }
    }
}