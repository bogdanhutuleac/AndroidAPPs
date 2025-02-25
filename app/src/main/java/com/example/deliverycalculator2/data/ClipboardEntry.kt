package com.example.deliverycalculator2.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date
import java.util.Calendar

@Entity(tableName = "clipboard_entries")
data class ClipboardEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val deliveryAddress: String,
    val subtotal: Double,
    val total: Double,
    val isPaid: Boolean = false,
    val phoneNumber: String = "",
    val maskingCode: String = "",
    @ColumnInfo(name = "timestamp")
    val timestamp: Date = adjustTimestampForLateNight(Date())
) {
    companion object {
        fun adjustTimestampForLateNight(date: Date): Date {
            val calendar = Calendar.getInstance()
            calendar.time = date
            
            // If time is between 00:00 and 00:30, adjust to previous day at 23:59:59
            if (calendar.get(Calendar.HOUR_OF_DAY) == 0 && 
                calendar.get(Calendar.MINUTE) <= 30) {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
            }
            
            return calendar.time
        }
    }
} 