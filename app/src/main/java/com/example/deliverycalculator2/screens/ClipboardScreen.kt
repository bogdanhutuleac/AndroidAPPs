package com.example.deliverycalculator2.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.deliverycalculator2.data.AppDatabase
import com.example.deliverycalculator2.data.ClipboardEntry
import com.example.deliverycalculator2.utils.SimpleDate
import com.example.deliverycalculator2.utils.ReceiptParser
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun ClipboardScreen(database: AppDatabase, selectedDate: SimpleDate) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries = remember { mutableStateOf<List<ClipboardEntry>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<ClipboardEntry?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate) {
        database.clipboardDao().getEntriesForDate(
            startOfDay = selectedDate.toStartOfDay(),
            endOfDay = selectedDate.toEndOfDay()
        ).collectLatest { dateEntries ->
            entries.value = dateEntries
        }
    }

    // Filter entries based on search query
    val filteredEntries = remember(entries.value, searchQuery) {
        if (searchQuery.isEmpty()) {
            entries.value
        } else {
            entries.value.filter { entry ->
                entry.deliveryAddress.contains(searchQuery, ignoreCase = true)
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
                                    entries.value = entries.value.filter { it.id != entry.id }
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
        // Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = clipboardManager.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val text = clipData.getItemAt(0).text.toString()
                            ReceiptParser.parseReceipt(text)?.let { entry ->
                                database.clipboardDao().insert(entry)
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Receipt from Clipboard")
            }

            FilledTonalIconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isSearchExpanded) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchExpanded) "Close search" else "Open search",
                    tint = if (isSearchExpanded)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Field
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = { 
                        Text(
                            "Search addresses...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Show search results count if searching
        AnimatedVisibility(
            visible = searchQuery.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = "${filteredEntries.size} results found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filteredEntries) { entry ->
                ReceiptCard(
                    entry = entry,
                    onDeleteClick = {
                        entryToDelete = entry
                        showDeleteDialog = true
                    },
                    onSubtotalUpdate = { newSubtotal ->
                        scope.launch {
                            database.clipboardDao().updateSubtotal(entry.id, newSubtotal)
                        }
                    },
                    onPaidStatusUpdate = { isPaid ->
                        scope.launch {
                            database.clipboardDao().updatePaidStatus(entry.id, isPaid)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ReceiptCard(
    entry: ClipboardEntry,
    onDeleteClick: () -> Unit,
    onSubtotalUpdate: (Double) -> Unit,
    onPaidStatusUpdate: (Boolean) -> Unit
) {
    var isEditingSubtotal by remember { mutableStateOf(false) }
    var subtotalText by remember { mutableStateOf(entry.subtotal.toString()) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header with Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Delivery Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.phoneNumber.isNotEmpty()) {
                        FilledTonalIconButton(
                            onClick = {
                                val phoneNumber = if (entry.maskingCode.isNotEmpty()) {
                                    // For JustEats receipts with masking code
                                    "${entry.phoneNumber},${entry.maskingCode}#"
                                } else {
                                    // For online receipts (no masking code)
                                    entry.phoneNumber
                                }
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call customer"
                            )
                        }
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete entry",
                            tint = Color.Red
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Address Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Delivery Address",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val uri = Uri.parse("https://maps.google.com/maps?q=${Uri.encode(entry.deliveryAddress)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.deliveryAddress,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Open in Maps",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Subtotal Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Subtotal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        modifier = Modifier.clickable { isEditingSubtotal = true },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = String.format(Locale.US, "€%.2f", entry.subtotal),
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Payment Status Column
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Payment Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (entry.isPaid) "Paid" else "Not Paid",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (entry.isPaid) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                        Switch(
                            checked = entry.isPaid,
                            onCheckedChange = onPaidStatusUpdate,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // Timestamp at the bottom
            Text(
                text = entry.timestamp.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    // Subtotal Edit Dialog
    if (isEditingSubtotal) {
        AlertDialog(
            onDismissRequest = { isEditingSubtotal = false },
            title = { Text("Edit Subtotal") },
            text = {
                OutlinedTextField(
                    value = subtotalText,
                    onValueChange = { subtotalText = it },
                    singleLine = true,
                    label = { Text("Subtotal") },
                    prefix = { Text("€") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        subtotalText.toDoubleOrNull()?.let { newSubtotal ->
                            onSubtotalUpdate(newSubtotal)
                        }
                        isEditingSubtotal = false
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
    }
} 