package com.hayduck.notemate.inference

import com.hayduck.notemate.domain.extraction.DeviceResourceState
import com.hayduck.notemate.domain.extraction.ExtractionDeviceConditions
import com.hayduck.notemate.domain.extraction.LocalInferenceRequest
import com.hayduck.notemate.domain.extraction.LocalInferenceResult
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class LiteRtLocalTextInferenceTest {
    @Test
    fun eachCompletedCallOwnsFreshSessionAndRedactsResult() = runTest {
        fixture { fixture ->
            val first = async { fixture.runtime.generate(request()) }
            runCurrent()
            fixture.sessions.last().emit("{\"synthetic\":true}")
            fixture.sessions.last().finish(true)
            runCurrent()
            val result = assertIs<LocalInferenceResult.Completed>(first.await())
            assertEquals("{\"synthetic\":true}", result.output)
            assertEquals("Completed([REDACTED])", result.toString())
            assertTrue(fixture.sessions.last().closed)
            val second = async { fixture.runtime.generate(request()) }
            runCurrent()
            assertEquals(2, fixture.sessions.size)
            fixture.sessions.last().emit("{}")
            fixture.sessions.last().finish(true)
            second.await()
        }
    }

    @Test
    fun busyRejectsWithoutOpeningOrQueueingAnotherSession() = runTest {
        fixture { fixture ->
            val first = async { fixture.runtime.generate(request()) }
            runCurrent()
            assertEquals(LocalInferenceResult.Busy, fixture.runtime.generate(request()))
            assertEquals(1, fixture.sessions.size)
            fixture.sessions.single().emit("{}")
            fixture.sessions.single().finish(true)
            first.await()
        }
    }

    @Test
    fun overflowCancelsAndWaitsForAcknowledgementBeforeClose() = runTest {
        fixture { fixture ->
            val result = async { fixture.runtime.generate(request()) }
            runCurrent()
            val session = fixture.sessions.single()
            session.emit("x".repeat(33))
            assertTrue(session.cancelled)
            assertFalse(session.closed)
            session.finish(false)
            assertEquals(LocalInferenceResult.Failed, result.await())
            assertTrue(session.closed)
        }
    }

    @Test
    fun timeoutWaitsForAcknowledgementAndReturnsTypedOutcome() = runTest {
        fixture { fixture ->
            val result = async { fixture.runtime.generate(request()) }
            runCurrent()
            val session = fixture.sessions.single()
            advanceTimeBy(1001)
            runCurrent()
            assertTrue(session.cancelled)
            assertFalse(session.closed)
            assertEquals(LocalInferenceResult.Busy, fixture.runtime.generate(request()))
            session.finish(false)
            assertEquals(LocalInferenceResult.TimedOut, result.await())
            assertTrue(session.closed)
        }
    }

    @Test
    fun callerCancellationPropagatesAfterSafeCleanup() = runTest {
        fixture { fixture ->
            val result = async { fixture.runtime.generate(request()) }
            runCurrent()
            val session = fixture.sessions.single()
            result.cancel()
            runCurrent()
            assertTrue(session.cancelled)
            assertFalse(session.closed)
            session.finish(false)
            result.cancelAndJoin()
            assertTrue(result.isCancelled)
            assertTrue(session.closed)
        }
    }

    @Test
    fun invalidChunksAndNativeFailureReturnSanitizedFailure() = runTest {
        fixture { fixture ->
            for (invalidChunk in listOf<String?>(null, "")) {
                val result = async { fixture.runtime.generate(request()) }
                runCurrent()
                fixture.sessions.last().emit(invalidChunk)
                fixture.sessions.last().finish(false)
                assertEquals(LocalInferenceResult.Failed, result.await())
            }
        }
    }

    @Test
    fun unsupportedAbiAndUnknownResourcesDoNotOpenSession() = runTest {
        for ((abi, state) in listOf(false to DeviceResourceState.AVAILABLE,
            true to DeviceResourceState.UNKNOWN)) {
            fixture(abi, state) { fixture ->
                assertEquals(LocalInferenceResult.Unavailable, fixture.runtime.generate(request()))
                assertTrue(fixture.sessions.isEmpty())
            }
        }
    }

    @Test
    fun initializationExceptionsNeverExposeInputAndReleaseAdmission() = runTest {
        fixture(openFailure = IllegalStateException("synthetic private payload")) { fixture ->
            assertEquals(LocalInferenceResult.Failed, fixture.runtime.generate(request()))
            assertEquals(LocalInferenceResult.Failed, fixture.runtime.generate(request()))
            assertTrue(fixture.sessions.isEmpty())
        }
    }

    @Test
    fun missingNativeLibraryReturnsUnavailable() = runTest {
        fixture(openFailure = UnsatisfiedLinkError("synthetic native diagnostic")) { fixture ->
            assertEquals(LocalInferenceResult.Unavailable, fixture.runtime.generate(request()))
            assertTrue(fixture.sessions.isEmpty())
        }
    }

    @Test
    fun synchronousStartFailureCancelsBeforeNativeCleanup() = runTest {
        fixture(startFailure = true) { fixture ->
            assertEquals(LocalInferenceResult.Failed, fixture.runtime.generate(request()))
            val session = fixture.sessions.single()
            assertTrue(session.cancelled)
            assertTrue(session.closed)
            assertTrue(session.cancelledBeforeClose)
        }
    }

    private suspend fun TestScope.fixture(
        supportsAbi: Boolean = true,
        memory: DeviceResourceState = DeviceResourceState.AVAILABLE,
        openFailure: Throwable? = null,
        startFailure: Boolean = false,
        block: suspend TestScope.(Fixture) -> Unit,
    ) {
        val directory = Files.createTempDirectory("synthetic-inference-test").toFile()
        try {
            val file = directory.resolve("synthetic.litertlm").also {
                it.writeText("synthetic model")
            }
            val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                .joinToString("") { "%02x".format(it) }
            val sessions = mutableListOf<FakeSession>()
            val runtime = LiteRtLocalTextInference(
                ProvisionedLocalModel(directory, file, digest),
                LocalInferenceBudgets(1024, 128, 32, 1, 1000, 1024),
                { ExtractionDeviceConditions(memory,
                    DeviceResourceState.AVAILABLE, DeviceResourceState.AVAILABLE) },
                { supportsAbi },
                NativeInferenceSessionFactory { _, _, _ ->
                    openFailure?.let { throw it }
                    FakeSession(startFailure).also(sessions::add)
                },
                StandardTestDispatcher(testScheduler),
            )
            block(Fixture(runtime, sessions))
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun request() = LocalInferenceRequest("Synthetic instructions", "Synthetic input", "{}")

    private class Fixture(val runtime: LiteRtLocalTextInference, val sessions: List<FakeSession>)

    private class FakeSession(private val startFailure: Boolean) : NativeInferenceSession {
        var cancelled = false
        var closed = false
        var cancelledBeforeClose = false
        private lateinit var chunk: (String?) -> Unit
        private lateinit var terminal: (Boolean) -> Unit

        override fun start(
            input: String,
            responseSchema: String,
            onChunk: (String?) -> Unit,
            onTerminal: (Boolean) -> Unit,
        ) {
            chunk = onChunk
            terminal = onTerminal
            if (startFailure) throw IllegalStateException("synthetic native start failure")
        }

        fun emit(value: String?) = chunk(value)
        fun finish(successful: Boolean) = terminal(successful)
        override fun cancel() { cancelled = true }
        override fun close() {
            cancelledBeforeClose = cancelled
            closed = true
        }
    }
}
