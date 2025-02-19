package com.example.deliverycalculator2

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.deliverycalculator2.data.AppDatabase
import com.example.deliverycalculator2.data.ClipboardEntry
import com.example.deliverycalculator2.screens.ReportScreen
import com.example.deliverycalculator2.worker.CleanupWorker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalDate
import java.util.*

enum class Screen {
    Home, Report
}

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var clipboardManager: ClipboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        database = AppDatabase.getDatabase(this)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        
        setupCleanupWorker()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(database)
                }
            }
        }
    }

    private fun setupCleanupWorker() {
        val cleanupWorkRequest = OneTimeWorkRequestBuilder<CleanupWorker>()
            .build()
        
        WorkManager.getInstance(applicationContext)
            .enqueue(cleanupWorkRequest)
    }
}

private fun parseReceipt(text: String): ClipboardEntry? {
    try {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        // Find delivery address based on receipt format
        var deliveryAddress = ""
        var subtotal = 0.0
        var foundSubtotal = false
        var isPaid = false
        
        // Determine receipt type
        val isJustEat = lines.firstOrNull()?.contains("JUST EAT", ignoreCase = true) == true
        val isShopReceipt = lines.any { it.contains("www.sanmarino.ie", ignoreCase = true) }
        
        when {
            isJustEat -> {
                // Just Eat format
                var customerDetailsIndex = -1
                
                for (i in lines.indices) {
                    val line = lines[i]
                    when {
                        // Find customer details line
                        line.equals("Customer details:", ignoreCase = true) -> {
                            customerDetailsIndex = i
                            // Get the next line as address (skipping empty lines)
                            if (i + 1 < lines.size) {
                                val addressLines = mutableListOf<String>()
                                var j = i + 1
                                while (j < lines.size && 
                                      !lines[j].startsWith("To contact") && 
                                      !lines[j].startsWith("verification code") &&
                                      !lines[j].startsWith("Previous orders")) {
                                    addressLines.add(lines[j])
                                    j++
                                }
                                deliveryAddress = addressLines.joinToString(", ")
                            }
                        }
                        // Find subtotal
                        line.equals("Subtotal", ignoreCase = true) && i + 1 < lines.size -> {
                            if (!foundSubtotal) {
                                val nextLine = lines[i + 1]
                                subtotal = nextLine.replace("EUR", "")
                                    .replace(",", ".")
                                    .trim()
                                    .toDoubleOrNull() ?: 0.0
                                foundSubtotal = true
                            }
                        }
                        // Check for paid status
                        line.contains("ORDER HAS BEEN PAID", ignoreCase = true) -> {
                            isPaid = true
                        }
                    }
                }
            }
            isShopReceipt -> {
                // Shop receipt format (with www.sanmarino.ie)
                for (i in lines.indices) {
                    if (lines[i].startsWith("Phone:", ignoreCase = true) && i > 0) {
                        deliveryAddress = lines[i - 1]
                        break
                    }
                }
                
                // Find subtotal and paid status
                for (i in lines.indices) {
                    val line = lines[i].lowercase()
                    when {
                        (line.contains("subtotal") || line.contains("sabtotal")) && i + 1 < lines.size -> {
                            if (!foundSubtotal) {
                                val nextLine = lines[i + 1]
                                subtotal = nextLine.replace("€", "")
                                    .replace(",", ".")
                                    .trim()
                                    .toDoubleOrNull() ?: 0.0
                                foundSubtotal = true
                            }
                        }
                        line.contains("payment") || line.contains("paid") -> {
                            isPaid = true
                        }
                    }
                }
            }
            else -> {
                // Online receipt format
                var acceptedIndex = -1
                var phoneNumberIndex = -1
                
                for (i in lines.indices) {
                    val line = lines[i]
                    if (line.contains("Accepted:", ignoreCase = true)) {
                        acceptedIndex = i
                    } else if (line.matches(Regex("\\d{10}"))) {
                        phoneNumberIndex = i
                        break
                    }
                }
                
                if (acceptedIndex != -1 && phoneNumberIndex != -1 && acceptedIndex < phoneNumberIndex) {
                    val addressLines = lines.slice((acceptedIndex + 1) until phoneNumberIndex)
                    deliveryAddress = addressLines.joinToString(", ")
                }
                
                // Find subtotal and paid status
                for (i in lines.indices) {
                    val line = lines[i].lowercase()
                    when {
                        (line.contains("subtotal") || line.contains("sabtotal")) && i + 1 < lines.size -> {
                            if (!foundSubtotal) {
                                val nextLine = lines[i + 1]
                                subtotal = nextLine.replace("€", "")
                                    .replace(",", ".")
                                    .trim()
                                    .toDoubleOrNull() ?: 0.0
                                foundSubtotal = true
                            }
                        }
                        line.contains("payment") || line.contains("paid") -> {
                            isPaid = true
                        }
                    }
                }
            }
        }

        // Debug logging
        println("Parsed Receipt Data:")
        println("Receipt Type: ${when {
            isJustEat -> "Just Eat"
            isShopReceipt -> "Shop Receipt"
            else -> "Online Receipt"
        }}")
        println("Address: $deliveryAddress")
        println("Subtotal: $subtotal")
        println("Paid: ${if (isPaid) "Yes" else "No"}")

        return ClipboardEntry(
            content = text,
            deliveryAddress = deliveryAddress,
            subtotal = subtotal,
            total = 0.0, // Set total to 0 since we're not using it
            isPaid = isPaid
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(database: AppDatabase) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var tempSelectedDate by remember { mutableStateOf(selectedDate) }

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
                Screen.Report -> ReportScreen()
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = tempSelectedDate.toEpochDay() * 24 * 60 * 60 * 1000
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                tempSelectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
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

@Composable
fun ClipboardScreen(database: AppDatabase, selectedDate: LocalDate) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries = remember { mutableStateOf<List<ClipboardEntry>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<ClipboardEntry?>(null) }

    LaunchedEffect(selectedDate) {
        database.clipboardDao().getAllEntries().collectLatest { allEntries ->
            entries.value = allEntries.filter { entry ->
                entry.timestamp.toLocalDate() == selectedDate
            }
        }
    }

    if (showDeleteDialog && entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                entryToDelete = null
            },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete this entry?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                entryToDelete?.let { entry ->
                                    database.clipboardDao().deleteEntry(entry.id)
                                    // Update the entries list after deletion
                                    val updatedEntries = entries.value.filter { it.id != entry.id }
                                    entries.value = updatedEntries
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            showDeleteDialog = false
                            entryToDelete = null
                        }
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        entryToDelete = null
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                scope.launch {
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = clipboardManager.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val text = clipData.getItemAt(0).text.toString()
                        parseReceipt(text)?.let { entry ->
                            database.clipboardDao().insert(entry)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Save Receipt from Clipboard")
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(entries.value) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Delivery Address:",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(entry.deliveryAddress)
                            }
                            IconButton(
                                onClick = {
                                    entryToDelete = entry
                                    showDeleteDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete entry",
                                    tint = Color.Red
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Subtotal:",
                                    fontWeight = FontWeight.Bold
                                )
                                var isEditingSubtotal by remember { mutableStateOf(false) }
                                var subtotalText by remember { mutableStateOf(entry.subtotal.toString()) }
                                if (isEditingSubtotal) {
                                    AlertDialog(
                                        onDismissRequest = { isEditingSubtotal = false },
                                        title = { Text("Edit Subtotal") },
                                        text = {
                                            OutlinedTextField(
                                                value = subtotalText,
                                                onValueChange = { subtotalText = it },
                                                singleLine = true,
                                                label = { Text("Subtotal") }
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        val newSubtotal = subtotalText.toDoubleOrNull() ?: entry.subtotal
                                                        database.clipboardDao().updateSubtotal(entry.id, newSubtotal)
                                                        entries.value = entries.value.map {
                                                            if (it.id == entry.id) it.copy(subtotal = newSubtotal) else it
                                                        }
                                                        isEditingSubtotal = false
                                                    }
                                                }
                                            ) {
                                                Text("Save")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { isEditingSubtotal = false }) {
                                                Text("Cancel")
                                            }
                                        }
                                    )
                                } else {
                                    Text(
                                        text = String.format(Locale.US, "€%.2f", entry.subtotal),
                                        modifier = Modifier.clickable { isEditingSubtotal = true }
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Paid:",
                                    fontWeight = FontWeight.Bold
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(if (entry.isPaid) "Yes" else "No")
                                    Switch(
                                        checked = entry.isPaid,
                                        onCheckedChange = { isChecked ->
                                            scope.launch {
                                                database.clipboardDao().updatePaidStatus(entry.id, isChecked)
                                                // Update the UI immediately
                                                entries.value = entries.value.map { 
                                                    if (it.id == entry.id) it.copy(isPaid = isChecked) else it 
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = entry.timestamp.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}