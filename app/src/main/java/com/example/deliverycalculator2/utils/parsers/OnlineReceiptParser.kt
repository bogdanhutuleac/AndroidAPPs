package com.example.deliverycalculator2.utils.parsers

import com.example.deliverycalculator2.data.ClipboardEntry

class OnlineReceiptParser : ReceiptParser {
    override fun canParse(lines: List<String>): Boolean {
        // This is a fallback parser, it will try to parse any receipt that other parsers couldn't handle
        return true
    }

    override fun parse(lines: List<String>): ClipboardEntry? {
        return try {
            var deliveryAddress = ""
            var subtotal = 0.0
            var foundSubtotal = false
            var isPaid = false
            val addressLines = mutableListOf<String>()
            var collectingAddress = false
            var phoneNumber = ""
            var foundAddressMarker = false

            for (i in lines.indices) {
                val line = lines[i].trim()
                when {
                    // Start collecting address after "Accepted:" or "Placed:" line
                    (line.startsWith("Accepted:", ignoreCase = true) || 
                    (!foundAddressMarker && line.startsWith("Placed:", ignoreCase = true))) -> {
                        collectingAddress = true
                        foundAddressMarker = true
                        addressLines.clear() // Clear any previously collected lines
                        continue
                    }
                    // Stop collecting address when we find a phone number or certain markers
                    (collectingAddress && (
                        line.matches(Regex("\\d{7,}|\\+\\d{7,}")) || // Phone number
                        line.startsWith("1x", ignoreCase = true) || // Order items
                        line.startsWith("Subtotal:", ignoreCase = true) || // Payment section
                        line.contains("€") // Price
                    )) -> {
                        collectingAddress = false
                        if (line.matches(Regex("\\d{7,}|\\+\\d{7,}"))) {
                            phoneNumber = line.replace(" ", "").replace("+", "00")
                        }
                        // Join collected address lines, filtering out empty lines and the restaurant name
                        deliveryAddress = addressLines
                            .filter { it.isNotEmpty() && !it.equals("SAN MARINO", ignoreCase = true) }
                            .joinToString(", ")
                    }
                    // Collect address lines between "Accepted:/Placed:" and phone number/order items
                    collectingAddress && line.isNotEmpty() -> {
                        addressLines.add(line)
                    }
                    // Handle subtotal
                    line.equals("Subtotal:", ignoreCase = true) && i + 1 < lines.size -> {
                        if (!foundSubtotal) {
                            val nextLine = lines[i + 1].trim()
                            subtotal = nextLine.replace("€", "")
                                .replace(",", ".")
                                .trim()
                                .toDoubleOrNull() ?: 0.0
                            foundSubtotal = true
                        }
                    }
                    // Also check for subtotal in the same line
                    line.startsWith("Subtotal:", ignoreCase = true) && line.contains("€") -> {
                        if (!foundSubtotal) {
                            val subtotalValue = line.substringAfter(":")
                                .replace("€", "")
                                .replace(",", ".")
                                .trim()
                            subtotal = subtotalValue.toDoubleOrNull() ?: 0.0
                            foundSubtotal = true
                        }
                    }
                    // Handle payment status
                    line.contains("Payment:", ignoreCase = true) && line.contains("Paid", ignoreCase = true) -> {
                        isPaid = true
                    }
                }
            }

            // If we didn't find either "Accepted:" or "Placed:" line, try the original method
            if (!foundAddressMarker) {
                addressLines.clear()
                for (i in lines.indices) {
                    val line = lines[i]
                    if (line.startsWith("+") || line.matches(Regex("\\d{10,}"))) {
                        phoneNumber = line.replace(" ", "").replace("+", "00")
                        deliveryAddress = addressLines.joinToString(", ")
                        break
                    }
                    addressLines.add(line)
                }
            }

            // Debug logging
            println("Debug: Online Receipt Parsing")
            println("Debug: Found Address Marker: $foundAddressMarker")
            println("Debug: Extracted address: $deliveryAddress")
            println("Debug: Phone Number: $phoneNumber")
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
                    phoneNumber = phoneNumber,
                    maskingCode = ""  // Online receipts don't have masking codes
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