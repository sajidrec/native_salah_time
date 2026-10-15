package com.infinitybd71.salah_time.ui.components

import android.view.View
import com.infinitybd71.salah_time.R
import com.infinitybd71.salah_time.databinding.ItemPrayerBinding
import com.infinitybd71.salah_time.utils.formatPrayerTime

fun ItemPrayerBinding.setup(name: String, time: String, isNext: Boolean) {
    this.wakthName.text = name
    this.wakthTime.text = formatPrayerTime(time.trim())

    if (isNext) {
        this.prayerContainer.setBackgroundResource(
            R.drawable.rounded_corners_bg_primary
        )
        this.wakthName.setTextColor(root.context.getColor(R.color.text_on_primary_secondary))


        this.wakthTime.setTextColor(root.context.getColor(R.color.text_on_primary_secondary))

        this.notificationBtn.setTextColor(root.context.getColor(R.color.text_on_primary_secondary))

        this.nextSalah.visibility = View.VISIBLE
    }
    else{
        this.nextSalah.visibility = View.GONE
    }
}