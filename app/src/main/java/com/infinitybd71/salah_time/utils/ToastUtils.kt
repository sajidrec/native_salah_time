package com.infinitybd71.salah_time.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.infinitybd71.salah_time.R

/**
 * Shows a toast using the app-branded custom layout:
 *  - icon: @drawable/ic_launcher_foreground
 *  - background: @color/ic_launcher_background (#FFFFFF)
 *
 * Every toast in the app should route through here so the look is consistent.
 */
object ToastUtils {
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.toast_custom, null)
        view.findViewById<TextView>(R.id.toast_message).text = message

        Toast(context).apply {
            setView(view)
            this.duration = duration
        }.show()
    }

    fun show(context: Context, messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
        show(context, context.getString(messageResId), duration)
    }
}
