package com.example.deliverycalculator2.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.deliverycalculator2.utils.SimpleDate
import com.example.deliverycalculator2.utils.TimeEntry
import com.example.deliverycalculator2.viewmodels.ReportViewModel
import java.text.NumberFormat
import java.util.*
import kotlin.math.ceil

private fun calculateWorkingHours(start: TimeEntry, end: TimeEntry): Double {
    val startMinutes = start.toComparableMinutes()
    val endMinutes = end.toComparableMinutes()
    
    // Handle special case when end time is 00:00 (midnight)
    val adjustedEndMinutes = if (end.hour == 0 && end.minute == 0) {
        24 * 60  // Convert to minutes (24 hours * 60 minutes)
    } else {
        endMinutes
    }
    
    return (adjustedEndMinutes - startMinutes).toDouble() / 60.0
}

@Composable
fun ReportScreen(selectedDate: SimpleDate) {
    val viewModel = remember { ReportViewModel.getInstance() }
    val state by viewModel.state.collectAsState()
    
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showTotalDialog by remember { mutableStateOf(false) }
    
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.GERMANY) }

    LaunchedEffect(selectedDate) {
        viewModel.updateSelectedDate(selectedDate)
    }

    val scrollState = rememberScrollState()

    val allTimeEntries = remember {
        buildList {
            for (hour in 0..23) {
                add(TimeEntry(hour, 0))
                add(TimeEntry(hour, 30))
            }
            add(TimeEntry(0, 0))
        }
    }

    // Debug prints to check values
    LaunchedEffect(state) {
        println("Start Time: ${state.startTime}")
        println("End Time: ${state.endTime}")
        println("Working Hours: ${calculateWorkingHours(state.startTime, state.endTime)}")
        println("Unpaid Total: ${state.unpaidTotal}")
        println("Paid Receipts Count: ${state.paidReceiptsCount}")
    }

    val workingHours = calculateWorkingHours(state.startTime, state.endTime)
    val hourlyRate = 5.0
    val hoursPayment = ceil(workingHours * hourlyRate)
    val paidReceiptRate = 3.0
    val paidReceiptsDeduction = state.paidReceiptsCount * paidReceiptRate
    val extraAmountValue = state.extraAmount.toDoubleOrNull() ?: 0.0
    val finalTotal = state.unpaidTotal - hoursPayment - paidReceiptsDeduction - extraAmountValue

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
                            Text(state.startTime.toString())
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
                            Text(state.endTime.toString())
                        }
                    }
                }
            }
        }

        // Hours Section
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
                            text = String.format(Locale.getDefault(),"%.1f", workingHours),
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

        // Amount Section
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
                    text = currencyFormatter.format(state.unpaidTotal),
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
                    if (state.isEditingExtra) {
                        OutlinedTextField(
                            value = state.extraAmount,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    viewModel.updateExtraAmount(newValue)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                        onClick = { viewModel.updateIsEditingExtra(!state.isEditingExtra) },
                        modifier = Modifier.align(if (state.isEditingExtra) Alignment.Bottom else Alignment.CenterVertically)
                    ) {
                        Text(if (state.isEditingExtra) "Save" else "Edit")
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
            Dialog(
                onDismissRequest = { showTotalDialog = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { showTotalDialog = false },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Amount
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Amount:",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormatter.format(state.unpaidTotal),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Receipt counts
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Total Receipts:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = (state.paidReceiptsCount + state.unpaidReceiptsCount).toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Update the morning time range to show the user's actual start time
                                    val startHour = state.startTime.hour
                                    val morningLabel = if (startHour >= 17) {
                                        "Morning (N/A):"
                                    } else {
                                        "Morning (${startHour}:00-17:00):"
                                    }
                                    
                                    Text(
                                        morningLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = state.morningReceiptsCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Deductions section
                        Text(
                            "Deductions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Hours Payment
                        DeductionItem(
                            label = "Hours Payment:",
                            amount = hoursPayment,
                            currencyFormatter = currencyFormatter
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Paid Receipts
                        DeductionItem(
                            label = "Paid Receipts (${state.paidReceiptsCount} × €${paidReceiptRate.toInt()}):",
                            amount = paidReceiptsDeduction,
                            currencyFormatter = currencyFormatter
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Extra
                        DeductionItem(
                            label = "Extra:",
                            amount = extraAmountValue,
                            currencyFormatter = currencyFormatter
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Final Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Final Total:",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currencyFormatter.format(finalTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (finalTotal >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
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
                        items(allTimeEntries.filter { it.toComparableMinutes() < state.endTime.toComparableMinutes() }) { time ->
                            TextButton(
                                onClick = {
                                    viewModel.updateStartTime(time)
                                    if (time.toComparableMinutes() >= state.endTime.toComparableMinutes()) {
                                        val nextIndex = allTimeEntries.indexOf(time) + 1
                                        if (nextIndex < allTimeEntries.size) {
                                            viewModel.updateEndTime(allTimeEntries[nextIndex])
                                        } else {
                                            viewModel.updateEndTime(TimeEntry(0, 0))
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
                                time.toComparableMinutes() > state.startTime.toComparableMinutes()
                            }
                        ) { time ->
                            TextButton(
                                onClick = {
                                    viewModel.updateEndTime(time)
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

@Composable
private fun DeductionItem(
    label: String,
    amount: Double,
    currencyFormatter: NumberFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "- " + currencyFormatter.format(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        )
    }
} 