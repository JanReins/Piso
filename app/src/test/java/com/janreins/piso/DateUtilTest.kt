package com.janreins.piso

import com.janreins.piso.util.DateUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class DateUtilTest {

    @Test
    fun shiftMonthKey_crossesYearBoundaries() {
        assertEquals("2026-01", DateUtil.shiftMonthKey("2025-12", 1))
        assertEquals("2025-12", DateUtil.shiftMonthKey("2026-01", -1))
        assertEquals("September 2026", DateUtil.getMonthDisplayName("2026-09"))
    }

    @Test
    fun pickerConversion_keepsCalendarDayAndTimeOfDay() {
        val zone = ZoneId.systemDefault()
        val original = LocalDateTime.of(2026, 3, 15, 21, 45).atZone(zone).toInstant().toEpochMilli()

        // Picker value for the same day is UTC midnight of March 15
        val pickerValue = DateUtil.toPickerUtcMillis(original)
        assertEquals(
            LocalDateTime.of(2026, 3, 15, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
            pickerValue
        )

        // Choosing March 2 keeps 21:45 local time
        val picked = LocalDateTime.of(2026, 3, 2, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val result = DateUtil.fromPickerUtcMillis(picked, keepTimeOf = original)
        assertEquals(LocalDateTime.of(2026, 3, 2, 21, 45), LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(result), zone))
    }
}
