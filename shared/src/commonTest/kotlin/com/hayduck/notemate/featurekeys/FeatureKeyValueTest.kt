package com.hayduck.notemate.featurekeys

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeatureKeyValueTest {
    @Test
    fun numericValidationRejectsIncompleteOverflowAndNonFiniteValues() {
        listOf("", "-", "1.5", "2147483648").forEach {
            assertNull(parseFeatureKeyValue(FeatureKeyType.INT, it))
        }
        listOf("", "-", "NaN", "Infinity", "1e100").forEach {
            assertNull(parseFeatureKeyValue(FeatureKeyType.FLOAT, it))
        }
        listOf("", "-", "NaN", "-Infinity", "1e999").forEach {
            assertNull(parseFeatureKeyValue(FeatureKeyType.DOUBLE, it))
        }
        assertEquals(FeatureKeyValue.IntValue(-3), parseFeatureKeyValue(FeatureKeyType.INT, " -3 "))
        assertEquals(FeatureKeyValue.FloatValue(1.25f),
            parseFeatureKeyValue(FeatureKeyType.FLOAT, "1.25"))
        assertEquals(FeatureKeyValue.DoubleValue(1e-10),
            parseFeatureKeyValue(FeatureKeyType.DOUBLE, "1e-10"))
        assertEquals(FeatureKeyValue.StringValue(""),
            parseFeatureKeyValue(FeatureKeyType.STRING, ""))
        assertNull(parseFeatureKeyValue(FeatureKeyType.BOOLEAN, "yes"))
    }
}
