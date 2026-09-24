package io.blindado.android.ui.test

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.blindado.android.domain.DomainCheck
import io.blindado.android.domain.DomainStatus
import io.blindado.android.domain.OverallResult
import io.blindado.android.domain.TestResult
import io.blindado.android.protection.NetworkStatus
import io.blindado.android.protection.TestDomains
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException

/** ViewModel da User Story 3 — Testar a proteção. */
class TestViewModel(context: Context) : ViewModel() {

    private val networkStatus = NetworkStatus(context.applicationContext)

    private val _result = MutableStateFlow<TestResult?>(null)
    val result: StateFlow<TestResult?> = _result

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    fun runTest() {
        viewModelScope.launch {
            _isRunning.value = true

            if (!networkStatus.hasActiveConnection()) {
                // FR-012: sem rede real, o resultado é sempre Indeterminado — nunca
                // Protegido nem Desprotegido.
                _result.value = TestResult(
                    items = TestDomains.ALL.map {
                        DomainCheck(it.domain, it.category, DomainStatus.VERIFICANDO)
                    },
                    overall = OverallResult.INDETERMINADO,
                )
                _isRunning.value = false
                return@launch
            }

            val items = mutableListOf<DomainCheck>()
            for (testDomain in TestDomains.ALL) {
                val status = checkDomain(testDomain.domain)
                items.add(DomainCheck(testDomain.domain, testDomain.category, status))
                _result.value = TestResult(items.toList(), overall = OverallResult.INDETERMINADO)
            }

            val trackerItems = items.filter { it.category != "Comum" }
            val allTrackersBlocked = trackerItems.isNotEmpty() && trackerItems.all { it.status == DomainStatus.BLOQUEADO }
            val allTrackersAccessible = trackerItems.isNotEmpty() && trackerItems.all { it.status == DomainStatus.ACESSIVEL }

            val overall = when {
                allTrackersBlocked -> OverallResult.PROTEGIDO
                allTrackersAccessible -> OverallResult.DESPROTEGIDO
                else -> OverallResult.DESPROTEGIDO // proteção parcial conta como desprotegido — sem meio-termo ambíguo
            }

            _result.value = TestResult(items, overall)
            _isRunning.value = false
        }
    }

    private suspend fun checkDomain(domain: String): DomainStatus = withContext(Dispatchers.IO) {
        try {
            InetAddress.getByName(domain)
            DomainStatus.ACESSIVEL
        } catch (e: UnknownHostException) {
            DomainStatus.BLOQUEADO
        }
    }
}
