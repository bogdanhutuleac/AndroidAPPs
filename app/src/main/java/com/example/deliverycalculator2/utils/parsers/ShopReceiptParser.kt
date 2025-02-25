package com.example.deliverycalculator2.utils.parsers

import com.example.deliverycalculator2.data.ClipboardEntry

class ShopReceiptParser : ReceiptParser {
    override fun canParse(lines: List<String>): Boolean {
        return lines.any { it.contains("www.sanmarino.ie", ignoreCase = true) }
    }

    override fun parse(lines: List<String>): ClipboardEntry? {
        return try {
            var deliveryAddress = ""
            var subtotal = 0.0
            var foundSubtotal = false
            var isPaid = false
            var phoneLineIndex = -1
            var shopPhoneLineIndex = -1
            var phoneNumber = ""
            var shopAddress = ""

            // First find the shop phone line and customer phone line
            for (i in lines.indices) {
                val line = lines[i].trim()
                if (line.startsWith("Ph:", ignoreCase = true)) {
                    shopPhoneLineIndex = i
                } else if (line.startsWith("Phone:", ignoreCase = true)) {
                    phoneLineIndex = i
                    // Extract phone number after "Phone:"
                    val extractedNumber = line.substringAfter(":", "").trim().replace(" ", "")
                    // If number starts with 2, add 01 prefix
                    phoneNumber = if (extractedNumber.startsWith("2")) {
                        "01$extractedNumber"
                    } else {
                        extractedNumber
                    }
                }
            }

            // Extract shop address (lines before Ph:)
            if (shopPhoneLineIndex > 0) {
                val shopAddressLines = lines.slice(0 until shopPhoneLineIndex)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.equals("San Marino", ignoreCase = true) }
                shopAddress = shopAddressLines.joinToString(", ")
            }

            // Extract delivery address (lines between Ph: and Phone:)
            if (shopPhoneLineIndex != -1 && phoneLineIndex != -1 && shopPhoneLineIndex < phoneLineIndex) {
                val addressLines = lines.slice((shopPhoneLineIndex + 2) until phoneLineIndex) // Start from Ph: + 2 to skip the date line
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
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

            // Debug logging
            println("Debug: Shop Receipt Parsing")
            println("Debug: Shop Phone line index: $shopPhoneLineIndex")
            println("Debug: Customer Phone line index: $phoneLineIndex")
            println("Debug: Shop Address: $shopAddress")
            println("Debug: Phone Number: $phoneNumber")
            println("Debug: Delivery Address: $deliveryAddress")
            println("Debug: Subtotal: $subtotal")

            // Only create the entry if we found an address
            if (deliveryAddress.isNotEmpty()) {
                ClipboardEntry(
                    content = lines.joinToString("\n"),
                    deliveryAddress = deliveryAddress,
                    subtotal = subtotal,
                    total = 0.0,
                    isPaid = isPaid,
                    phoneNumber = phoneNumber,
                    maskingCode = ""  // Shop receipts don't have masking codes
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