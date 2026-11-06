package com.infinitybd71.salah_time.ui.components

import android.view.View
import com.infinitybd71.salah_time.R
import com.infinitybd71.salah_time.databinding.ItemPrayerBinding
import com.infinitybd71.salah_time.utils.formatPrayerTime

fun ItemPrayerBinding.setup(name: String, time: String, isNext: Boolean, isAlarmSet: Boolean = false) {
    this.wakthName.text = name
    this.wakthTime.text = formatPrayerTime(time.trim())

    // Change notification icon based on alarm state
    this.notificationBtn.text = if (isAlarmSet) "notifications_active" else "notifications"

    if (isNext) {
        this.prayerContainer.setBackgroundResource(
            R.drawable.rounded_corners_bg_primary
        )
        this.wakthName.setTextColor(root.context.getColor(R.color.text_on_primary_secondary))


        this.wakthTime.setTextColor(root.context.getColor(R.color.text_on_primary_secondary))

        this.notificationBtn.setTextColor(root.context.getColor(R.color.white))

        this.nextSalah.visibility = View.VISIBLE
    } else {
        this.notificationBtn.setTextColor(root.context.getColor(R.color.text_secondary))
        this.nextSalah.visibility = View.GONE
    }
}