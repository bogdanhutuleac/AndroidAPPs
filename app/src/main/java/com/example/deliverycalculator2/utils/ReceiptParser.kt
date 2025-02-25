package com.example.deliverycalculator2.utils

import com.example.deliverycalculator2.data.ClipboardEntry

object ReceiptParser {
    fun parseReceipt(text: String): ClipboardEntry? {
        return try {
            val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            
            // Determine receipt type
            val isJustEat = lines.any { it.contains("JUST EAT", ignoreCase = true) }
            val isJustEat2 = lines.any { it.contains("JustEats", ignoreCase = true) }
            val isShopReceipt = lines.any { it.contains("www.sanmarino.ie", ignoreCase = true) }
            val isDeliveroo = lines.any { it.contains("deliveroo", ignoreCase = true) }
            
            when {
                isJustEat -> parseJustEatReceipt(lines)
                isJustEat2 -> parseJustEat2Receipt(lines)
                isShopReceipt -> parseShopReceipt(lines)
                isDeliveroo -> parseDeliverooReceipt(lines)
                else -> parseOnlineReceipt(lines)
            }?.also {
                // Debug logging
                println("Parsed Receipt Data:")
                println("Receipt Type: ${when {
                    isJustEat -> "Just Eat"
                    isJustEat2 -> "Just Eat 2"
                    isShopReceipt -> "Shop Receipt"
                    isDeliveroo -> "Deliveroo"
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

    private fun parseJustEat2Receipt(lines: List<String>): ClipboardEntry? {
        return try {
            var deliveryAddress = ""
            var subtotal = 0.0
            var isPaid = true  // Default to true, will be set to false if "Order NOT Paid" is found
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
                    line.contains("Order NOT Paid", ignoreCase = true) -> {
                        isPaid = false
                    }
                    // Look for line starting with "code)" or containing "(masking code)"
                    line.startsWith("code)", ignoreCase = true) && i + 1 < lines.size -> {
                        maskingCode = line.replace("code)", "").trim()
                        deliveryAddress = lines[i + 1].trim()
                    }
                    line.contains("(masking code)", ignoreCase = true) -> {
                        // For lines like "01 483 2993 (masking code) 517616386 Knockard Dundrum Road..."
                        val parts = line.split("(masking code)")
                        if (parts.size == 2) {
                            maskingCode = parts[1].trim().split(" ").firstOrNull() ?: ""
                            deliveryAddress = parts[1].trim().substringAfter(" ").trim()
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

    private fun parseShopReceipt(lines: List<String>): ClipboardEntry? {
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

    private fun parseOnlineReceipt(lines: List<String>): ClipboardEntry? {
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

    private fun parseDeliverooReceipt(lines: List<String>): ClipboardEntry? {
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