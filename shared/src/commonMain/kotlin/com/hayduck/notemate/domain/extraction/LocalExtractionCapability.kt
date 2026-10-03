package com.hayduck.notemate.domain.extraction

/** Optional inference status supplied by an actual platform capability. */
enum class LocalAiState {
    READY, DOWNLOAD_REQUIRED, DOWNLOADING, UNAVAILABLE, UNSUPPORTED, DEFERRED, FAILED,
}

enum class DeviceResourceState { AVAILABLE, CONSTRAINED, UNKNOWN }

/** Platform-owned signals; unknown readings deny heavy work. */
data class ExtractionDeviceConditions(
    val memory: DeviceResourceState,
    val battery: DeviceResourceState,
    val thermal: DeviceResourceState,
) {
    val allowsHeavyWork: Boolean get() =
        memory == DeviceResourceState.AVAILABLE && battery == DeviceResourceState.AVAILABLE &&
            thermal == DeviceResourceState.AVAILABLE
}
