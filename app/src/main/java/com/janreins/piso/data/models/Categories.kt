package com.janreins.piso.data.models

/**
 * Default categories and types specified for the Piso personal money book.
 */
object Categories {

    val INCOME = listOf(
        "Salary",
        "Freelance",
        "Business",
        "Allowance",
        "Gift",
        "Interest",
        "Other"
    )

    val EXPENSE = listOf(
        "Food",
        "Transport",
        "Bills",
        "Shopping",
        "Health",
        "Education",
        "Entertainment",
        "Rent",
        "Savings",
        "Debt",
        "Other"
    )

    val ACCOUNT_KINDS = listOf(
        "Cash",
        "Bank",
        "Savings",
        "E-Wallet (GCash, Maya)",
        "Other"
    )

    val DEBT_KINDS = listOf(
        "Credit Card",
        "Personal",
        "Family",
        "Housing",
        "Vehicle",
        "Other"
    )

    val INVESTMENT_KINDS = listOf(
        "Stocks",
        "Mutual Funds / ETFs",
        "Gold",
        "Silver",
        "Bitcoin",
        "Other Crypto",
        "Other"
    )
}
