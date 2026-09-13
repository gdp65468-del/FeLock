package com.selflock.app.domain.usecase

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardPolicyTest {
    @Test fun immediateRewardIsAlwaysAvailable() = assertTrue(RewardPolicy.isAvailable(LocalTime.NOON, null, null))

    @Test fun scheduledRewardOnlyCountsInsideItsWindow() {
        assertTrue(RewardPolicy.isAvailable(LocalTime.of(7, 15), LocalTime.of(7, 0), LocalTime.of(7, 30)))
        assertFalse(RewardPolicy.isAvailable(LocalTime.of(7, 45), LocalTime.of(7, 0), LocalTime.of(7, 30)))
    }

    @Test fun rewardWindowCanCrossMidnight() {
        assertTrue(RewardPolicy.isAvailable(LocalTime.of(0, 30), LocalTime.of(23, 0), LocalTime.of(1, 0)))
    }

    @Test fun progressIsCappedAtRequiredTime() = assertEquals(60, RewardPolicy.creditedSeconds(360, 240, 420))

    @Test fun temporaryRewardOnlyReleasesSelectedAppsBeforeExpiry() {
        assertTrue(RewardPolicy.isReleased(2_000, 1_000, setOf("whatsapp"), "whatsapp"))
        assertFalse(RewardPolicy.isReleased(2_000, 1_000, setOf("whatsapp"), "youtube"))
        assertFalse(RewardPolicy.isReleased(2_000, 2_000, setOf("whatsapp"), "whatsapp"))
    }
}
