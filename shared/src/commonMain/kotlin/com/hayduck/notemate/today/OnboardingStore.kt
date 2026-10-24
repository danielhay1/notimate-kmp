package com.hayduck.notemate.today

enum class OnboardingStep { WELCOME, PRIVACY, ACCESS, ALERTS, COMPLETE }

/** Local education progress and permission history; current grants always come from the OS. */
data class OnboardingPreferences(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val hasHadAccess: Boolean = false,
    val hasRequestedAlerts: Boolean = false,
) {
    val completed: Boolean get() = step == OnboardingStep.COMPLETE
}

/** Main-safe local progress storage. Invalid existing data must fail without being reset. */
interface OnboardingStore {
    suspend fun load(): OnboardingPreferences
    suspend fun save(preferences: OnboardingPreferences)
}
