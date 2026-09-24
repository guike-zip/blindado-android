package io.blindado.android.protection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Checa se um domínio resolve via o resolvedor DNS atual do sistema (que, com a proteção
 * ativa, é a própria `BlindadoVpnService`) — usado pela tela de Teste (User Story 3). Interface
 * para permitir uma fake sem depender de resolução DNS real em teste unitário (Princípio V).
 */
interface DomainResolving {
    suspend fun isReachable(domain: String): Boolean
}

class SystemDomainResolving : DomainResolving {
    override suspend fun isReachable(domain: String): Boolean = withContext(Dispatchers.IO) {
        try {
            InetAddress.getByName(domain)
            true
        } catch (e: UnknownHostException) {
            false
        }
    }
}
