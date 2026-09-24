package com.janreins.piso

import com.janreins.piso.util.CurrencyUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyUtilTest {

    @Test
    fun parsePositiveAmount_rejectsNonFiniteValues() {
        assertNull(CurrencyUtil.parsePositiveAmount("Infinity"))
        assertNull(CurrencyUtil.parsePositiveAmount("NaN"))
        assertNull(CurrencyUtil.parsePositiveAmount("1e999"))
        assertEquals(1250.5, CurrencyUtil.parsePositiveAmount("₱1,250.50")!!, 0.0001)
    }

    @Test
    fun parseAmount_allowsZeroAndNegativeButNotGarbage() {
        assertEquals(0.0, CurrencyUtil.parseAmount("0")!!, 0.0)
        assertEquals(-500.0, CurrencyUtil.parseAmount("-500")!!, 0.0)
        assertNull(CurrencyUtil.parseAmount("abc"))
        assertNull(CurrencyUtil.parseAmount(""))
        assertNull(CurrencyUtil.parseAmount("-Infinity"))
    }

    @Test
    fun formatPeso_doesNotShowNegativeZero() {
        assertEquals("₱0.00", CurrencyUtil.formatPeso(-0.0000001))
        assertEquals("₱0.00", CurrencyUtil.formatPeso(-0.004))
        assertEquals("-₱0.01", CurrencyUtil.formatPeso(-0.01))
        assertEquals("-₱1,500.00", CurrencyUtil.formatPeso(-1500.0))
        assertEquals("₱12,500.00", CurrencyUtil.formatPeso(12500.0))
    }
}
