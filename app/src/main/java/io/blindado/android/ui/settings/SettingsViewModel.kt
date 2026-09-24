package io.blindado.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.blindado.android.data.ProfileStoring
import io.blindado.android.domain.DnsProvider
import io.blindado.android.protection.ProviderCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** ViewModel da User Story 4 — Transparência. */
class SettingsViewModel(profileStore: ProfileStoring) : ViewModel() {

    val currentProviderName: StateFlow<String> = profileStore.profile.map { profile ->
        if (profile.providerId == DnsProvider.CUSTOM_PROVIDER_ID) {
            profile.customDohUrl ?: "servidor personalizado"
        } else {
            ProviderCatalog.byId(profile.providerId)?.displayName ?: "provedor desconhecido"
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "carregando…",
    )
}
