package com.hayduck.notemate.inference

import android.content.Context
import android.os.Build
import android.os.Process
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.ThinkingConfig
import com.hayduck.notemate.domain.extraction.ExtractionDeviceConditions
import com.hayduck.notemate.domain.extraction.LocalInferenceRequest
import com.hayduck.notemate.domain.extraction.LocalInferenceResult
import com.hayduck.notemate.domain.extraction.LocalTextInference
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * CPU inference over a separately provisioned immutable model in no-backup storage.
 *
 * Busy admission never queues requests. Calls own an engine and isolated conversation.
 * Native initialization blocks; cancellation and timeout finish after native initialization or
 * cancellation acknowledgement, which can exceed the configured deadline. Cleanup waits for JNI.
 * Model qualification, provisioning, integrity metadata, and limits belong to the caller.
 */
class LiteRtLocalTextInference internal constructor(
    private val model: ProvisionedLocalModel,
    private val budgets: LocalInferenceBudgets,
    private val resources: () -> ExtractionDeviceConditions,
    private val supportsAbi: () -> Boolean,
    private val sessions: NativeInferenceSessionFactory,
    private val dispatcher: CoroutineDispatcher,
) : LocalTextInference {
    constructor(
        context: Context,
        modelFile: File,
        expectedSha256: String,
        budgets: LocalInferenceBudgets,
        resourcePolicy: InferenceResourcePolicy,
    ) : this(
        ProvisionedLocalModel(
            context.applicationContext.noBackupFilesDir, modelFile, expectedSha256,
        ),
        budgets,
        AndroidInferenceResourceProbe(context, resourcePolicy)::read,
        { Process.is64Bit() && Build.SUPPORTED_ABIS.any {
            it == "arm64-v8a" || it == "x86_64"
        } },
        LiteRtInferenceSessionFactory,
        Dispatchers.IO,
    )

    private val admission = Mutex()

    override suspend fun generate(request: LocalInferenceRequest): LocalInferenceResult {
        currentCoroutineContext().ensureActive()
        if (!admission.tryLock()) return LocalInferenceResult.Busy
        var session: NativeInferenceSession? = null
        try {
            return withContext(dispatcher) {
                withTimeoutOrNull(budgets.timeoutMillis) {
                    if (!supportsAbi() || !resources().allowsHeavyWork) {
                        return@withTimeoutOrNull LocalInferenceResult.Unavailable
                    }
                    val file = model.verify(budgets.maxModelBytes)
                        ?: return@withTimeoutOrNull LocalInferenceResult.Unavailable
                    session = sessions.open(file, budgets, request.instructions)
                    currentCoroutineContext().ensureActive()
                    awaitOutput(checkNotNull(session), request)
                } ?: LocalInferenceResult.TimedOut
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return LocalInferenceResult.Failed
        } catch (_: LinkageError) {
            return LocalInferenceResult.Unavailable
        } finally {
            try {
                withContext(NonCancellable + dispatcher) { session?.close() }
            } catch (_: Exception) {
                // Native failures must never expose prompts through exception messages.
            } catch (_: LinkageError) {
            } finally {
                admission.unlock()
            }
        }
    }

    private suspend fun awaitOutput(
        session: NativeInferenceSession,
        request: LocalInferenceRequest,
    ): LocalInferenceResult {
        val completed = CompletableDeferred<Unit>()
        val output = StringBuilder()
        val stateLock = Any()
        var invalid = false
        try {
            session.start(
                request.input,
                request.responseSchema,
                onChunk = { chunk ->
                    val cancel = synchronized(stateLock) {
                        if (invalid) false
                        else if (chunk == null ||
                            chunk.length > budgets.outputCharacters - output.length) {
                            invalid = true
                            output.clear()
                            true
                        } else {
                            output.append(chunk)
                            false
                        }
                    }
                    if (cancel) runCatching { session.cancel() }
                },
                onTerminal = { successful ->
                    synchronized(stateLock) { if (!successful) invalid = true }
                    completed.complete(Unit)
                },
            )
        } catch (_: Exception) {
            synchronized(stateLock) { invalid = true }
            // JNI can fail synchronously. Cancel before the native destructor
            // waits for tasks.
            runCatching { session.cancel() }
            completed.complete(Unit)
        }
        try {
            completed.await()
            return synchronized(stateLock) {
                if (invalid || output.isEmpty()) LocalInferenceResult.Failed
                else LocalInferenceResult.Completed(output.toString())
            }
        } finally {
            withContext(NonCancellable) {
                if (!completed.isCompleted) {
                    runCatching { session.cancel() }
                    completed.await()
                }
                synchronized(stateLock) { output.clear() }
            }
        }
    }
}

internal fun interface NativeInferenceSessionFactory {
    fun open(
        file: File,
        budgets: LocalInferenceBudgets,
        instructions: String,
    ): NativeInferenceSession
}

internal interface NativeInferenceSession {
    fun start(
        input: String,
        responseSchema: String,
        onChunk: (String?) -> Unit,
        onTerminal: (Boolean) -> Unit,
    )
    fun cancel()
    /** Waits for native tasks before freeing resources, also after synchronous start failure. */
    fun close()
}

private object LiteRtInferenceSessionFactory : NativeInferenceSessionFactory {
    override fun open(
        file: File,
        budgets: LocalInferenceBudgets,
        instructions: String,
    ): NativeInferenceSession {
        Engine.setNativeMinLogSeverity(LogSeverity.INFINITY)
        val engine = Engine(
            EngineConfig(
                modelPath = file.absolutePath,
                backend = Backend.CPU(threadCount = budgets.cpuThreads),
                maxNumTokens = budgets.contextTokens,
                cacheDir = ":nocache",
            )
        )
        try {
            engine.initialize()
            val conversation = engine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(instructions),
                    tools = emptyList(),
                    automaticToolCalling = false,
                    maxOutputToken = budgets.outputTokens,
                    thinkingConfig = ThinkingConfig(false, 0),
                    enableResponseFormat = true,
                )
            )
            return LiteRtInferenceSession(engine, conversation)
        } catch (error: Throwable) {
            if (engine.isInitialized()) runCatching { engine.close() }
            throw error
        }
    }
}

private class LiteRtInferenceSession(
    private val engine: Engine,
    private val conversation: Conversation,
) : NativeInferenceSession {
    override fun start(
        input: String,
        responseSchema: String,
        onChunk: (String?) -> Unit,
        onTerminal: (Boolean) -> Unit,
    ) {
        conversation.sendMessageAsync(
            input,
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    if (message.toolCalls.isNotEmpty()) {
                        onChunk(null)
                        return
                    }
                    for (content in message.contents.contents) {
                        onChunk((content as? Content.Text)?.text)
                    }
                }

                override fun onDone() = onTerminal(true)

                override fun onError(throwable: Throwable) = onTerminal(false)
            },
            responseFormat = ResponseFormat.json(responseSchema),
        )
    }

    override fun cancel() = conversation.cancelProcess()

    override fun close() {
        try {
            conversation.close()
        } finally {
            engine.close()
        }
    }
}
