package com.janreins.piso.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an investment asset (Stocks, Mutual Funds, Gold, Crypto, etc.).
 */
@Entity(tableName = "investments")
data class Investment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val kind: String, // Stocks, Mutual Funds / ETFs, Gold, Silver, Bitcoin, Other Crypto, Other
    val currentValue: Double,
    val notes: String = "",
    val quantity: String? = null,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)
