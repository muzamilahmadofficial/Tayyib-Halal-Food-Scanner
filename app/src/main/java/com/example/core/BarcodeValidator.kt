package com.example.core

object BarcodeValidator {

    private val numericRegex = Regex("^[0-9]{8,14}$")
    private val alphanumericCodeRegex = Regex("^[A-Za-z0-9\\-._/]{4,32}$")
    private val gtinInUrlRegex = Regex("(?:gtin|barcode|product|code)[=/]([0-9]{8,14})", RegexOption.IGNORE_CASE)

    /**
     * Normalizes a scanned or manually entered barcode value.
     * Supports EAN-13, EAN-8, UPC-A, UPC-E, GTIN-14, Code 128, Code 39,
     * and QR codes embedding a GTIN/barcode URL parameter.
     */
    fun normalizeAndValidate(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // Direct numeric food barcode (EAN-8, UPC-E, UPC-A, EAN-13, GTIN-14)
        val digitsOnly = trimmed.replace(" ", "").replace("-", "")
        if (numericRegex.matches(digitsOnly)) {
            return digitsOnly
        }

        // QR code containing a product barcode URL
        val urlMatch = gtinInUrlRegex.find(trimmed)
        if (urlMatch != null) {
            return urlMatch.groupValues[1]
        }

        // Code 128 / Code 39 alphanumeric code
        if (!trimmed.startsWith("http", ignoreCase = true) && alphanumericCodeRegex.matches(trimmed)) {
            return trimmed
        }

        return null
    }

    /**
     * Strict check for manual numeric entry field (8 to 14 digits).
     */
    fun isValidManualBarcode(raw: String): Boolean {
        val cleaned = raw.trim().replace(" ", "").replace("-", "")
        return numericRegex.matches(cleaned)
    }

    /**
     * Validates standard EAN-13 / EAN-8 / UPC-A check digit when applicable.
     */
    fun hasValidGtinCheckDigit(digits: String): Boolean {
        if (digits.length !in listOf(8, 12, 13, 14) || !digits.all { it.isDigit() }) return false
        val body = digits.dropLast(1)
        val checkDigit = digits.last().digitToInt()
        var sum = 0
        body.reversed().forEachIndexed { index, ch ->
            val weight = if (index % 2 == 0) 3 else 1
            sum += ch.digitToInt() * weight
        }
        val expected = (10 - (sum % 10)) % 10
        return expected == checkDigit
    }
}
