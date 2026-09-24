package io.blindado.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import io.blindado.android.domain.ProtectionLevel
import io.blindado.android.domain.ProtectionProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.protectionProfileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "protection_profile",
)

/** Persiste o [ProtectionProfile] escolhido pelo usuário entre reaberturas do app (FR-008). */
class ProtectionProfileStore(
    private val context: Context,
    private val defaultProviderId: String,
) {
    private object Keys {
        val LEVEL = stringPreferencesKey("level")
        val PROVIDER_ID = stringPreferencesKey("provider_id")
        val CUSTOM_DOH_URL = stringPreferencesKey("custom_doh_url")
    }

    val profile: Flow<ProtectionProfile> = context.protectionProfileDataStore.data.map { prefs ->
        val level = prefs[Keys.LEVEL]?.let { ProtectionLevel.valueOf(it) } ?: ProtectionLevel.PADRAO
        val providerId = prefs[Keys.PROVIDER_ID] ?: defaultProviderId
        val customUrl = prefs[Keys.CUSTOM_DOH_URL]
        ProtectionProfile(level = level, providerId = providerId, customDohUrl = customUrl)
    }

    suspend fun save(profile: ProtectionProfile) {
        context.protectionProfileDataStore.edit { prefs ->
            prefs[Keys.LEVEL] = profile.level.name
            prefs[Keys.PROVIDER_ID] = profile.providerId
            if (profile.customDohUrl != null) {
                prefs[Keys.CUSTOM_DOH_URL] = profile.customDohUrl
            } else {
                prefs.remove(Keys.CUSTOM_DOH_URL)
            }
        }
    }
}
