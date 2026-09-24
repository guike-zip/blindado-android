package io.blindado.android.domain

/**
 * Um provedor de DNS criptografado suportado, ou o marcador "custom" para o endereço
 * personalizado informado pelo usuário no nível [ProtectionLevel.PERSONALIZADO].
 */
data class DnsProvider(
    val id: String,
    val displayName: String,
    val dohEndpoint: String,
) {
    companion object {
        const val CUSTOM_PROVIDER_ID: String = "custom"
    }
}
