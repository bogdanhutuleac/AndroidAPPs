package com.example.deliverycalculator2.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

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
    val timestamp: Date = Date()
) 