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
                    }
                    line.startsWith("Paid Amount", ignoreCase = true) -> {
                        val amount = line.substringAfter("€")
                            .replace(",", ".")
                            .trim()
                            .toDoubleOrNull() ?: 0.0
                        isPaid = amount > 0
                    }
                    line.contains("Outstanding", ignoreCase = true) -> {
                        val amount = line.substringAfter("€")
                            .replace(",", ".")
                            .trim()
                            .toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            isPaid = false
                        }
                    }
                    // Look for line starting with "code)" or containing "(masking code)"
                    line.startsWith("code)", ignoreCase = true) && i + 1 < lines.size -> {
                        maskingCode = line.replace("code)", "").trim()
                        deliveryAddress = lines[i + 1].trim()
                    }
                    line.contains("(masking code)", ignoreCase = true) -> {
                        // For lines like "01 483 2993 (masking code) 517616386 Knockard Dundrum Road..."
                        val parts = line.split("(masking")
                        if (parts.size == 2) {
                            val afterMasking = parts[1].substringAfter("code)").trim()
                            maskingCode = afterMasking.split(" ").firstOrNull() ?: ""
                            deliveryAddress = afterMasking.substringAfter(maskingCode).trim()
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
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 