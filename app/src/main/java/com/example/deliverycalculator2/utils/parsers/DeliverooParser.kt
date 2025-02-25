package com.example.deliverycalculator2.utils.parsers

import com.example.deliverycalculator2.data.ClipboardEntry

class DeliverooParser : ReceiptParser {
    override fun canParse(lines: List<String>): Boolean {
        return lines.any { it.contains("deliveroo", ignoreCase = true) }
    }

    override fun parse(lines: List<String>): ClipboardEntry? {
        return try {
            var deliveryAddress = ""
            var subtotal = 0.0
            var isPaid = false
            var phoneNumber = ""
            var accessCode = ""
            var collectingAddress = false
            val addressLines = mutableListOf<String>()

            for (i in lines.indices) {
                val line = lines[i].trim()
                when {
                    line.startsWith("Address:", ignoreCase = true) -> {
                        collectingAddress = true
                        // Get the address from this line if it contains it
                        val addressPart = line.substringAfter("Address:").trim()
                        if (addressPart.isNotEmpty()) {
                            addressLines.add(addressPart)
                        }
                    }
                    collectingAddress && !line.startsWith("Customer:", ignoreCase = true) && 
                    !line.startsWith("Phone:", ignoreCase = true) && line.isNotEmpty() -> {
                        addressLines.add(line)
                    }
                    (line.startsWith("Customer:", ignoreCase = true) || 
                    line.startsWith("Phone:", ignoreCase = true)) && collectingAddress -> {
                        collectingAddress = false
                        deliveryAddress = addressLines.joinToString(", ").replace(";", ",")
                    }
                    line.startsWith("Subtotal", ignoreCase = true) -> {
                        if (line.contains("€")) {
                            // Amount is on the same line
                            subtotal = line.substringAfter("€")
                                .replace(",", ".")
                                .trim()
                                .toDoubleOrNull() ?: 0.0
                        } else if (i + 1 < lines.size) {
                            // Amount is on the next line
                            val nextLine = lines[i + 1]
                            subtotal = nextLine.replace("€", "")
                                .replace(",", ".")
                                .trim()
                                .toDoubleOrNull() ?: 0.0
                        }
                    }
                    line.contains("ORDER PAID", ignoreCase = true) -> {
                        isPaid = true
                    }
                    line.startsWith("Phone", ignoreCase = true) -> {
                        phoneNumber = line.substringAfter("+").trim()
                            .replace(" ", "")
                        if (phoneNumber.startsWith("353")) {
                            phoneNumber = "0" + phoneNumber.substring(3)
                        }
                    }
                    line.startsWith("Access code:", ignoreCase = true) -> {
                        accessCode = line.substringAfter("Access code:").trim()
                            .replace("-", "")
                            .replace(" ", "")
                    }
                }
            }

            // If we're still collecting address at the end, finalize it
            if (collectingAddress && addressLines.isNotEmpty()) {
                deliveryAddress = addressLines.joinToString(", ").replace(";", ",")
            }

            // Debug logging
            println("Debug: Deliveroo Receipt Parsing")
            println("Debug: Extracted address: $deliveryAddress")
            println("Debug: Phone Number: $phoneNumber")
            println("Debug: Access Code: $accessCode")
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
                    maskingCode = accessCode
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