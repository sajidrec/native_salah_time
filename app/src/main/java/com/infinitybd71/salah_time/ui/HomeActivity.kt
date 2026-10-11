package com.infinitybd71.salah_time.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.infinitybd71.salah_time.R
import com.infinitybd71.salah_time.databinding.ActivityHomeBinding
import com.infinitybd71.salah_time.ui.components.setup

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.locationRow.setup(
            icon = "location_on",
            title = "Dhaka, Bangladesh"
        )

        binding.dateRow.setup(
            icon = "calendar_today",
            title = "Wednesday, 07 October",
            subtitle = "2026"
        )

        binding.fajr.setup(
            name = "Fajr",
            time = "4:38 AM",
            isNext = true
        )
        binding.dhuhr.setup(
            name = "Dhuhr",
            time = "11:11 AM",
            isNext = false
        )
        binding.asr.setup(
            name = "Asr",
            time = "4:38 AM",
            isNext = false
        )
        binding.magrib.setup(
            name = "Magrib",
            time = "4:38 AM",
            isNext = false
        )
        binding.esha.setup(
            name = "Esha",
            time = "4:38 AM",
            isNext = false
        )
    }
}