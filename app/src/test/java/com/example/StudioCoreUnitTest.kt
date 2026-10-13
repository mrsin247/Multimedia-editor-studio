package com.example

import com.example.model.AspectRatioType
import com.example.model.ColorAdjustments
import com.example.model.SampleType
import com.example.model.StudioFilter
import com.example.photo.CutoutProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudioCoreUnitTest {

    @Test
    fun testColorMatrixCreation_withoutCrashing() {
        val adjustments = ColorAdjustments(
            brightness = 15f,
            contrast = 1.2f,
            saturation = 1.4f,
            warmth = 10f
        )
        val filter = CutoutProcessor.createCombinedFilter(adjustments, StudioFilter.CYBERPUNK)
        assertNotNull(filter)
    }

    @Test
    fun testSampleTypes_haveValidFrequencies() {
        SampleType.values().forEach { sample ->
            assertTrue("Base frequency should be positive", sample.baseFreq > 0f)
            assertTrue("Display name should not be blank", sample.displayName.isNotBlank())
        }
    }

    @Test
    fun testAspectRatios_calculateProperly() {
        assertEquals(16f / 9f, AspectRatioType.LANDSCAPE.ratio, 0.01f)
        assertEquals(9f / 16f, AspectRatioType.PORTRAIT.ratio, 0.01f)
        assertEquals(1f, AspectRatioType.SQUARE.ratio, 0.01f)
    }
}
