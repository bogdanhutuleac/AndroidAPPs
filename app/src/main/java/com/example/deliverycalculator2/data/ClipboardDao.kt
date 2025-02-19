package com.example.deliverycalculator2.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<ClipboardEntry>>

    @Query("SELECT * FROM clipboard_entries WHERE date(timestamp) = date(:date) ORDER BY timestamp DESC")
    fun getEntriesForDate(date: LocalDateTime): Flow<List<ClipboardEntry>>

    @Insert
    suspend fun insert(entry: ClipboardEntry)

    @Query("DELETE FROM clipboard_entries WHERE timestamp < :cutoffDate")
    suspend fun deleteOldEntries(cutoffDate: LocalDateTime)

    @Query("DELETE FROM clipboard_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("DELETE FROM clipboard_entries")
    suspend fun deleteAll()

    @Query("UPDATE clipboard_entries SET isPaid = :isPaid WHERE id = :entryId")
    suspend fun updatePaidStatus(entryId: Long, isPaid: Boolean)

    @Query("UPDATE clipboard_entries SET subtotal = :subtotal WHERE id = :entryId")
    suspend fun updateSubtotal(entryId: Long, subtotal: Double)

    @Query("UPDATE clipboard_entries SET total = :total WHERE id = :entryId")
    suspend fun updateTotal(entryId: Long, total: Double)
} 