package com.example.deliverycalculator2.utils.parsers

import com.example.deliverycalculator2.data.ClipboardEntry

object ReceiptParserFactory {
    private val parsers = listOf(
        JustEatParser(),
        JustEats2Parser(),
        ShopReceiptParser(),
        DeliverooParser(),
        OnlineReceiptParser() // This should be last as it's the fallback parser
    )

    fun parseReceipt(text: String): ClipboardEntry? {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        // Try each parser in order until one succeeds
        for (parser in parsers) {
            if (parser.canParse(lines)) {
                try {
                    val result = parser.parse(lines)
                    if (result != null) {
                        // Debug logging
                        println("Successfully parsed receipt using ${parser.javaClass.simpleName}")
                        return result
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("Error parsing with ${parser.javaClass.simpleName}: ${e.message}")
                }
            }
        }
        
        return null
    }
} 