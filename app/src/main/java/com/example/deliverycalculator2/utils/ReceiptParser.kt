package com.example.deliverycalculator2.utils

import com.example.deliverycalculator2.data.ClipboardEntry
import com.example.deliverycalculator2.utils.parsers.ReceiptParserFactory

object ReceiptParser {
    fun parseReceipt(text: String): ClipboardEntry? {
        return ReceiptParserFactory.parseReceipt(text)
    }
} 