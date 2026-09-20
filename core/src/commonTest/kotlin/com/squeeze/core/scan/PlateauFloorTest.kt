package com.squeeze.core.scan

import com.squeeze.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlateauFloorTest {

    private fun shoulderOnly(ratio: Double, sex: Sex = Sex.MALE) =
        SilhouetteBodyFat.estimate(ShapeIndices(waistToShoulder = ratio, waistToHip = null), sex)

    @Test
    fun `the scan that reported below essential now resolves from photo`() {
        val estimate = shoulderOnly(0.686)
        assertNotNull(estimate)
        // Continuous interpolation from photo — now resolves rather than hitting a floor.
        assertTrue(estimate.percent < SilhouetteBodyFat.plateauCeilingPercent(Sex.MALE), "got ${estimate.percent}")
        assertTrue(estimate.percent >= 3.0, "got ${estimate.percent}")
    }

    @Test
    fun `lean ratios resolve continuously and monotonically`() {
        val ratios = listOf(0.75, 0.70, 0.65, 0.60, 0.55)
        val percents = ratios.map { shoulderOnly(it)!!.percent }
        // Smaller ratio -> leaner, non-increasing (clamped at MIN produces equals)
        assertTrue(percents.zipWithNext().all { (a, b) -> b <= a + 1e-9 }, "$percents")
        // At least first two are strictly decreasing before clamp
        assertTrue(percents[0] > percents[1] && percents[1] > percents[2], "$percents")
        percents.forEach { assertTrue(it < SilhouetteBodyFat.plateauCeilingPercent(Sex.MALE) + 1e-9, "$it") }
    }

    @Test
    fun `lean shoulder readings carry photo error not plateau interval`() {
        val estimate = shoulderOnly(0.686)
        assertNotNull(estimate)
        assertEquals(8.0, estimate.standardErrorPercent, 1e-9)
    }

    @Test
    fun `above the plateau the mapping is unchanged`() {
        val percents = listOf(0.80, 0.88, 0.96, 1.02).map {
            val estimate = shoulderOnly(it)
            assertNotNull(estimate, "ratio $it")
            estimate.percent
        }
        assertTrue(percents.zipWithNext().all { (a, b) -> b > a }, "$percents")
        assertTrue(percents.first() > 3.0)
        assertEquals(35.0 - SilhouetteBodyFat.OBSERVED_OFFSET_PERCENT, percents.last(), 0.5)
    }

    @Test
    fun `women resolve continuously too`() {
        val estimate = shoulderOnly(0.60, Sex.FEMALE)
        assertNotNull(estimate)
        assertTrue(estimate.percent in 3.0..21.2, "got ${estimate.percent}")
        assertTrue(estimate.percent > shoulderOnly(0.60, Sex.MALE)!!.percent)
    }

    @Test
    fun `the hip path resolves continuously from photo`() {
        val estimate = SilhouetteBodyFat.estimate(ShapeIndices(waistToShoulder = 0.70, waistToHip = 0.78), Sex.MALE)
        assertNotNull(estimate)
        assertTrue(estimate.percent < SilhouetteBodyFat.leanestClaimable(Sex.MALE), "got ${estimate.percent}")
        assertTrue(estimate.percent >= 3.0)
    }

    @Test
    fun `no photograph produces unbounded value`() {
        val ratios = listOf(0.40, 0.55, 0.65, 0.70, 0.75, 0.80, 0.90, 1.00, 1.20)
        for (shoulder in ratios) {
            for (hip in ratios + listOf(null)) {
                Sex.entries.forEach { sex ->
                    val estimate = SilhouetteBodyFat.estimate(ShapeIndices(shoulder, hip), sex) ?: return@forEach
                    assertTrue(estimate.percent in 3.0..60.0, "shoulder=$shoulder hip=$hip $sex gave ${estimate.percent}")
                    assertTrue(estimate.standardErrorPercent in 4.0..9.0, "error ${estimate.standardErrorPercent}")
                }
            }
        }
    }
}
