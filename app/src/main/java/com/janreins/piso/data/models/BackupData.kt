package com.janreins.piso.data.models

import org.json.JSONArray
import org.json.JSONObject

/**
 * Helper to export and import all Piso room data to/from JSON for private backup and restore.
 */
data class BackupData(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val budgets: List<Budget>,
    val goals: List<Goal>,
    val debts: List<Debt>,
    val investments: List<Investment>
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Piso")
        root.put("exportedAt", System.currentTimeMillis())

        val accArr = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("kind", acc.kind)
            obj.put("balance", acc.balance)
            obj.put("notes", acc.notes)
            accArr.put(obj)
        }
        root.put("accounts", accArr)

        val txArr = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("dateMillis", tx.dateMillis)
            obj.put("type", tx.type)
            obj.put("category", tx.category)
            obj.put("amount", tx.amount)
            obj.put("note", tx.note)
            if (tx.accountId != null) obj.put("accountId", tx.accountId)
            if (tx.transferToId != null) obj.put("transferToId", tx.transferToId)
            if (tx.goalId != null) obj.put("goalId", tx.goalId)
            if (tx.goalFlow != null) obj.put("goalFlow", tx.goalFlow)
            if (tx.debtId != null) obj.put("debtId", tx.debtId)
            txArr.put(obj)
        }
        root.put("transactions", txArr)

        val bgtArr = JSONArray()
        budgets.forEach { bgt ->
            val obj = JSONObject()
            obj.put("id", bgt.id)
            obj.put("category", bgt.category)
            obj.put("limitAmount", bgt.limitAmount)
            obj.put("monthKey", bgt.monthKey)
            bgtArr.put(obj)
        }
        root.put("budgets", bgtArr)

        val goalArr = JSONArray()
        goals.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("targetAmount", g.targetAmount)
            obj.put("currentAmount", g.currentAmount)
            obj.put("isCompleted", g.isCompleted)
            if (g.deadlineMillis != null) obj.put("deadlineMillis", g.deadlineMillis)
            if (g.accountId != null) obj.put("accountId", g.accountId)
            goalArr.put(obj)
        }
        root.put("goals", goalArr)

        val debtArr = JSONArray()
        debts.forEach { d ->
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("name", d.name)
            obj.put("kind", d.kind)
            obj.put("originalAmount", d.originalAmount)
            obj.put("remainingAmount", d.remainingAmount)
            obj.put("notes", d.notes)
            if (d.dueMillis != null) obj.put("dueMillis", d.dueMillis)
            debtArr.put(obj)
        }
        root.put("debts", debtArr)

        val invArr = JSONArray()
        investments.forEach { inv ->
            val obj = JSONObject()
            obj.put("id", inv.id)
            obj.put("name", inv.name)
            obj.put("kind", inv.kind)
            obj.put("currentValue", inv.currentValue)
            obj.put("notes", inv.notes)
            if (inv.quantity != null) obj.put("quantity", inv.quantity)
            invArr.put(obj)
        }
        root.put("investments", invArr)

        return root.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): BackupData? {
            return try {
                val root = JSONObject(jsonStr)

                val accounts = mutableListOf<Account>()
                val accArr = root.optJSONArray("accounts") ?: JSONArray()
                for (i in 0 until accArr.length()) {
                    val obj = accArr.getJSONObject(i)
                    accounts.add(
                        Account(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            kind = obj.optString("kind", "Cash"),
                            balance = obj.optDouble("balance", 0.0),
                            notes = obj.optString("notes", "")
                        )
                    )
                }

                val transactions = mutableListOf<Transaction>()
                val txArr = root.optJSONArray("transactions") ?: JSONArray()
                for (i in 0 until txArr.length()) {
                    val obj = txArr.getJSONObject(i)
                    transactions.add(
                        Transaction(
                            id = obj.optLong("id", 0L),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            type = obj.getString("type"),
                            category = obj.optString("category", ""),
                            amount = obj.getDouble("amount"),
                            note = obj.optString("note", ""),
                            accountId = if (obj.has("accountId")) obj.getLong("accountId") else null,
                            transferToId = if (obj.has("transferToId")) obj.getLong("transferToId") else null,
                            goalId = if (obj.has("goalId")) obj.getLong("goalId") else null,
                            goalFlow = if (obj.has("goalFlow")) obj.getString("goalFlow") else null,
                            debtId = if (obj.has("debtId")) obj.getLong("debtId") else null
                        )
                    )
                }

                val budgets = mutableListOf<Budget>()
                val bgtArr = root.optJSONArray("budgets") ?: JSONArray()
                for (i in 0 until bgtArr.length()) {
                    val obj = bgtArr.getJSONObject(i)
                    budgets.add(
                        Budget(
                            id = obj.optLong("id", 0L),
                            category = obj.getString("category"),
                            limitAmount = obj.getDouble("limitAmount"),
                            monthKey = obj.getString("monthKey")
                        )
                    )
                }

                val goals = mutableListOf<Goal>()
                val goalArr = root.optJSONArray("goals") ?: JSONArray()
                for (i in 0 until goalArr.length()) {
                    val obj = goalArr.getJSONObject(i)
                    goals.add(
                        Goal(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            targetAmount = obj.getDouble("targetAmount"),
                            currentAmount = obj.optDouble("currentAmount", 0.0),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            deadlineMillis = if (obj.has("deadlineMillis")) obj.getLong("deadlineMillis") else null,
                            accountId = if (obj.has("accountId")) obj.getLong("accountId") else null
                        )
                    )
                }

                val debts = mutableListOf<Debt>()
                val debtArr = root.optJSONArray("debts") ?: JSONArray()
                for (i in 0 until debtArr.length()) {
                    val obj = debtArr.getJSONObject(i)
                    debts.add(
                        Debt(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            kind = obj.optString("kind", "Other"),
                            originalAmount = obj.getDouble("originalAmount"),
                            remainingAmount = obj.optDouble("remainingAmount", 0.0),
                            notes = obj.optString("notes", ""),
                            dueMillis = if (obj.has("dueMillis")) obj.getLong("dueMillis") else null
                        )
                    )
                }

                val investments = mutableListOf<Investment>()
                val invArr = root.optJSONArray("investments") ?: JSONArray()
                for (i in 0 until invArr.length()) {
                    val obj = invArr.getJSONObject(i)
                    investments.add(
                        Investment(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            kind = obj.optString("kind", "Other"),
                            currentValue = obj.getDouble("currentValue"),
                            notes = obj.optString("notes", ""),
                            quantity = if (obj.has("quantity")) obj.getString("quantity") else null
                        )
                    )
                }

                BackupData(accounts, transactions, budgets, goals, debts, investments)
            } catch (_: Exception) {
                null
            }
        }
    }
}
