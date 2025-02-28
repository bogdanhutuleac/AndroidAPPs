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
import java.util.Calendar

data class ReportState(
    val startTime: TimeEntry = TimeEntry(12, 0),
    val endTime: TimeEntry = TimeEntry(0, 0),
    val extraAmount: String = "0.00",
    val isEditingExtra: Boolean = false,
    val unpaidTotal: Double = 0.0,
    val paidReceiptsCount: Int = 0,
    val unpaidReceiptsCount: Int = 0,
    val morningReceiptsCount: Int = 0,
    val selectedDate: SimpleDate = SimpleDate.now()
)

class ReportViewModel(private val database: AppDatabase) : ViewModel() {
    private val _state = MutableStateFlow(ReportState())
    val state: StateFlow<ReportState> = _state
    private val MORNING_TARGET = 10
    private val DELIVERY_PRICE = 3.0

    init {
        loadReceiptData(state.value.selectedDate)
    }

    fun updateStartTime(time: TimeEntry) {
        // If start time is changed to 17:00 or later, reset extra amount to "0.00" and isEditingExtra to false
        if (time.hour >= 17) {
            _state.update { it.copy(
                startTime = time,
                extraAmount = "0.00",
                isEditingExtra = false
            ) }
        } else {
            _state.update { it.copy(startTime = time) }
        }
        // Reload receipt data when start time changes to recalculate morning receipts
        loadReceiptData(state.value.selectedDate)
    }

    fun updateEndTime(time: TimeEntry) {
        _state.update { it.copy(endTime = time) }
        // Reload receipt data when end time changes to recalculate morning receipts
        loadReceiptData(state.value.selectedDate)
    }

    fun updateExtraAmount(amount: String) = _state.update { it.copy(extraAmount = amount) }

    fun updateIsEditingExtra(isEditing: Boolean) = _state.update { it.copy(isEditingExtra = isEditing) }

    private fun calculateExtraAmount(morningCount: Int): String {
        return if (morningCount < MORNING_TARGET) {
            val difference = MORNING_TARGET - morningCount
            String.format("%.2f", difference * DELIVERY_PRICE)
        } else {
            "0.00"
        }
    }

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
                    
                    // Get the user's start hour (default is 12)
                    val startHour = state.value.startTime.hour
                    
                    // Calculate morning receipts (between user's start time and 17:00)
                    val morningCount = if (startHour >= 17) {
                        // If user starts at or after 17:00, there are no morning receipts
                        0
                    } else {
                        entries.count { entry ->
                            val calendar = Calendar.getInstance().apply {
                                time = entry.timestamp
                            }
                            val hour = calendar.get(Calendar.HOUR_OF_DAY)
                            // Only count as morning receipt if hour is between user's start time and 16 (before 17:00)
                            hour in startHour..16
                        }
                    }
                    
                    // Calculate extra amount based on morning count
                    val extraAmount = calculateExtraAmount(morningCount)
                    
                    println("Debug: Found $paidCount paid receipts and $unpaidCount unpaid receipts")
                    println("Debug: User start time: ${state.value.startTime}")
                    println("Debug: Morning receipts (${startHour}:00-17:00): $morningCount")
                    println("Debug: Calculated extra amount: $extraAmount")
                    println("Debug: Unpaid total is $unpaidSum")
                    
                    _state.update { currentState ->
                        // If start time is 17:00 or later, always set extraAmount to "0.00" unless user is currently editing it
                        val finalExtraAmount = if (startHour >= 17) {
                            if (currentState.isEditingExtra) currentState.extraAmount else "0.00"
                        } else {
                            if (currentState.isEditingExtra) currentState.extraAmount else extraAmount
                        }
                        
                        currentState.copy(
                            paidReceiptsCount = paidCount,
                            unpaidReceiptsCount = unpaidCount,
                            morningReceiptsCount = morningCount,
                            unpaidTotal = unpaidSum,
                            extraAmount = finalExtraAmount,
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