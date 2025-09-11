package com.epic.documentmanager.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    const val FORMAT_DATE = "dd/MM/yyyy"
    const val FORMAT_DATETIME = "dd/MM/yyyy HH:mm"
    const val FORMAT_TIME = "HH:mm"
    const val FORMAT_MONTH_YEAR = "MM/yyyy"
    const val FORMAT_YEAR_MONTH = "yyyy-MM"

    private val dateFormat = SimpleDateFormat(FORMAT_DATE, Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat(FORMAT_DATETIME, Locale.getDefault())
    private val timeFormat = SimpleDateFormat(FORMAT_TIME, Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat(FORMAT_MONTH_YEAR, Locale.getDefault())
    private val yearMonthFormat = SimpleDateFormat(FORMAT_YEAR_MONTH, Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long): String {
        return monthYearFormat.format(Date(timestamp))
    }

    fun getCurrentMonth(): String {
        return SimpleDateFormat("MM", Locale.getDefault()).format(Date())
    }

    fun getCurrentYear(): String {
        return SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    }

    fun getMonthName(month: String): String {
        return when (month) {
            "01" -> "Januari"
            "02" -> "Februari"
            "03" -> "Maret"
            "04" -> "April"
            "05" -> "Mei"
            "06" -> "Juni"
            "07" -> "Juli"
            "08" -> "Agustus"
            "09" -> "September"
            "10" -> "Oktober"
            "11" -> "November"
            "12" -> "Desember"
            else -> "Unknown"
        }
    }

    fun getStartOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getEndOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        return calendar.timeInMillis
    }
}