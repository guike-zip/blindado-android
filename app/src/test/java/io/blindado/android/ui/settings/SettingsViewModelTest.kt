package io.blindado.android.ui.settings

import io.blindado.android.domain.DnsProvider
import io.blindado.android.domain.ProtectionLevel
import io.blindado.android.domain.ProtectionProfile
import io.blindado.android.fake.FakeProfileStore
import io.blindado.android.protection.ProviderCatalog
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun `nome do provedor conhecido bate com o perfil persistido`() = runTest {
        val store = FakeProfileStore(
            ProtectionProfile(level = ProtectionLevel.PADRAO, providerId = ProviderCatalog.CONTROL_D_PADRAO.id),
        )
        val vm = SettingsViewModel(store)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ProviderCatalog.CONTROL_D_PADRAO.displayName, vm.currentProviderName.value)
    }

    @Test
    fun `URL personalizada e exibida quando o provedor e custom`() = runTest {
        val store = FakeProfileStore(
            ProtectionProfile(
                level = ProtectionLevel.PERSONALIZADO,
                providerId = DnsProvider.CUSTOM_PROVIDER_ID,
                customDohUrl = "https://dns.example.com/dns-query",
            ),
        )
        val vm = SettingsViewModel(store)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://dns.example.com/dns-query", vm.currentProviderName.value)
    }
}
