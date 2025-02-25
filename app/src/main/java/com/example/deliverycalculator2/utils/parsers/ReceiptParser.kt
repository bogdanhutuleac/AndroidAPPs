package com.example.deliverycalculator2.utils.parsers

import com.example.deliverycalculator2.data.ClipboardEntry

interface ReceiptParser {
    fun canParse(lines: List<String>): Boolean
    fun parse(lines: List<String>): ClipboardEntry?
} 