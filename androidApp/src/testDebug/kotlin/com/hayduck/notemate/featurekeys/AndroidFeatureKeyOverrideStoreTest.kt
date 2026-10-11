package com.hayduck.notemate.featurekeys

import android.app.Application
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AndroidFeatureKeyOverrideStoreTest {
    @Test
    fun savedOverrideIsLoadedByANewManagerAndCanBeReset() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val defaults = """[
            {"key":"notificationTestingEnabled","type":"boolean","default":false}
        ]"""
        val first = FeatureKeyManager(true, AndroidFeatureKeyOverrideStore(context))
        first.initialize(defaults)
        assertFalse(first.isEnabled(FeatureKeys.NOTIFICATION_TESTING_ENABLED))
        first.applyEdits(mapOf(FeatureKeys.NOTIFICATION_TESTING_ENABLED to "true"))
        val second = FeatureKeyManager(true, AndroidFeatureKeyOverrideStore(context))
        second.initialize(defaults)
        assertTrue(second.isEnabled(FeatureKeys.NOTIFICATION_TESTING_ENABLED))
        second.applyEdits(mapOf(FeatureKeys.NOTIFICATION_TESTING_ENABLED to "false"))
        assertEquals("{}", AndroidFeatureKeyOverrideStore(context).read())
    }
}
