package com.example.deliverycalculator2.utils

import java.util.*

data class SimpleDate(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int
) {
    companion object {
        fun now(): SimpleDate {
            val calendar = Calendar.getInstance()
            return SimpleDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun fromMillis(millis: Long): SimpleDate {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = millis
            return SimpleDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    fun toStartOfDay(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    fun toEndOfDay(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    fun toMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth)
        return calendar.timeInMillis
    }

    fun toDate(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    override fun toString(): String {
        return String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleDate) return false
        return year == other.year && month == other.month && dayOfMonth == other.dayOfMonth
    }

    override fun hashCode(): Int {
        var result = year
        result = 31 * result + month
        result = 31 * result + dayOfMonth
        return result
    }
}

fun Date.toSimpleDate(): SimpleDate {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return SimpleDate(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
} 