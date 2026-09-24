package io.blindado.android.ui.level

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.blindado.android.data.ProfileStoring
import io.blindado.android.domain.DnsProvider
import io.blindado.android.domain.ProtectionLevel
import io.blindado.android.domain.ProtectionProfile
import io.blindado.android.protection.DohUrlValidator
import io.blindado.android.protection.ProtectionManaging
import io.blindado.android.protection.ProviderCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel da User Story 2 — Escolher o nível de proteção. */
class ProtectionLevelViewModel(
    private val protectionManaging: ProtectionManaging,
    private val profileStore: ProfileStoring,
    private val urlValidator: DohUrlValidator = DohUrlValidator(),
) : ViewModel() {

    val profile: StateFlow<ProtectionProfile> = profileStore.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProtectionProfile.default(ProviderCatalog.ADGUARD_PADRAO.id),
    )

    private val _customUrlError = MutableStateFlow<String?>(null)
    val customUrlError: StateFlow<String?> = _customUrlError

    fun providersFor(level: ProtectionLevel): List<DnsProvider> = ProviderCatalog.providersFor(level)

    fun selectLevel(level: ProtectionLevel) {
        if (level == ProtectionLevel.PERSONALIZADO) {
            // Aguarda o usuário informar/validar a URL — não aplica ainda.
            return
        }
        val provider = ProviderCatalog.defaultProviderFor(level) ?: return
        applyProfile(ProtectionProfile(level = level, providerId = provider.id))
    }

    fun selectProvider(providerId: String) {
        val current = profile.value
        applyProfile(current.copy(providerId = providerId))
    }

    fun selectCustomUrl(url: String) {
        viewModelScope.launch {
            when (val result = urlValidator.validate(url)) {
                is DohUrlValidator.Result.Invalid -> _customUrlError.value = result.reason
                DohUrlValidator.Result.Valid -> {
                    _customUrlError.value = null
                    applyProfile(
                        ProtectionProfile(
                            level = ProtectionLevel.PERSONALIZADO,
                            providerId = DnsProvider.CUSTOM_PROVIDER_ID,
                            customDohUrl = url,
                        ),
                    )
                }
            }
        }
    }

    private fun applyProfile(newProfile: ProtectionProfile) {
        viewModelScope.launch {
            profileStore.save(newProfile)
            // FR-009: reaplica sem exigir nova permissão — só se a proteção já estiver ativa.
            runCatching { protectionManaging.updateProfile(newProfile) }
        }
    }
}
