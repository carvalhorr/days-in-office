package com.carvalhorr.daysInOffice.core.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ComplianceResultTest {

    private fun result(
        totalWorkingDays: Int,
        officeDays: Int,
        unknownDays: Int = 0,
        targetPercentage: Float = 0.5f
    ) = ComplianceResult(
        periodStart = LocalDate.of(2026, 1, 1),
        periodEnd = LocalDate.of(2026, 1, 31),
        totalWorkingDays = totalWorkingDays,
        officeDays = officeDays,
        remoteDays = 0,
        unknownDays = unknownDays,
        targetPercentage = targetPercentage
    )

    @Test
    fun `given 10 working days and 5 office days when targetPercentage=0_5 then isCompliant=true and daysNeededToComply=0`() {
        val r = result(totalWorkingDays = 10, officeDays = 5)
        assertTrue(r.isCompliant)
        assertEquals(0, r.daysNeededToComply)
    }

    @Test
    fun `given 10 working days and 4 office days when targetPercentage=0_5 then isCompliant=false and daysNeededToComply=1`() {
        val r = result(totalWorkingDays = 10, officeDays = 4)
        assertFalse(r.isCompliant)
        assertEquals(1, r.daysNeededToComply)
    }

    @Test
    fun `given 0 working days when any configuration then daysNeededToComply=0 and currentPercentage=0`() {
        val r = result(totalWorkingDays = 0, officeDays = 0)
        assertEquals(0, r.daysNeededToComply)
        assertEquals(0f, r.currentPercentage)
    }
}
