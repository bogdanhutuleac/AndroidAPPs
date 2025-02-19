package com.example.deliverycalculator2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.deliverycalculator2.data.AppDatabase
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

// Time entry data class moved outside the composable
private data class TimeEntry(val hour: Int, val minute: Int) {
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

private fun calculateWorkingHours(start: TimeEntry, end: TimeEntry): Double {
    val startMinutes = start.toComparableMinutes()
    val endMinutes = end.toComparableMinutes()
    return (endMinutes - startMinutes) / 60.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen() {
    var startTime by remember { mutableStateOf(TimeEntry(12, 0)) }
    var endTime by remember { mutableStateOf(TimeEntry(0, 0)) }  // 00:00 represents end of day
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var extraAmount by remember { mutableStateOf("0.00") }
    var isEditingExtra by remember { mutableStateOf(true) }
    var showSavedMessage by remember { mutableStateOf(false) }
    var unpaidTotal by remember { mutableStateOf(0.0) }
    var showTotalDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    var paidReceiptsCount by remember { mutableStateOf(0) }
    var unpaidReceiptsCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY) }

    // Collect the count of paid receipts and calculate unpaid total
    LaunchedEffect(Unit) {
        database.clipboardDao().getAllEntries()
            .collect { entries ->
                paidReceiptsCount = entries.count { it.isPaid }
                unpaidReceiptsCount = entries.count { !it.isPaid }
                unpaidTotal = entries
                    .filter { !it.isPaid }
                    .sumOf { it.subtotal }
            }
    }

    val scrollState = rememberScrollState()

    // Generate all possible time entries
    val allTimeEntries = remember {
        buildList {
            for (hour in 0..23) {
                add(TimeEntry(hour, 0))
                add(TimeEntry(hour, 30))
            }
            add(TimeEntry(0, 0))  // Add 00:00 as the last option
        }
    }

    // Calculate working hours and payment
    val workingHours = calculateWorkingHours(startTime, endTime)
    val hourlyRate = 5 // 5 euros per hour
    val hoursPayment = ceil(workingHours * hourlyRate)
    
    // Calculate paid receipts deduction
    val paidReceiptRate = 3 // 3 euros per paid receipt
    val paidReceiptsDeduction = paidReceiptsCount * paidReceiptRate
    
    // Parse extra amount
    val extraAmountValue = extraAmount.toDoubleOrNull() ?: 0.0
    
    // Calculate final total
    val finalTotal = unpaidTotal - hoursPayment - paidReceiptsDeduction - extraAmountValue

    // Calculate total receipts (paid + unpaid)
    val totalReceipts = remember(paidReceiptsCount, unpaidReceiptsCount) {
        paidReceiptsCount + unpaidReceiptsCount
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start
    ) {
        // Working Hours Section
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Working Hours",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Start Time Selection
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Start Time",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(startTime.toString())
                        }
                    }

                    // End Time Selection
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "End Time",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(endTime.toString())
                        }
                    }
                }
            }
        }

        // Hours Section (New)
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Hours",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Working Hours",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = String.format("%.1f", workingHours),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Payment",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = currencyFormatter.format(hoursPayment),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
        }

        // Amount Section (New)
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currencyFormatter.format(unpaidTotal),
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        // Extra Amount Section
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Extra",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isEditingExtra) {
                        OutlinedTextField(
                            value = extraAmount,
                            onValueChange = { newValue ->
                                // Only allow valid decimal numbers with up to 2 decimal places
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    extraAmount = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            prefix = { Text("€") }
                        )
                    } else {
                        Text(
                            text = currencyFormatter.format(extraAmountValue),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            isEditingExtra = !isEditingExtra
                        },
                        modifier = Modifier.align(if (isEditingExtra) Alignment.Bottom else Alignment.CenterVertically)
                    ) {
                        Text(if (isEditingExtra) "Save" else "Edit")
                    }
                }
            }
        }

        // Show Total Button
        Button(
            onClick = { showTotalDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Show Total")
        }

        // Total Dialog
        if (showTotalDialog) {
            AlertDialog(
                onDismissRequest = { showTotalDialog = false },
                modifier = Modifier.padding(16.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { showTotalDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                },
                text = {
                    // Deductions list
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Receipts:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = (paidReceiptsCount + unpaidReceiptsCount).toString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Amount:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = currencyFormatter.format(unpaidTotal),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Hours Payment:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "- " + currencyFormatter.format(hoursPayment),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Paid Receipts (${paidReceiptsCount} × €${paidReceiptRate}):",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "- " + currencyFormatter.format(paidReceiptsDeduction),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Extra:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "- " + currencyFormatter.format(extraAmountValue),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Final Total:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = currencyFormatter.format(finalTotal),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }

        // Time Picker Dialogs
        if (showStartTimePicker) {
            AlertDialog(
                onDismissRequest = { showStartTimePicker = false },
                title = { Text("Select Start Time") },
                text = {
                    LazyColumn(
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(allTimeEntries.filter { it.toComparableMinutes() < endTime.toComparableMinutes() }) { time ->
                            TextButton(
                                onClick = {
                                    startTime = time
                                    if (time.toComparableMinutes() >= endTime.toComparableMinutes()) {
                                        // Set end time to next available slot
                                        val nextIndex = allTimeEntries.indexOf(time) + 1
                                        endTime = if (nextIndex < allTimeEntries.size) {
                                            allTimeEntries[nextIndex]
                                        } else {
                                            TimeEntry(0, 0)
                                        }
                                    }
                                    showStartTimePicker = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(time.toString())
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showStartTimePicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showEndTimePicker) {
            AlertDialog(
                onDismissRequest = { showEndTimePicker = false },
                title = { Text("Select End Time") },
                text = {
                    LazyColumn(
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(
                            allTimeEntries.filter { time ->
                                time.toComparableMinutes() > startTime.toComparableMinutes()
                            }
                        ) { time ->
                            TextButton(
                                onClick = {
                                    endTime = time
                                    showEndTimePicker = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(time.toString())
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showEndTimePicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
} 