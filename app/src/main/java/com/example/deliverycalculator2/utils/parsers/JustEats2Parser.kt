package com.example.deliverycalculator2.utils.parsers

import com.example.deliverycalculator2.data.ClipboardEntry

class JustEats2Parser : ReceiptParser {
    override fun canParse(lines: List<String>): Boolean {
        return lines.any { it.contains("JustEats", ignoreCase = true) }
    }

    override fun parse(lines: List<String>): ClipboardEntry? {
        return try {
            var deliveryAddress = ""
            var subtotal = 0.0
            var isPaid = false  // Default to false, will be set to true if payment is confirmed
            var maskingCode = ""

            // Debug: Print all lines for inspection
            println("Debug: JustEats2Parser - Processing receipt with ${lines.size} lines")
            lines.forEachIndexed { index, line ->
                println("Debug: Line $index: $line")
            }

            // Find sections and extract data
            for (i in lines.indices) {
                val line = lines[i].trim()
                when {
                    line.equals("Order Price", ignoreCase = true) && i + 1 < lines.size -> {
                        // Extract subtotal from Order Price
                        val nextLine = lines[i + 1]
                        subtotal = nextLine.replace("€", "")
                            .replace(",", ".")
                            .trim()
                            .toDoubleOrNull() ?: 0.0
                        println("Debug: Found Order Price, subtotal = $subtotal")
                    }
                    line.startsWith("Paid Amount", ignoreCase = true) -> {
                        val amount = line.substringAfter("€")
                            .replace(",", ".")
                            .trim()
                            .toDoubleOrNull() ?: 0.0
                        isPaid = amount > 0
                        println("Debug: Found Paid Amount: $amount, isPaid set to $isPaid")
                    }
                    line.contains("Outstanding", ignoreCase = true) -> {
                        val amount = line.substringAfter("€")
                            .replace(",", ".")
                            .trim()
                            .toDoubleOrNull() ?: 0.0
                        println("Debug: Found Outstanding: $amount")
                        if (amount > 0) {
                            isPaid = false
                            println("Debug: Outstanding amount > 0, isPaid set to false")
                        } else {
                            // If Outstanding is 0.00, consider it paid
                            isPaid = true
                            println("Debug: Outstanding amount is 0, isPaid set to true")
                        }
                    }
                    // Check for "Order Paid" text with more flexibility
                    line.contains("Order Paid", ignoreCase = true) || 
                    line.contains("Order paid", ignoreCase = true) || 
                    line.equals("Paid", ignoreCase = true) -> {
                        isPaid = true
                        println("Debug: Found payment confirmation text: '$line', isPaid set to true")
                    }
                    // Look for line starting with "code)" or containing "(masking code)"
                    line.startsWith("code)", ignoreCase = true) && i + 1 < lines.size -> {
                        maskingCode = line.replace("code)", "").trim()
                        deliveryAddress = lines[i + 1].trim()
                        println("Debug: Found masking code: $maskingCode")
                    }
                    line.contains("(masking code)", ignoreCase = true) -> {
                        // For lines like "01 483 2993 (masking code) 517616386 Knockard Dundrum Road..."
                        val parts = line.split("(masking")
                        if (parts.size == 2) {
                            val afterMasking = parts[1].substringAfter("code)").trim()
                            maskingCode = afterMasking.split(" ").firstOrNull() ?: ""
                            deliveryAddress = afterMasking.substringAfter(maskingCode).trim()
                            println("Debug: Found masking code in line: $maskingCode")
                        }
                    }
                }
            }

            // Debug logging
            println("Debug: JustEat2 Receipt Parsing")
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
                println("Debug: No delivery address found, returning null")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Debug: Exception in JustEats2Parser: ${e.message}")
            null
        }
    }
} 