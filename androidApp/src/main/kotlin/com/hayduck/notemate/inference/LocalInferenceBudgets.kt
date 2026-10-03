package com.hayduck.notemate.inference

/** Explicit device-qualified limits; file size does not predict the model's peak memory usage. */
class LocalInferenceBudgets(
    val contextTokens: Int,
    val outputTokens: Int,
    val outputCharacters: Int,
    val cpuThreads: Int,
    val timeoutMillis: Long,
    val maxModelBytes: Long,
) {
    init {
        require(contextTokens > outputTokens && outputTokens > 0 && outputCharacters in 1..8192 &&
            cpuThreads > 0 && timeoutMillis > 0 && maxModelBytes > 0) {
            "Invalid inference budgets."
        }
    }
}
