package com.hayduck.notemate.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayduck.notemate.domain.configuration.LocalConfigurationRepository
import com.hayduck.notemate.domain.results.LocalResultsRepository
import com.hayduck.notemate.permissions.AndroidPermissionGateway

internal class TodayViewModel(
    configurationRepository: LocalConfigurationRepository,
    resultsRepository: LocalResultsRepository,
    onboardingStore: OnboardingStore,
) : ViewModel() {
    val screen = TodayStateHolder(
        configurationRepository, resultsRepository, onboardingStore, viewModelScope,
    )

    fun refreshPermissions(gateway: AndroidPermissionGateway) {
        try {
            screen.updatePermissions(gateway.snapshot(screen.hasRequestedAlerts))
        } catch (_: RuntimeException) {
            screen.permissionCheckFailed()
        }
    }
}
