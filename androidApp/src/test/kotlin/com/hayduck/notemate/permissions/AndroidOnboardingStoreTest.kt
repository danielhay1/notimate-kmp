package com.hayduck.notemate.permissions

import android.app.Application
import com.hayduck.notemate.today.OnboardingPreferences
import com.hayduck.notemate.today.OnboardingStep
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AndroidOnboardingStoreTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()

    @Test
    fun missingInstallationStartsAtWelcomeAndEachStepSurvivesRestart() = runTest {
        val store = AndroidOnboardingStore(context)
        assertEquals(OnboardingPreferences(), store.load())
        OnboardingStep.entries.forEach { step ->
            val expected = OnboardingPreferences(
                step, hasHadAccess = true, hasRequestedAlerts = true,
            )
            store.save(expected)
            assertEquals(expected, AndroidOnboardingStore(context).load())
        }
        assertTrue(File(context.noBackupFilesDir, "onboarding-state").isFile)
    }

    @Test
    fun malformedExistingDataIsRejectedWithoutReset() = runTest {
        val file = File(context.noBackupFilesDir, "onboarding-state")
        val invalidData = listOf(
            byteArrayOf(),
            byteArrayOf(2, 0, 0, 0),
            byteArrayOf(1, 5, 0, 0),
            byteArrayOf(1, 0, 2, 0),
            byteArrayOf(1, 0, 0),
            byteArrayOf(1, 0, 0, 0, 0),
        )
        invalidData.forEach { bytes ->
            file.writeBytes(bytes)
            assertFails { AndroidOnboardingStore(context).load() }
            assertFails { AndroidOnboardingStore(context).save(OnboardingPreferences()) }
            assertContentEquals(bytes, file.readBytes())
        }
    }
}
