package com.example.deliverycalculator2.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<ClipboardEntry>>

    @Query("SELECT * FROM clipboard_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getEntriesForDate(startOfDay: Date, endOfDay: Date): Flow<List<ClipboardEntry>>

    @Insert
    suspend fun insert(entry: ClipboardEntry)

    @Query("DELETE FROM clipboard_entries WHERE timestamp < :cutoffDate")
    suspend fun deleteOldEntries(cutoffDate: Date)

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