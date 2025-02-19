package com.example.deliverycalculator2.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.deliverycalculator2.data.AppDatabase
import com.example.deliverycalculator2.utils.SimpleDate
import com.example.deliverycalculator2.viewmodels.ReportViewModel

enum class Screen {
    Home, Report
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(database: AppDatabase) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var selectedDate by remember { mutableStateOf(SimpleDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var tempSelectedDate by remember { mutableStateOf(selectedDate) }

    // Initialize ReportViewModel
    LaunchedEffect(Unit) {
        ReportViewModel.initialize(database)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deliveries - ${selectedDate.toString()}") },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Select Date")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentScreen == Screen.Home,
                    onClick = { currentScreen = Screen.Home }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Description, contentDescription = "Report") },
                    label = { Text("Report") },
                    selected = currentScreen == Screen.Report,
                    onClick = { currentScreen = Screen.Report }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.Home -> ClipboardScreen(database, selectedDate)
                Screen.Report -> ReportScreen(selectedDate)
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.toMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                tempSelectedDate = SimpleDate.fromMillis(millis)
                            }
                            selectedDate = tempSelectedDate
                            showDatePicker = false
                        }
                    ) {
                        Text(
                            "Set Date",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDatePicker = false }
                    ) {
                        Text(
                            "Cancel",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        todayDateBorderColor = MaterialTheme.colorScheme.primary,
                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
} 