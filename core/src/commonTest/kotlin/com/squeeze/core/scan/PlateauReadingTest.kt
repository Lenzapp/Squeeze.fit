package com.squeeze.core.scan

import com.squeeze.core.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlateauReadingTest {
    @Test
    fun `the plateau ceiling is where the outline used to stop`() {
        val male = SilhouetteBodyFat.plateauCeilingPercent(Sex.MALE)
        assertEquals(11.6, male, 0.05)
    }
    @Test
    fun `women plateau higher, because their lean anchor is higher`() {
        val female = SilhouetteBodyFat.plateauCeilingPercent(Sex.FEMALE)
        assertEquals(21.2, female, 0.05)
    }
    @Test
    fun `lean ratios now resolve continuously from photo`() {
        val ceiling = SilhouetteBodyFat.plateauCeilingPercent(Sex.MALE)
        listOf(0.58, 0.62, 0.70).forEach { ratio ->
            val estimate = SilhouetteBodyFat.estimate(ShapeIndices(ratio, null), Sex.MALE)
            assertNotNull(estimate, "ratio $ratio")
            assertTrue(estimate.percent < ceiling, "ratio $ratio gave ${estimate.percent}, should be below ceiling $ceiling now resolved")
            assertTrue(estimate.percent >= 3.0)
        }
    }
    @Test
    fun `a ratio off the plateau produces a figure above the ceiling`() {
        val ceiling = SilhouetteBodyFat.plateauCeilingPercent(Sex.MALE)
        val estimate = SilhouetteBodyFat.estimate(ShapeIndices(0.90, null), Sex.MALE)
        assertNotNull(estimate)
        assertTrue(estimate.percent > ceiling, "${estimate.percent} should clear $ceiling")
    }
    @Test
    fun `lean shoulder readings now carry photo interval`() {
        val estimate = SilhouetteBodyFat.estimate(ShapeIndices(0.62, null), Sex.MALE)
        assertNotNull(estimate)
        assertEquals(8.0, estimate.standardErrorPercent, 1e-9)
    }
    @Test
    fun `the real reference ratios all land below former plateau`() {
        val measured = listOf(0.586, 0.592, 0.580, 0.632, 0.681, 0.677, 0.679)
        assertTrue(measured.all { it < SilhouetteBodyFat.LEAN_PLATEAU_RATIO }, "a reference ratio cleared the plateau: $measured")
        measured.forEach { ratio ->
            val e = SilhouetteBodyFat.estimate(ShapeIndices(ratio, null), Sex.MALE)
            assertNotNull(e)
            assertTrue(e.percent >= 3.0)
        }
    }
}
