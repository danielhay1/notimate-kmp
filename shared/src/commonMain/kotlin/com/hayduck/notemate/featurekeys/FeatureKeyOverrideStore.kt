package com.hayduck.notemate.featurekeys

/** Main-safe storage of one debug override snapshot, independent of the bundled defaults. */
interface FeatureKeyOverrideStore {
    /** Returns null on a fresh installation; storage failures must throw without resetting data. */
    suspend fun read(): String?

    /** Replaces the snapshot, throwing if storage cannot accept it. */
    suspend fun write(overrides: String)
}
