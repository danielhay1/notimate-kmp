package com.hayduck.notemate.featurekeys

import android.content.Context
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidFeatureKeyOverrideStore(context: Context) : FeatureKeyOverrideStore {
    private val preferences = context.getSharedPreferences("debug_feature_keys", Context.MODE_PRIVATE)

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        preferences.getString("overrides", null)
    }

    override suspend fun write(overrides: String): Unit = withContext(Dispatchers.IO) {
        if (!preferences.edit().putString("overrides", overrides).commit()) {
            throw IOException("Feature overrides could not be saved")
        }
    }
}
