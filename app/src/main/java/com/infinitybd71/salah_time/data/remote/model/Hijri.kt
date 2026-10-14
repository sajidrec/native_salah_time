package com.infinitybd71.salah_time.data.remote.model

data class Hijri(
    val adjustedHolidays: List<Any?>,
    val date: String,
    val day: String,
    val designation: Designation,
    val format: String,
    val holidays: List<Any?>,
    val method: String,
    val month: MonthX,
    val weekday: WeekdayX,
    val year: String
)