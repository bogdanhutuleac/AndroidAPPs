package com.example.deliverycalculator2.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliverycalculator2.data.AppDatabase
import com.example.deliverycalculator2.utils.SimpleDate
import com.example.deliverycalculator2.utils.TimeEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportState(
    val startTime: TimeEntry = TimeEntry(12, 0),
    val endTime: TimeEntry = TimeEntry(0, 0),
    val extraAmount: String = "0.00",
    val isEditingExtra: Boolean = false,
    val unpaidTotal: Double = 0.0,
    val paidReceiptsCount: Int = 0,
    val unpaidReceiptsCount: Int = 0,
    val selectedDate: SimpleDate = SimpleDate.now()
)

class ReportViewModel(private val database: AppDatabase) : ViewModel() {
    private val _state = MutableStateFlow(ReportState())
    val state: StateFlow<ReportState> = _state

    init {
        loadReceiptData(state.value.selectedDate)
    }

    fun updateStartTime(time: TimeEntry) = _state.update { it.copy(startTime = time) }

    fun updateEndTime(time: TimeEntry) = _state.update { it.copy(endTime = time) }

    fun updateExtraAmount(amount: String) = _state.update { it.copy(extraAmount = amount) }

    fun updateIsEditingExtra(isEditing: Boolean) = _state.update { it.copy(isEditingExtra = isEditing) }

    fun updateSelectedDate(date: SimpleDate) {
        if (date != state.value.selectedDate) {
            _state.update { 
                it.copy(
                    startTime = TimeEntry(12, 0),
                    endTime = TimeEntry(0, 0),
                    extraAmount = "0.00",
                    isEditingExtra = false,
                    selectedDate = date
                )
            }
            loadReceiptData(date)
        }
    }

    private fun loadReceiptData(date: SimpleDate) {
        viewModelScope.launch {
            try {
                database.clipboardDao().getEntriesForDate(
                    startOfDay = date.toStartOfDay(),
                    endOfDay = date.toEndOfDay()
                ).collect { entries ->
                    val paidCount = entries.count { it.isPaid }
                    val unpaidCount = entries.count { !it.isPaid }
                    val unpaidSum = entries.filter { !it.isPaid }
                        .sumOf { it.subtotal }
                    
                    println("Debug: Found $paidCount paid receipts and $unpaidCount unpaid receipts")
                    println("Debug: Unpaid total is $unpaidSum")
                    
                    _state.update { currentState ->
                        currentState.copy(
                            paidReceiptsCount = paidCount,
                            unpaidReceiptsCount = unpaidCount,
                            unpaidTotal = unpaidSum,
                            selectedDate = date
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Debug: Error loading receipt data: ${e.message}")
            }
        }
    }

    companion object {
        private var instance: ReportViewModel? = null

        fun getInstance(): ReportViewModel {
            return instance ?: throw IllegalStateException("ReportViewModel must be initialized first")
        }

        fun initialize(database: AppDatabase) {
            if (instance == null) {
                instance = ReportViewModel(database)
            }
        }
    }
} 