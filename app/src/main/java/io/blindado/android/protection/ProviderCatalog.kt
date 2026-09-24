package io.blindado.android.protection

import io.blindado.android.domain.DnsProvider
import io.blindado.android.domain.ProtectionLevel

/**
 * Catálogo dos provedores DoH suportados por [ProtectionLevel] (research.md #6) — mesmos dois
 * provedores usados no app irmão de iOS, para consistência de produto.
 *
 * PENDÊNCIA: os hostnames abaixo são os endpoints públicos conhecidos de cada provedor no
 * momento da implementação — DEVEM ser reconfirmados contra a documentação oficial atual da
 * AdGuard e da Control D antes do lançamento (research.md #6 pede explicitamente para não
 * inventar isso sem checar).
 */
object ProviderCatalog {

    val ADGUARD_PADRAO = DnsProvider(
        id = "adguard-padrao",
        displayName = "AdGuard DNS",
        dohEndpoint = "https://dns.adguard-dns.com/dns-query",
    )
    val ADGUARD_FAMILIA = DnsProvider(
        id = "adguard-familia",
        displayName = "AdGuard DNS Família",
        dohEndpoint = "https://family.adguard-dns.com/dns-query",
    )
    val CONTROL_D_PADRAO = DnsProvider(
        id = "control-d-padrao",
        displayName = "Control D",
        dohEndpoint = "https://freedns.controld.com/p2",
    )
    val CONTROL_D_FAMILIA = DnsProvider(
        id = "control-d-familia",
        displayName = "Control D Família",
        dohEndpoint = "https://freedns.controld.com/family",
    )

    private val byLevel: Map<ProtectionLevel, List<DnsProvider>> = mapOf(
        ProtectionLevel.PADRAO to listOf(ADGUARD_PADRAO, CONTROL_D_PADRAO),
        ProtectionLevel.FAMILIA to listOf(ADGUARD_FAMILIA, CONTROL_D_FAMILIA),
        ProtectionLevel.PERSONALIZADO to emptyList(),
    )

    private val allKnown: Map<String, DnsProvider> =
        (byLevel.values.flatten()).associateBy { it.id }

    /** Provedores suportados para um [ProtectionLevel] — vazio para PERSONALIZADO. */
    fun providersFor(level: ProtectionLevel): List<DnsProvider> = byLevel[level].orEmpty()

    /** O provedor padrão de um nível — o primeiro da lista suportada. */
    fun defaultProviderFor(level: ProtectionLevel): DnsProvider? = byLevel[level]?.firstOrNull()

    /** Resolve um [DnsProvider] pelo id, entre todos os provedores conhecidos (Padrão+Família). */
    fun byId(id: String): DnsProvider? = allKnown[id]
}
