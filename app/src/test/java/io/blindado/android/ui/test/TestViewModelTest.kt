package io.blindado.android.ui.test

import io.blindado.android.domain.OverallResult
import io.blindado.android.protection.ConnectivityChecking
import io.blindado.android.protection.DomainResolving
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakeConnectivity(private val active: Boolean) : ConnectivityChecking {
    override fun hasActiveConnection(): Boolean = active
}

private class FakeDomainResolving(private val reachable: Set<String>) : DomainResolving {
    override suspend fun isReachable(domain: String): Boolean = domain in reachable
}

/** FR-012: nunca inferir Protegido/Desprotegido sem conexão real. */
@OptIn(ExperimentalCoroutinesApi::class)
class TestViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sem rede o resultado e sempre Indeterminado`() = runTest {
        val vm = TestViewModel(FakeConnectivity(active = false), FakeDomainResolving(emptySet()))

        vm.runTest()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(OverallResult.INDETERMINADO, vm.result.value?.overall)
    }

    @Test
    fun `todos os rastreadores bloqueados resulta em Protegido`() = runTest {
        // "Comum" (example.com) acessível, rastreadores todos inacessíveis (bloqueados).
        val vm = TestViewModel(FakeConnectivity(active = true), FakeDomainResolving(setOf("example.com")))

        vm.runTest()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(OverallResult.PROTEGIDO, vm.result.value?.overall)
    }

    @Test
    fun `rastreadores acessiveis resulta em Desprotegido`() = runTest {
        // Tudo acessível, incluindo os rastreadores — proteção não está bloqueando nada.
        val todosAcessiveis = setOf(
            "doubleclick.net", "googleadservices.com", "google-analytics.com", "example.com",
        )
        val vm = TestViewModel(FakeConnectivity(active = true), FakeDomainResolving(todosAcessiveis))

        vm.runTest()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(OverallResult.DESPROTEGIDO, vm.result.value?.overall)
    }
}
