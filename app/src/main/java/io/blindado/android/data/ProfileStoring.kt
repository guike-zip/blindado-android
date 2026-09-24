package io.blindado.android.data

import io.blindado.android.domain.ProtectionProfile
import kotlinx.coroutines.flow.Flow

/**
 * Interface por trás da qual fica a persistência do [ProtectionProfile] (Constituição,
 * Princípio V — testável). [ProtectionProfileStore] é a implementação real (DataStore
 * Preferences); [io.blindado.android.fake.FakeProfileStore] é a fake usada em testes.
 */
interface ProfileStoring {
    val profile: Flow<ProtectionProfile>
    suspend fun save(profile: ProtectionProfile)
}
