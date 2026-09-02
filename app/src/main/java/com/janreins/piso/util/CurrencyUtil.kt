package com.janreins.piso.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utility functions for formatting and parsing Philippine Peso (₱) amounts.
 */
object CurrencyUtil {

    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    private val pesoFormatter = DecimalFormat("#,##0.00", symbols)

    /**
     * Formats a number as Philippine Peso, e.g. ₱12,500.00 or -₱500.00
     */
    fun formatPeso(amount: Double): String {
        return if (amount < 0) {
            "-₱" + pesoFormatter.format(kotlin.math.abs(amount))
        } else {
            "₱" + pesoFormatter.format(amount)
        }
    }

    /**
     * Safely parses a string input into a positive Double amount.
     * Returns null if invalid or <= 0.
     */
    fun parsePositiveAmount(text: String): Double? {
        val clean = text.trim().replace(",", "").replace("₱", "")
        val value = clean.toDoubleOrNull() ?: return null
        return if (value > 0) value else null
    }

    /**
     * Formats double with 2 decimal places for text field editing.
     */
    fun formatInputAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", amount)
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }
}
