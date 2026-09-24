package io.blindado.android.ui.level

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.blindado.android.domain.DnsProvider
import io.blindado.android.domain.ProtectionLevel

@Composable
fun ProtectionLevelScreen(
    viewModel: ProtectionLevelViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile by viewModel.profile.collectAsState()
    val customUrlError by viewModel.customUrlError.collectAsState()
    var customUrlText by remember { mutableStateOf(profile.customDohUrl.orEmpty()) }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Voltar" }) {
            Text("← Voltar")
        }

        Text("Nível de proteção", style = MaterialTheme.typography.titleLarge)

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))

        ProtectionLevel.entries.forEach { level ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile.level == level,
                        onClick = { viewModel.selectLevel(level) },
                    )
                    .semantics { contentDescription = levelLabel(level) }
                    .padding(vertical = 8.dp),
            ) {
                RadioButton(selected = profile.level == level, onClick = { viewModel.selectLevel(level) })
                Text(levelLabel(level), modifier = Modifier.padding(start = 8.dp))
            }
        }

        if (profile.level == ProtectionLevel.PERSONALIZADO) {
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
            TextField(
                value = customUrlText,
                onValueChange = { customUrlText = it },
                label = { Text("Endereço do servidor DoH (https://...)") },
                isError = customUrlError != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Endereço do servidor DoH personalizado" },
            )
            customUrlError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = { viewModel.selectCustomUrl(customUrlText) }) {
                Text("Salvar endereço personalizado")
            }
        } else {
            val providers = viewModel.providersFor(profile.level)
            if (providers.size > 1) {
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
                Text("Provedor", style = MaterialTheme.typography.titleMedium)
                providers.forEach { provider: DnsProvider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = profile.providerId == provider.id,
                                onClick = { viewModel.selectProvider(provider.id) },
                            )
                            .semantics { contentDescription = provider.displayName }
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(
                            selected = profile.providerId == provider.id,
                            onClick = { viewModel.selectProvider(provider.id) },
                        )
                        Text(provider.displayName, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

private fun levelLabel(level: ProtectionLevel): String = when (level) {
    ProtectionLevel.PADRAO -> "Padrão"
    ProtectionLevel.FAMILIA -> "Família"
    ProtectionLevel.PERSONALIZADO -> "Personalizado"
}
