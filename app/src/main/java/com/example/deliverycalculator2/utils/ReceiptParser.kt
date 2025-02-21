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
        var deliveryAddress = ""
        var subtotal = 0.0
        var foundSubtotal = false
        var isPaid = false
        var maskingCode = ""

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
                // Extract verification code
                line.contains("verification code", ignoreCase = true) && i + 1 < lines.size -> {
                    maskingCode = lines[i + 1].trim().replace(" ", "")
                }
            }
        }

        return ClipboardEntry(
            content = lines.joinToString("\n"),
            deliveryAddress = deliveryAddress,
            subtotal = subtotal,
            total = 0.0,
            isPaid = isPaid,
            phoneNumber = "014832993",
            maskingCode = maskingCode
        )
    }

    private fun parseJustEat2Receipt(lines: List<String>): ClipboardEntry? {
        var deliveryAddress = ""
        var subtotal = 0.0
        var isPaid = true  // Default to true, will be set to false if "Order NOT Paid" is found
        var customerDetailsIndex = -1
        var maskingCode = ""

        // Find sections and extract data
        for (i in lines.indices) {
            val line = lines[i]
            when {
                line.equals("Customer Details", ignoreCase = true) -> {
                    customerDetailsIndex = i
                }
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
                // Extract masking code
                line.contains("(masking", ignoreCase = true) -> {
                    // Check if masking code is in the same line
                    if (line.contains("code)", ignoreCase = true)) {
                        maskingCode = line.substringAfter("code)").trim()
                    } else if (i + 1 < lines.size) {
                        // Get masking code from next line if not in same line
                        val nextLine = lines[i + 1].trim()
                        if (nextLine.contains("code)", ignoreCase = true)) {
                            maskingCode = nextLine.substringAfter("code)").trim()
                        } else {
                            maskingCode = nextLine
                        }
                    }
                    maskingCode = maskingCode.replace(" ", "")
                }
            }
        }

        // Extract address after Customer Details
        if (customerDetailsIndex != -1) {
            val addressLines = mutableListOf<String>()
            var i = customerDetailsIndex + 1
            var foundMaskingCode = false
            
            while (i < lines.size && !lines[i].contains("Order NOT Paid")) {
                val line = lines[i].trim()
                
                // Skip until we find the masking code line
                if (!foundMaskingCode) {
                    if (line.contains("(masking")) {
                        foundMaskingCode = true
                        i++ // Skip the masking code line
                        continue
                    }
                }
                
                // After masking code, collect address lines
                if (foundMaskingCode) {
                    // Skip phone numbers and empty lines
                    if (!line.matches(Regex("\\d{9,}")) && 
                        !line.contains("code", ignoreCase = true) &&
                        line.isNotEmpty()) {
                        addressLines.add(line)
                    }
                }
                i++
            }
            
            deliveryAddress = addressLines.joinToString(", ")
        }

        // Debug logging
        println("Debug: JustEat2 Receipt Parsing")
        println("Debug: Customer Details Index: $customerDetailsIndex")
        println("Debug: Extracted address: $deliveryAddress")
        println("Debug: Phone Number: 014832993")
        println("Debug: Masking Code: $maskingCode")
        println("Debug: Subtotal: $subtotal")
        println("Debug: Is Paid: $isPaid")

        return ClipboardEntry(
            content = lines.joinToString("\n"),
            deliveryAddress = deliveryAddress,
            subtotal = subtotal,
            total = 0.0,
            isPaid = isPaid,
            phoneNumber = "014832993",
            maskingCode = maskingCode
        )
    }

    private fun parseShopReceipt(lines: List<String>): ClipboardEntry? {
        var deliveryAddress = ""
        var subtotal = 0.0
        var foundSubtotal = false
        var isPaid = false
        var dateLineIndex = -1
        var phoneLineIndex = -1
        var phoneNumber = ""

        // First find the date line and phone line
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.matches(Regex("\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4}.*"))) {
                dateLineIndex = i
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
                break
            }
        }

        // Extract address between date and phone
        if (dateLineIndex != -1 && phoneLineIndex != -1 && dateLineIndex < phoneLineIndex) {
            val addressLines = lines.slice((dateLineIndex + 1) until phoneLineIndex)
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("Ph:", ignoreCase = true) }
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
        println("Debug: Date line index: $dateLineIndex")
        println("Debug: Phone line index: $phoneLineIndex")
        println("Debug: Phone Number: $phoneNumber")
        println("Debug: Extracted address: $deliveryAddress")
        println("Debug: Subtotal: $subtotal")

        return ClipboardEntry(
            content = lines.joinToString("\n"),
            deliveryAddress = deliveryAddress,
            subtotal = subtotal,
            total = 0.0,
            isPaid = isPaid,
            phoneNumber = phoneNumber,
            maskingCode = ""  // Shop receipts don't have masking codes
        )
    }

    private fun parseOnlineReceipt(lines: List<String>): ClipboardEntry? {
        var deliveryAddress = ""
        var subtotal = 0.0
        var foundSubtotal = false
        var isPaid = false
        var addressLines = mutableListOf<String>()
        var collectingAddress = false
        var phoneNumber = ""

        for (i in lines.indices) {
            val line = lines[i]
            when {
                line.startsWith("Accepted:", ignoreCase = true) -> {
                    collectingAddress = true
                }
                collectingAddress && (line.startsWith("+") || line.matches(Regex("\\d{10,}"))) -> {
                    collectingAddress = false
                    deliveryAddress = addressLines.joinToString(", ")
                    // Extract phone number
                    phoneNumber = line.replace(" ", "")
                    // Only replace + with 00 if it's an international number
                    if (phoneNumber.startsWith("+")) {
                        phoneNumber = phoneNumber.replace("+", "00")
                    }
                }
                collectingAddress -> {
                    addressLines.add(line)
                }
                line.equals("Subtotal:", ignoreCase = true) && i + 1 < lines.size -> {
                    if (!foundSubtotal) {
                        val nextLine = lines[i + 1]
                        subtotal = nextLine.replace("€", "")
                            .replace(",", ".")
                            .trim()
                            .toDoubleOrNull() ?: 0.0
                        foundSubtotal = true
                    }
                }
                line.contains("payment", ignoreCase = true) && line.contains("paid", ignoreCase = true) -> {
                    isPaid = true
                }
            }
        }

        // Debug logging
        println("Debug: Online Receipt Parsing")
        println("Debug: Extracted address: $deliveryAddress")
        println("Debug: Phone Number: $phoneNumber")
        println("Debug: Subtotal: $subtotal")
        println("Debug: Is Paid: $isPaid")

        return ClipboardEntry(
            content = lines.joinToString("\n"),
            deliveryAddress = deliveryAddress,
            subtotal = subtotal,
            total = 0.0,
            isPaid = isPaid,
            phoneNumber = phoneNumber,
            maskingCode = ""  // Online receipts don't have masking codes
        )
    }

    private fun parseDeliverooReceipt(lines: List<String>): ClipboardEntry? {
        var deliveryAddress = ""
        var subtotal = 0.0
        var isPaid = false
        var phoneNumber = ""
        var accessCode = ""

        for (i in lines.indices) {
            val line = lines[i].trim()
            when {
                line.startsWith("Address:", ignoreCase = true) -> {
                    deliveryAddress = line.substringAfter("Address:").trim()
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

        // Debug logging
        println("Debug: Deliveroo Receipt Parsing")
        println("Debug: Extracted address: $deliveryAddress")
        println("Debug: Phone Number: $phoneNumber")
        println("Debug: Access Code: $accessCode")
        println("Debug: Subtotal: $subtotal")
        println("Debug: Is Paid: $isPaid")

        return ClipboardEntry(
            content = lines.joinToString("\n"),
            deliveryAddress = deliveryAddress,
            subtotal = subtotal,
            total = 0.0,
            isPaid = isPaid,
            phoneNumber = phoneNumber,
            maskingCode = accessCode
        )
    }
} 