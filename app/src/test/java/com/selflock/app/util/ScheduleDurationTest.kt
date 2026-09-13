package com.selflock.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleDurationTest {
    @Test fun acceptsScheduleUpToTwelveHours() = assertTrue(ScheduleDuration.isAllowed(8, 0, 20, 0, true))
    @Test fun rejectsScheduleLongerThanTwelveHoursByDefault() = assertFalse(ScheduleDuration.isAllowed(8, 0, 20, 1, true))
    @Test fun acceptsLongScheduleWhenLimitIsDisabled() = assertTrue(ScheduleDuration.isAllowed(8, 0, 20, 1, false))
    @Test fun rejectsEqualStartAndEnd() = assertFalse(ScheduleDuration.isAllowed(8, 0, 8, 0, false))
}
