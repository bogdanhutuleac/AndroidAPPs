package com.example.deliverycalculator2.utils.parsers

import com.example.deliverycalculator2.data.ClipboardEntry

class JustEatParser : ReceiptParser {
    override fun canParse(lines: List<String>): Boolean {
        return lines.any { it.contains("JUST EAT", ignoreCase = true) }
    }

    override fun parse(lines: List<String>): ClipboardEntry? {
        return try {
            var deliveryAddress = ""
            var subtotal = 0.0
            var foundSubtotal = false
            var isPaid = false
            var maskingCode = ""
            
            for (i in lines.indices) {
                val line = lines[i]
                when {
                    // Find customer details line
                    line.equals("Customer details:", ignoreCase = true) -> {
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
                    // Extract verification code
                    line.contains("verification code", ignoreCase = true) && i + 1 < lines.size -> {
                        maskingCode = lines[i + 1].trim().replace(" ", "")
                    }
                }
            }

            // Debug logging
            println("Debug: Just Eat Receipt Parsing")
            println("Debug: Extracted address: $deliveryAddress")
            println("Debug: Phone Number: 014832993")
            println("Debug: Masking Code: $maskingCode")
            println("Debug: Subtotal: $subtotal")
            println("Debug: Is Paid: $isPaid")

            // Only create the entry if we found an address
            if (deliveryAddress.isNotEmpty()) {
                ClipboardEntry(
                    content = lines.joinToString("\n"),
                    deliveryAddress = deliveryAddress,
                    subtotal = subtotal,
                    total = 0.0,
                    isPaid = isPaid,
                    phoneNumber = "014832993",
                    maskingCode = maskingCode
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 