package com.hayduck.notemate.inference

import com.hayduck.notemate.domain.extraction.DeviceResourceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidInferenceResourceProbeTest {
    private val policy = InferenceResourcePolicy(1024, 30, 1)

    @Test
    fun unknownSignalsDenyInference() {
        val result = policy.evaluate(InferenceResourceReadings(null, null, null, null))
        assertFalse(result.allowsHeavyWork)
        assertEquals(DeviceResourceState.UNKNOWN, result.memory)
        assertEquals(DeviceResourceState.UNKNOWN, result.battery)
        assertEquals(DeviceResourceState.UNKNOWN, result.thermal)
        assertEquals(
            DeviceResourceState.UNKNOWN,
            policy.evaluate(InferenceResourceReadings(2048, null, 60, 0)).memory,
        )
    }

    @Test
    fun thresholdsAndLowMemoryDenyUnsafeAdmission() {
        assertTrue(policy.evaluate(InferenceResourceReadings(1024, false, 30, 1)).allowsHeavyWork)
        for (readings in listOf(
            InferenceResourceReadings(1023, false, 30, 1),
            InferenceResourceReadings(1024, true, 30, 1),
            InferenceResourceReadings(1024, false, 29, 1),
            InferenceResourceReadings(1024, false, 30, 2),
        )) assertFalse(policy.evaluate(readings).allowsHeavyWork)
    }
}
