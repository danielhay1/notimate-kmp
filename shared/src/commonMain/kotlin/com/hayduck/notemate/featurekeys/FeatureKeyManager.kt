package com.hayduck.notemate.featurekeys

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * App-owned configuration loaded before normal startup. Release instances never access overrides.
 * Pass the same instance to feature consumers; unknown/unloaded booleans fail closed.
 */
class FeatureKeyManager(
    val isDebug: Boolean,
    private val overrideStore: FeatureKeyOverrideStore? = null,
) {
    private val mutex = Mutex()
    private val mutableKeys = MutableStateFlow<List<FeatureKey>>(emptyList())
    val keys: StateFlow<List<FeatureKey>> = mutableKeys.asStateFlow()
    var isInitialized: Boolean = false
        private set
    var discardedOverrides: Boolean = false
        private set

    /** Loads defaults once; invalid defaults or unreadable storage leave the manager uninitialized. */
    suspend fun initialize(defaults: String) = mutex.withLock {
        if (isInitialized) return@withLock
        val definitions = FeatureKeyJson.defaults(defaults)
        val stored = if (isDebug) requireNotNull(overrideStore).read() else null
        val overrides = FeatureKeyJson.overrides(stored)
        var accepted = 0
        val loaded = definitions.map { feature ->
            val value = FeatureKeyJson.overrideValue(overrides?.get(feature.key), feature.type)
            if (value != null) accepted++
            feature.copy(value = value ?: feature.defaultValue)
        }
        discardedOverrides = stored != null && (overrides == null || accepted != overrides.size)
        mutableKeys.value = loaded
        isInitialized = true
    }

    /** Persists an entire validated debug edit before publishing it; defaults are never rewritten. */
    suspend fun applyEdits(edits: Map<String, String>) = mutex.withLock {
        check(isDebug && isInitialized)
        require(edits.keys == mutableKeys.value.map { it.key }.toSet())
        val updated = mutableKeys.value.map { feature ->
            feature.copy(value = requireNotNull(parseFeatureKeyValue(
                feature.type, edits.getValue(feature.key),
            )))
        }
        requireNotNull(overrideStore).write(FeatureKeyJson.encodeOverrides(updated))
        mutableKeys.value = updated
        discardedOverrides = false
    }

    fun isEnabled(key: String): Boolean = (value(key) as? FeatureKeyValue.BooleanValue)?.value == true
    fun intValue(key: String): Int = (requireValue(key) as FeatureKeyValue.IntValue).value
    fun floatValue(key: String): Float = (requireValue(key) as FeatureKeyValue.FloatValue).value
    fun doubleValue(key: String): Double = (requireValue(key) as FeatureKeyValue.DoubleValue).value
    fun stringValue(key: String): String = (requireValue(key) as FeatureKeyValue.StringValue).value

    private fun value(key: String): FeatureKeyValue? = keys.value.firstOrNull { it.key == key }?.value
    private fun requireValue(key: String): FeatureKeyValue = requireNotNull(value(key))
}
