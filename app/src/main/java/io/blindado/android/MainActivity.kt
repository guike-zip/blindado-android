package io.blindado.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.blindado.android.data.ProtectionProfileStore
import io.blindado.android.protection.ProviderCatalog
import io.blindado.android.protection.RealProtectionManaging
import io.blindado.android.ui.home.HomeScreen
import io.blindado.android.ui.home.HomeViewModel
import io.blindado.android.ui.level.ProtectionLevelScreen
import io.blindado.android.ui.level.ProtectionLevelViewModel
import io.blindado.android.ui.settings.SettingsScreen
import io.blindado.android.ui.settings.SettingsViewModel
import io.blindado.android.ui.test.TestScreen
import io.blindado.android.ui.test.TestViewModel
import io.blindado.android.ui.theme.BlindadoTheme

private enum class BottomDestination(val label: String) {
    INICIO("Início"),
    TESTAR("Testar"),
    AJUSTES("Ajustes"),
}

class MainActivity : ComponentActivity() {

    // Sem framework de DI (Constituição, Princípio VII) — composição manual e simples aqui,
    // no único lugar do app que precisa de um Context de Activity.
    private val protectionManaging by lazy { RealProtectionManaging(applicationContext) }
    private val profileStore by lazy {
        ProtectionProfileStore(applicationContext, ProviderCatalog.ADGUARD_PADRAO.id)
    }

    private val homeViewModel by viewModels<HomeViewModel> {
        viewModelFactory { initializer { HomeViewModel(protectionManaging, profileStore) } }
    }
    private val levelViewModel by viewModels<ProtectionLevelViewModel> {
        viewModelFactory { initializer { ProtectionLevelViewModel(protectionManaging, profileStore) } }
    }
    private val testViewModel by viewModels<TestViewModel> {
        viewModelFactory {
            initializer {
                TestViewModel(
                    connectivityChecking = io.blindado.android.protection.NetworkStatus(applicationContext),
                    domainResolving = io.blindado.android.protection.SystemDomainResolving(),
                )
            }
        }
    }
    private val settingsViewModel by viewModels<SettingsViewModel> {
        viewModelFactory { initializer { SettingsViewModel(profileStore) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlindadoTheme {
                BlindadoApp(
                    homeViewModel = homeViewModel,
                    levelViewModel = levelViewModel,
                    testViewModel = testViewModel,
                    settingsViewModel = settingsViewModel,
                )
            }
        }
    }
}

@Composable
private fun BlindadoApp(
    homeViewModel: HomeViewModel,
    levelViewModel: ProtectionLevelViewModel,
    testViewModel: TestViewModel,
    settingsViewModel: SettingsViewModel,
) {
    var selected by remember { mutableStateOf(BottomDestination.INICIO) }
    var showLevelDetail by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selected == BottomDestination.INICIO,
                    onClick = { selected = BottomDestination.INICIO; showLevelDetail = false },
                    icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
                    label = { Text(BottomDestination.INICIO.label) },
                )
                NavigationBarItem(
                    selected = selected == BottomDestination.TESTAR,
                    onClick = { selected = BottomDestination.TESTAR },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    label = { Text(BottomDestination.TESTAR.label) },
                )
                NavigationBarItem(
                    selected = selected == BottomDestination.AJUSTES,
                    onClick = { selected = BottomDestination.AJUSTES },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(BottomDestination.AJUSTES.label) },
                )
            }
        },
    ) { innerPadding ->
        when {
            selected == BottomDestination.INICIO && showLevelDetail ->
                ProtectionLevelScreen(
                    viewModel = levelViewModel,
                    onBack = { showLevelDetail = false },
                    modifier = Modifier.padding(innerPadding),
                )
            selected == BottomDestination.INICIO ->
                HomeScreen(
                    viewModel = homeViewModel,
                    onOpenLevel = { showLevelDetail = true },
                    modifier = Modifier.padding(innerPadding),
                )
            selected == BottomDestination.TESTAR ->
                TestScreen(viewModel = testViewModel, modifier = Modifier.padding(innerPadding))
            selected == BottomDestination.AJUSTES ->
                SettingsScreen(viewModel = settingsViewModel, modifier = Modifier.padding(innerPadding))
        }
    }
}
