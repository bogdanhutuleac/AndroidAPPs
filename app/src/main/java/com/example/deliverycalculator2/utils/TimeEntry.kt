package com.example.deliverycalculator2.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimeEntry(val hour: Int, val minute: Int) : Parcelable {
    override fun toString(): String {
        // Special case for 24:00/00:00 and 24:30/00:30
        return when {
            (hour == 24 || hour == 0) && minute == 0 -> "00:00"
            (hour == 24 || hour == 0) && minute == 30 -> "00:30"
            else -> String.format("%02d:%02d", hour, minute)
        }
    }

    fun toComparableMinutes(): Int {
        return when {
            // Convert 00:00 to represent end of day (24:00)
            hour == 0 && minute == 0 -> 24 * 60
            // Convert 00:30 to represent 24:30
            hour == 0 && minute == 30 -> 24 * 60 + 30
            else -> hour * 60 + minute
        }
    }
} 