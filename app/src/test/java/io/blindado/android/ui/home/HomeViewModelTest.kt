package io.blindado.android.ui.home

import io.blindado.android.data.ProfileStoring
import io.blindado.android.domain.ErrorReason
import io.blindado.android.domain.ProtectionLevel
import io.blindado.android.domain.ProtectionProfile
import io.blindado.android.domain.ProtectionState
import io.blindado.android.fake.FakeProfileStore
import io.blindado.android.fake.FakeProtectionManaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre as 5 transições de estado de `data-model.md` § ProtectionState, incluindo
 * `Erro(PERMISSAO_REVOGADA)` forçado via [FakeProtectionManaging] — tudo sem dispositivo físico
 * nem VpnService real (Constituição, Princípio V).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var fakeManaging: FakeProtectionManaging
    private lateinit var fakeStore: ProfileStoring

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakeManaging = FakeProtectionManaging()
        fakeStore = FakeProfileStore(ProtectionProfile.default("adguard-padrao"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HomeViewModel(fakeManaging, fakeStore)

    @Test
    fun `estado inicial e NaoConfigurado`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProtectionState.NaoConfigurado, vm.state.value)
    }

    @Test
    fun `blindar sem permissao pendente instala e fica Blindado`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.blindar()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ProtectionState.Blindado, vm.state.value)
    }

    @Test
    fun `blindar com permissao pendente emite permissionRequest e nao instala ainda`() = runTest {
        // Um FakeProtectionManaging normal nunca pede permissão (vpnPermissionIntent = null),
        // então simulamos o caso "permissão pendente" via subclasse anônima mínima.
        val managingComPermissaoPendente = object : io.blindado.android.protection.ProtectionManaging by fakeManaging {
            override fun vpnPermissionIntent(): android.content.Intent =
                android.content.Intent("android.net.VpnService")
        }
        val vm = HomeViewModel(managingComPermissaoPendente, fakeStore)
        dispatcher.scheduler.advanceUntilIdle()

        vm.blindar()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.permissionRequest.value != null)
        assertEquals(ProtectionState.NaoConfigurado, vm.state.value) // ainda não instalou
    }

    @Test
    fun `permissao negada resulta em Erro PERMISSAO_NEGADA`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPermissionResult(granted = false)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ProtectionState.Erro(ErrorReason.PERMISSAO_NEGADA), vm.state.value)
    }

    @Test
    fun `revogacao externa e refletida via stateChanges`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.blindar()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProtectionState.Blindado, vm.state.value)

        fakeManaging.forceState(ProtectionState.Erro(ErrorReason.PERMISSAO_REVOGADA))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ProtectionState.Erro(ErrorReason.PERMISSAO_REVOGADA), vm.state.value)
    }

    @Test
    fun `remover volta para NaoConfigurado`() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.blindar()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ProtectionState.Blindado, vm.state.value)

        vm.remover()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ProtectionState.NaoConfigurado, vm.state.value)
    }
}
