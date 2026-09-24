package io.blindado.android.domain

/**
 * A escolha persistida do usuário (DataStore Preferences) — nível + provedor (ou URL
 * personalizada) escolhidos, sobrevivendo a reaberturas do app (FR-008).
 */
data class ProtectionProfile(
    val level: ProtectionLevel,
    val providerId: String,
    val customDohUrl: String? = null,
) {
    companion object {
        /**
         * Valor padrão de primeira instalação, antes de qualquer escolha do usuário — mesma
         * convenção do app irmão de iOS: nunca deixar o usuário sem um nível/provedor
         * implícito ao ativar pela primeira vez.
         */
        fun default(defaultProviderId: String): ProtectionProfile =
            ProtectionProfile(level = ProtectionLevel.PADRAO, providerId = defaultProviderId)
    }
}
