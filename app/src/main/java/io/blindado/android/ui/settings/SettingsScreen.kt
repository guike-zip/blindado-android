package io.blindado.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Tela de Transparência (User Story 4) — o que o app faz e não faz, provedor DNS atual, e link
 * para a política de privacidade completa.
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val providerName by viewModel.currentProviderName.collectAsState()
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.titleLarge)

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 24.dp))

        Text("Privacidade", style = MaterialTheme.typography.titleMedium)

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))

        Text(
            "O Blindado não coleta, transmite ou armazena nenhum dado seu em servidor próprio " +
                "— não existe backend. Não é uma VPN de anonimização: não esconde seu IP nem " +
                "troca sua localização, é só um túnel local para configurar DNS criptografado. " +
                "As únicas consultas de rede são as que o próprio DNS já exige, enviadas direto " +
                "ao provedor que você escolheu.",
            style = MaterialTheme.typography.bodyLarge,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))

        Text(
            "Provedor DNS atual: $providerName",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { contentDescription = "Provedor DNS atual: $providerName" },
        )

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 24.dp))

        TextButton(
            onClick = {
                // TODO(T039): substituir pelo link real da política de privacidade publicada,
                // mesma dinâmica do app irmão de iOS (GitHub Pages ou equivalente).
                uriHandler.openUri("https://example.com/blindado-privacidade")
            },
            modifier = Modifier.semantics { contentDescription = "Ler a política de privacidade completa" },
        ) {
            Text("Ler a política de privacidade completa")
        }
    }
}
