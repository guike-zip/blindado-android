package io.blindado.android.domain

/** Nível de proteção escolhido pelo usuário — determina o [DnsProvider] usado. */
enum class ProtectionLevel {
    /** Bloqueia anúncios e rastreadores conhecidos. */
    PADRAO,

    /** Também bloqueia conteúdo adulto. */
    FAMILIA,

    /** Usuário define o próprio servidor DoH. */
    PERSONALIZADO,
}
