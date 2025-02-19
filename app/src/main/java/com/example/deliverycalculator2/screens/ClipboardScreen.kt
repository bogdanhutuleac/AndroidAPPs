package com.example.deliverycalculator2.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
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

    LaunchedEffect(selectedDate) {
        database.clipboardDao().getEntriesForDate(
            startOfDay = selectedDate.toStartOfDay(),
            endOfDay = selectedDate.toEndOfDay()
        ).collectLatest { dateEntries ->
            entries.value = dateEntries
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val uri = Uri.parse("https://maps.google.com/maps?q=${Uri.encode(entry.deliveryAddress)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    ) {
                        Text(
                            text = entry.deliveryAddress,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Open in Maps",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
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
                            onCheckedChange = onPaidStatusUpdate
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