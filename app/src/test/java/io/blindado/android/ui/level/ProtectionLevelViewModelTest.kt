package io.blindado.android.ui.level

import io.blindado.android.domain.ProtectionLevel
import io.blindado.android.domain.ProtectionProfile
import io.blindado.android.fake.FakeProfileStore
import io.blindado.android.fake.FakeProtectionManaging
import io.blindado.android.protection.DohUrlValidator
import io.blindado.android.protection.ProviderCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Fake sem rede real — controla o resultado de [validate] a partir do teste. */
private class FakeDohUrlValidator(private val result: Result) : DohUrlValidator() {
    override suspend fun validate(url: String): Result = result
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProtectionLevelViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var fakeManaging: FakeProtectionManaging
    private lateinit var fakeStore: FakeProfileStore

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakeManaging = FakeProtectionManaging()
        fakeStore = FakeProfileStore(ProtectionProfile.default(ProviderCatalog.ADGUARD_PADRAO.id))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `trocar de nivel aplica o provedor padrao daquele nivel`() = runTest {
        val vm = ProtectionLevelViewModel(fakeManaging, fakeStore)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectLevel(ProtectionLevel.FAMILIA)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ProtectionLevel.FAMILIA, vm.profile.value.level)
        assertEquals(ProviderCatalog.defaultProviderFor(ProtectionLevel.FAMILIA)?.id, vm.profile.value.providerId)
    }

    @Test
    fun `trocar de provedor dentro do mesmo nivel`() = runTest {
        val vm = ProtectionLevelViewModel(fakeManaging, fakeStore)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectProvider(ProviderCatalog.CONTROL_D_PADRAO.id)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ProviderCatalog.CONTROL_D_PADRAO.id, vm.profile.value.providerId)
        assertEquals(ProtectionLevel.PADRAO, vm.profile.value.level) // nível não muda
    }

    @Test
    fun `URL personalizada valida e salva e aplicada`() = runTest {
        val validator = FakeDohUrlValidator(DohUrlValidator.Result.Valid)
        val vm = ProtectionLevelViewModel(fakeManaging, fakeStore, validator)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectCustomUrl("https://dns.example.com/dns-query")
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.customUrlError.value)
        assertEquals(ProtectionLevel.PERSONALIZADO, vm.profile.value.level)
        assertEquals("https://dns.example.com/dns-query", vm.profile.value.customDohUrl)
    }

    @Test
    fun `URL personalizada invalida nao e salva`() = runTest {
        val validator = FakeDohUrlValidator(DohUrlValidator.Result.Invalid("endereço inválido"))
        val vm = ProtectionLevelViewModel(fakeManaging, fakeStore, validator)
        dispatcher.scheduler.advanceUntilIdle()
        val profileAntes = vm.profile.value

        vm.selectCustomUrl("não é uma url")
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.customUrlError.value)
        assertEquals(profileAntes, vm.profile.value) // perfil não mudou
    }
}
