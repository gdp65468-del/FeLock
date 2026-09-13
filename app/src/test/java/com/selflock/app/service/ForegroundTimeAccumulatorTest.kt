package com.selflock.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundTimeAccumulatorTest {
    @Test
    fun countsEntireForegroundInterval() {
        val accumulator = ForegroundTimeAccumulator(0L)

        assertNull(accumulator.observe("progress.app", 0L, true))
        assertEquals(ForegroundInterval("progress.app", 180L), accumulator.observe("other.app", 180_000L, true))
    }

    @Test
    fun preservesSubsecondRemainderBetweenObservations() {
        val accumulator = ForegroundTimeAccumulator(0L)

        accumulator.observe("progress.app", 0L, true)
        assertNull(accumulator.observe("progress.app", 600L, true))
        assertEquals(ForegroundInterval("progress.app", 1L), accumulator.observe("progress.app", 1_200L, true))
    }

    @Test
    fun doesNotCountTimeWhileDeviceIsInactive() {
        val accumulator = ForegroundTimeAccumulator(0L)

        accumulator.observe("progress.app", 0L, true)
        assertNull(accumulator.observe(null, 60_000L, false))
    }

    @Test
    fun progressCanAccumulateAcrossSeparateSessions() {
        val accumulator = ForegroundTimeAccumulator(0L)

        accumulator.observe("progress.app", 0L, true)
        val firstSession = accumulator.observe("other.app", 60_000L, true)
        accumulator.observe("progress.app", 120_000L, true)
        val secondSession = accumulator.observe("other.app", 240_000L, true)

        assertEquals(180L, firstSession!!.seconds + secondSession!!.seconds)
    }
}
