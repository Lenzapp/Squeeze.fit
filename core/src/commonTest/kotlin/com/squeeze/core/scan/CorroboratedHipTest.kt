package com.squeeze.core.scan

import com.squeeze.core.model.Sex
import com.squeeze.core.model.Profile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CorroboratedHipTest {
    private val man = Profile(heightCm = 175.0, birthYear = 1993, sex = Sex.MALE)
    private fun hipReading(ratio: Double) = SilhouetteBodyFat.estimate(ShapeIndices(waistToShoulder = 0.80, waistToHip = ratio, hipCorroborated = true), Sex.MALE)

    @Test
    fun `the trousers scan now resolves from photo`() {
        val estimate = hipReading(0.788)
        assertNotNull(estimate)
        assertTrue(estimate.percent >= 12.0, "got ${estimate.percent}")
        assertTrue(estimate.percent >= 3.0)
    }

    @Test
    fun `the wide-stance scan resolves from photo too`() {
        val estimate = hipReading(0.69)
        assertNotNull(estimate)
        assertTrue(estimate.percent >= 3.0 && estimate.percent <= 11.6, "got ${estimate.percent}")
    }

    @Test
    fun `lean hip readings resolve continuously`() {
        listOf(0.788, 0.69).forEach { ratio ->
            val measured = hipReading(ratio)
            assertNotNull(measured, "ratio $ratio")
            assertTrue(measured.percent in 3.0..60.0, "ratio $ratio gave ${measured.percent}")
        }
    }

    @Test
    fun `corroboration cannot move any reading, at any ratio, for either sex`() {
        val ratios = listOf(0.55, 0.69, 0.72, 0.788, 0.80, 0.87, 0.95, 1.06, 1.20, 1.45)
        for (ratio in ratios) {
            Sex.entries.forEach { sex ->
                val checked = SilhouetteBodyFat.estimate(ShapeIndices(0.80, ratio, hipCorroborated = true), sex)
                val unchecked = SilhouetteBodyFat.estimate(ShapeIndices(0.80, ratio, hipCorroborated = false), sex)
                assertEquals(unchecked?.percent, checked?.percent, "$sex at $ratio")
                assertEquals(unchecked?.standardErrorPercent, checked?.standardErrorPercent, "$sex at $ratio")
            }
        }
    }

    @Test
    fun `a hip reading above the floor is still the reading, untouched`() {
        val estimate = hipReading(0.87)
        assertNotNull(estimate)
        assertTrue(estimate.percent > 13.0, "got ${estimate.percent}")
        assertEquals(com.squeeze.core.model.EstimationMethod.PHOTO_SHAPE.standardErrorPercent, estimate.standardErrorPercent, 1e-9)
    }
}
