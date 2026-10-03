package com.hayduck.notemate.featurekeys

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalForeignApi::class)
internal class IosFeatureKeyOverrideStore : FeatureKeyOverrideStore {
    private val preferences = NSUserDefaults.standardUserDefaults

    override suspend fun read(): String? = preferences.stringForKey("debugFeatureKeyOverrides")

    override suspend fun write(overrides: String) {
        preferences.setObject(overrides, forKey = "debugFeatureKeyOverrides")
    }
}
