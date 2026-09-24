package io.blindado.android.fake

import io.blindado.android.data.ProfileStoring
import io.blindado.android.domain.ProtectionProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Implementação em memória de [ProfileStoring] para testes unitários — sem DataStore/Context. */
class FakeProfileStore(initial: ProtectionProfile) : ProfileStoring {
    private val _profile = MutableStateFlow(initial)
    override val profile: StateFlow<ProtectionProfile> = _profile

    override suspend fun save(profile: ProtectionProfile) {
        _profile.value = profile
    }
}
