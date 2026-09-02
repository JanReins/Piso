package com.janreins.piso.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a financial account (e.g. Cash, Bank, Savings, E-wallet).
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val kind: String, // Cash, Bank, Savings, E-Wallet, Other
    val balance: Double,
    val notes: String = ""
)
