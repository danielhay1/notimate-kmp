package com.hayduck.notemate.featurekeys

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

internal val featureDefaults = """[
    {"key":"notificationTestingEnabled","type":"boolean","default":false},
    {"key":"count","type":"int","default":2},
    {"key":"ratio","type":"float","default":1.5},
    {"key":"precision","type":"double","default":2.25},
    {"key":"label","type":"string","default":"Synthetic label"}
]"""

internal class MemoryFeatureKeyStore(var saved: String? = null) : FeatureKeyOverrideStore {
    var reads = 0
    var writes = 0
    var failRead = false
    var failWrite = false

    override suspend fun read(): String? {
        reads++
        if (failRead) error("Synthetic storage failure")
        return saved
    }

    override suspend fun write(overrides: String) {
        writes++
        if (failWrite) error("Synthetic storage failure")
        saved = overrides
    }
}

class FeatureKeyManagerTest {
    @Test
    fun releaseIgnoresDebugStorageAndRejectsEdits() = runTest {
        val store = MemoryFeatureKeyStore("""{
            "notificationTestingEnabled":{"type":"boolean","value":true}
        }""")
        val manager = FeatureKeyManager(isDebug = false, store)
        assertFalse(manager.isEnabled(FeatureKeys.NOTIFICATION_TESTING_ENABLED))
        manager.initialize(featureDefaults)
        assertFalse(manager.isEnabled(FeatureKeys.NOTIFICATION_TESTING_ENABLED))
        assertEquals(2, manager.intValue("count"))
        assertEquals(1.5f, manager.floatValue("ratio"))
        assertEquals(2.25, manager.doubleValue("precision"))
        assertEquals("Synthetic label", manager.stringValue("label"))
        assertFailsWith<IllegalStateException> { manager.applyEdits(emptyMap()) }
        assertEquals(0, store.reads)
        assertEquals(0, store.writes)
    }

    @Test
    fun typedEditsPersistAcrossManagersWithoutChangingDefaults() = runTest {
        val store = MemoryFeatureKeyStore()
        val manager = FeatureKeyManager(true, store)
        manager.initialize(featureDefaults)
        val edits = manager.keys.value.associate { it.key to it.value.text }.toMutableMap()
        edits[FeatureKeys.NOTIFICATION_TESTING_ENABLED] = "true"
        edits["count"] = "-7"
        edits["ratio"] = "-0.25"
        edits["precision"] = "1.234567890123"
        edits["label"] = "Synthetic \"label\"\nשורה"
        manager.applyEdits(edits)
        val reloaded = FeatureKeyManager(true, store)
        reloaded.initialize(featureDefaults)
        assertTrue(reloaded.isEnabled(FeatureKeys.NOTIFICATION_TESTING_ENABLED))
        assertEquals(-7, reloaded.intValue("count"))
        assertEquals(-0.25f, reloaded.floatValue("ratio"))
        assertEquals(1.234567890123, reloaded.doubleValue("precision"))
        assertEquals(edits["label"], reloaded.stringValue("label"))
        assertEquals(FeatureKeyValue.IntValue(2), reloaded.keys.value[1].defaultValue)
        assertFalse(reloaded.isEnabled("removedKey"))
        manager.applyEdits(manager.keys.value.associate { it.key to it.defaultValue.text })
        assertEquals("{}", store.saved)
    }

    @Test
    fun addedDefaultsAndChangedTypesDoNotReuseStaleOverrides() = runTest {
        val store = MemoryFeatureKeyStore("""{
            "notificationTestingEnabled":{"type":"boolean","value":true},
            "count":{"type":"string","value":"7"},
            "ratio":{"type":"float","value":1e100},
            "removed":{"type":"int","value":1}
        }""")
        val manager = FeatureKeyManager(true, store)
        manager.initialize(featureDefaults)
        assertTrue(manager.isEnabled(FeatureKeys.NOTIFICATION_TESTING_ENABLED))
        assertEquals(2, manager.intValue("count"))
        assertEquals(1.5f, manager.floatValue("ratio"))
        assertEquals(2.25, manager.doubleValue("precision"))
        assertTrue(manager.discardedOverrides)
    }

    @Test
    fun malformedOverridesFallBackButReadFailuresDoNotResetStorage() = runTest {
        val store = MemoryFeatureKeyStore("invalid-json")
        val manager = FeatureKeyManager(true, store)
        manager.initialize(featureDefaults)
        assertTrue(manager.discardedOverrides)
        assertEquals(0, store.writes)
        store.failRead = true
        val unreadable = FeatureKeyManager(true, store)
        assertFailsWith<IllegalStateException> { unreadable.initialize(featureDefaults) }
        assertFalse(unreadable.isInitialized)
        assertEquals("invalid-json", store.saved)
    }

    @Test
    fun invalidEditsAndFailedSavesLeavePublishedValuesUnchanged() = runTest {
        val store = MemoryFeatureKeyStore()
        val manager = FeatureKeyManager(true, store)
        manager.initialize(featureDefaults)
        val original = manager.keys.value
        val edits = original.associate { it.key to it.value.text }.toMutableMap()
        assertFailsWith<IllegalArgumentException> { manager.applyEdits(emptyMap()) }
        edits["count"] = "2147483648"
        assertFailsWith<IllegalArgumentException> { manager.applyEdits(edits) }
        assertEquals(0, store.writes)
        edits["count"] = "8"
        store.failWrite = true
        assertFailsWith<IllegalStateException> { manager.applyEdits(edits) }
        assertEquals(original, manager.keys.value)
        assertEquals(null, store.saved)
    }

    @Test
    fun invalidDefaultSchemaBlocksInitialization() = runTest {
        listOf(
            """[{"key":"duplicate","type":"int","default":1},
                {"key":"duplicate","type":"int","default":2}]""",
            """[{"key":"bad","type":"unknown","default":1}]""",
            """[{"key":"bad","type":"int","default":"1"}]""",
            """[{"key":"bad","type":"boolean","default":null}]""",
        ).forEach { defaults ->
            val manager = FeatureKeyManager(false)
            assertFails { manager.initialize(defaults) }
            assertFalse(manager.isInitialized)
        }
    }
}
