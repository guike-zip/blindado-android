package io.blindado.android.ui.test

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.blindado.android.domain.DomainStatus
import io.blindado.android.domain.OverallResult

@Composable
fun TestScreen(viewModel: TestViewModel, modifier: Modifier = Modifier) {
    val result by viewModel.result.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text("Testar a proteção", style = MaterialTheme.typography.titleLarge)

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))

        Button(
            onClick = { viewModel.runTest() },
            enabled = !isRunning,
            modifier = Modifier.semantics { contentDescription = "Rodar teste de proteção" },
        ) {
            Text(if (isRunning) "Testando..." else "Rodar teste")
        }

        result?.let { testResult ->
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
            Text(
                overallLabel(testResult.overall),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics {
                    contentDescription = "Resultado geral: ${overallLabel(testResult.overall)}"
                },
            )

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))

            LazyColumn {
                items(testResult.items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .semantics {
                                contentDescription = "${item.domain}: ${statusLabel(item.status)}"
                            },
                    ) {
                        Text(item.domain, modifier = Modifier.padding(end = 8.dp))
                        Text(statusLabel(item.status), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun overallLabel(overall: OverallResult): String = when (overall) {
    OverallResult.PROTEGIDO -> "Protegido"
    OverallResult.DESPROTEGIDO -> "Desprotegido"
    OverallResult.INDETERMINADO -> "Indeterminado — sem conexão de rede"
}

private fun statusLabel(status: DomainStatus): String = when (status) {
    DomainStatus.BLOQUEADO -> "Bloqueado"
    DomainStatus.ACESSIVEL -> "Acessível"
    DomainStatus.VERIFICANDO -> "Verificando..."
}
