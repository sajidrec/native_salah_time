package com.infinitybd71.salah_time.ui.components

import android.view.View

import com.infinitybd71.salah_time.databinding.ViewInfoRowBinding

fun ViewInfoRowBinding.setup(
    icon: String,
    title: String,
    subtitle: String? = null
) {
    this.icon.text = icon
    this.title.text = title
    this.subtitle.text = subtitle
    this.subtitle.visibility =
        if (subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
}