package io.blindado.android.protection

import io.blindado.android.domain.ErrorReason
import io.blindado.android.domain.ProtectionProfile
import io.blindado.android.domain.ProtectionState
import io.blindado.android.fake.FakeProtectionManaging
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/**
 * Regras do contrato de `specs/001-dns-privado-android/contracts/protection-managing.md`:
 * uma falha de [ProtectionManaging.install] SEMPRE carrega um [ErrorReason] real — nunca um
 * motivo inventado (FR-005). Este teste garante que a exceção propagada preserva exatamente o
 * motivo configurado, sem qualquer substituição por um valor genérico ao longo do caminho.
 */
class ProtectionManagingContractTest {

    @Test
    fun `install propaga o ErrorReason real configurado, sem inventar outro`() = runTest {
        val fake = FakeProtectionManaging()
        val causaOriginal = RuntimeException("erro real do sistema Android")
        fake.installFailure = ProtectionException(ErrorReason.FALHA_DESCONHECIDA, causaOriginal)

        try {
            fake.install(ProtectionProfile.default("adguard-padrao"))
            fail("Esperava ProtectionException")
        } catch (e: ProtectionException) {
            assertEquals(ErrorReason.FALHA_DESCONHECIDA, e.reason)
            assertEquals(causaOriginal, e.cause)
        }
    }

    @Test
    fun `sem installFailure configurado, install tem sucesso e fica Blindado`() = runTest {
        val fake = FakeProtectionManaging()
        assertNull(fake.installFailure)

        fake.install(ProtectionProfile.default("adguard-padrao"))

        assertEquals(ProtectionState.Blindado, fake.currentState())
    }

    @Test
    fun `PERMISSAO_REVOGADA so aparece quando forcado, nunca por padrao`() = runTest {
        val fake = FakeProtectionManaging()
        fake.install(ProtectionProfile.default("adguard-padrao"))
        assertEquals(ProtectionState.Blindado, fake.currentState())

        fake.forceState(ProtectionState.Erro(ErrorReason.PERMISSAO_REVOGADA))

        assertEquals(ProtectionState.Erro(ErrorReason.PERMISSAO_REVOGADA), fake.currentState())
    }
}
