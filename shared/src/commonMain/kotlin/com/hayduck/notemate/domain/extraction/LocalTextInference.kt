package com.hayduck.notemate.domain.extraction

/** On-device inference; implementations own native cleanup and never log or persist prompts. */
fun interface LocalTextInference {
    /** Isolated conversation; caller cancellation stops inference before propagating. */
    suspend fun generate(request: LocalInferenceRequest): LocalInferenceResult
}

/** Transient request after source admission; [responseSchema] constrains advisory output only. */
class LocalInferenceRequest(
    val instructions: String,
    val input: String,
    val responseSchema: String,
) {
    init {
        require(instructions.length in 1..8192 && input.length in 1..32768 &&
            responseSchema.length in 1..8192) { "Invalid inference request bounds." }
    }

    override fun toString(): String = "LocalInferenceRequest([REDACTED])"
}

/** Typed outcomes contain no exception details; successful output is transient and untrusted. */
sealed interface LocalInferenceResult {
    class Completed(val output: String) : LocalInferenceResult {
        override fun toString(): String = "Completed([REDACTED])"
    }
    data object Unavailable : LocalInferenceResult
    data object Busy : LocalInferenceResult
    data object TimedOut : LocalInferenceResult
    data object Failed : LocalInferenceResult
}
