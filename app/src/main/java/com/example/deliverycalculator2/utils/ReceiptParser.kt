package com.example.deliverycalculator2.utils

import com.example.deliverycalculator2.data.ClipboardEntry

object ReceiptParser {
    fun parseReceipt(text: String): ClipboardEntry? {
        return try {
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
                isJustEat -> parseJustEatReceipt(lines)
                isShopReceipt -> parseShopReceipt(lines)
                else -> parseOnlineReceipt(lines)
            }?.also {
                // Debug logging
                println("Parsed Receipt Data:")
                println("Receipt Type: ${when {
                    isJustEat -> "Just Eat"
                    isShopReceipt -> "Shop Receipt"
                    else -> "Online Receipt"
                }}")
                println("Address: ${it.deliveryAddress}")
                println("Subtotal: ${it.subtotal}")
                println("Paid: ${if (it.isPaid) "Yes" else "No"}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseJustEatReceipt(lines: List<String>): ClipboardEntry? {
        var deliveryAddress = ""
        var subtotal = 0.0
        var foundSubtotal = false
        var isPaid = false

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

        return ClipboardEntry(
            content = lines.joinToString("\n"),
            deliveryAddress = deliveryAddress,
            subtotal = subtotal,
            total = 0.0,
            isPaid = isPaid
        )
    }

    private fun parseShopReceipt(lines: List<String>): ClipboardEntry? {
        var deliveryAddress = ""
        var subtotal = 0.0
        var foundSubtotal = false
        var isPaid = false

        // Find address
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

        return ClipboardEntry(
            content = lines.joinToString("\n"),
            deliveryAddress = deliveryAddress,
            subtotal = subtotal,
            total = 0.0,
            isPaid = isPaid
        )
    }

    private fun parseOnlineReceipt(lines: List<String>): ClipboardEntry? {
        var deliveryAddress = ""
        var subtotal = 0.0
        var foundSubtotal = false
        var isPaid = false

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

        return ClipboardEntry(
            content = lines.joinToString("\n"),
            deliveryAddress = deliveryAddress,
            subtotal = subtotal,
            total = 0.0,
            isPaid = isPaid
        )
    }
} 