package com.hayduck.notemate.featurekeys

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureKeyStartupViewModelTest {
    @Test
    fun debugEditsStayDraftUntilContinueAndFailedSaveCanRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = MemoryFeatureKeyStore()
            val manager = FeatureKeyManager(true, store)
            val model = FeatureKeyStartupViewModel(manager, true) { featureDefaults }
            advanceUntilIdle()
            assertIs<FeatureKeyStartupUiState.Editing>(model.state.value)
            model.edit("count", "-")
            assertFalse(assertIs<FeatureKeyStartupUiState.Editing>(model.state.value).canContinue)
            model.continueToApp()
            assertEquals(0, store.writes)
            model.edit("count", "9")
            assertEquals(2, manager.intValue("count"))
            store.failWrite = true
            model.continueToApp()
            advanceUntilIdle()
            assertTrue(assertIs<FeatureKeyStartupUiState.Editing>(model.state.value).saveFailed)
            assertEquals(2, manager.intValue("count"))
            store.failWrite = false
            model.continueToApp()
            advanceUntilIdle()
            assertEquals(FeatureKeyStartupUiState.Ready, model.state.value)
            assertEquals(9, manager.intValue("count"))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun releaseSkipsEditorAndLoadFailureCanRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var fail = true
            val model = FeatureKeyStartupViewModel(FeatureKeyManager(false), true) {
                if (fail) error("Synthetic read failure")
                featureDefaults
            }
            advanceUntilIdle()
            assertEquals(FeatureKeyStartupUiState.LoadFailed, model.state.value)
            fail = false
            model.load()
            advanceUntilIdle()
            assertEquals(FeatureKeyStartupUiState.Ready, model.state.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
