package com.janreins.piso.data.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Represents a debt (Credit Card, Personal, Family, Housing, Vehicle, etc.).
 */
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val kind: String, // Credit Card, Personal, Family, Housing, Vehicle, Other
    val originalAmount: Double,
    val remainingAmount: Double,
    val notes: String = "",
    val dueMillis: Long? = null
) {
    val isPaidOff: Boolean
        get() = remainingAmount <= 0.0001
}
