package com.janreins.piso

import com.janreins.piso.data.models.Account
import com.janreins.piso.data.models.BackupData
import com.janreins.piso.data.models.Investment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupDataTest {

    @Test
    fun fromJsonString_rejectsJsonThatIsNotAPisoBackup() {
        assertNull(BackupData.fromJsonString("{}"))
        assertNull(BackupData.fromJsonString("""{"name": "some other app config"}"""))
        assertNull(BackupData.fromJsonString("not json"))
    }

    @Test
    fun fromJsonString_acceptsExportedBackup() {
        val exported = BackupData(
            accounts = listOf(Account(id = 1, name = "Wallet", kind = "Cash", balance = 250.0)),
            transactions = emptyList(),
            budgets = emptyList(),
            goals = emptyList(),
            debts = emptyList(),
            investments = emptyList()
        ).toJsonString()

        val parsed = BackupData.fromJsonString(exported)
        assertNotNull(parsed)
        assertEquals("Wallet", parsed!!.accounts.single().name)
    }

    @Test
    fun investmentLastUpdatedSurvivesRoundTrip() {
        val exported = BackupData(
            accounts = emptyList(),
            transactions = emptyList(),
            budgets = emptyList(),
            goals = emptyList(),
            debts = emptyList(),
            investments = listOf(
                Investment(id = 1, name = "PSEi fund", kind = "Stocks", currentValue = 5000.0, lastUpdatedMillis = 1_700_000_000_000)
            )
        ).toJsonString()

        val parsed = BackupData.fromJsonString(exported)!!
        assertEquals(1_700_000_000_000, parsed.investments.single().lastUpdatedMillis)
    }

    @Test
    fun fromJsonString_acceptsEmptyPisoBackup() {
        assertNotNull(BackupData.fromJsonString("""{"appName": "Piso", "version": 2}"""))
    }
}
