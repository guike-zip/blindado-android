package io.blindado.android.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.blindado.android.domain.ErrorReason
import io.blindado.android.domain.ProtectionState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenLevel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val permissionRequest by viewModel.permissionRequest.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onPermissionResult(granted = result.resultCode == android.app.Activity.RESULT_OK)
    }

    LaunchedEffect(permissionRequest) {
        permissionRequest?.let { intent -> permissionLauncher.launch(intent) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ShieldIndicator(state)

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 24.dp))

        Text(
            text = stateTitle(state),
            style = MaterialTheme.typography.headlineMedium,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))

        Text(
            text = stateBody(state),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 32.dp))

        when (state) {
            is ProtectionState.Blindado -> {
                OutlinedButton(
                    onClick = onOpenLevel,
                    modifier = Modifier.semantics {
                        contentDescription = "Nível de proteção"
                    },
                ) {
                    Text("Nível de proteção")
                }

                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))

                OutlinedButton(
                    onClick = { viewModel.remover() },
                    enabled = !isBusy,
                    modifier = Modifier.semantics {
                        contentDescription = "Remover proteção"
                    },
                ) {
                    Text("Remover proteção")
                }
            }
            else -> {
                Button(
                    onClick = { viewModel.blindar() },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.semantics {
                        contentDescription = "Blindar meu Android"
                    },
                ) {
                    Text("Blindar meu Android")
                }
            }
        }
    }
}

@Composable
private fun ShieldIndicator(state: ProtectionState) {
    val (icon, description) = when (state) {
        is ProtectionState.Blindado -> Icons.Filled.CheckCircle to "Protegido"
        is ProtectionState.NaoConfigurado -> Icons.Filled.Shield to "Não configurado"
        is ProtectionState.Erro -> Icons.Filled.Shield to "Erro: ${state.motivo}"
    }
    val tint = if (state is ProtectionState.Blindado) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = Modifier.padding(16.dp),
    )
}

private fun stateTitle(state: ProtectionState): String = when (state) {
    is ProtectionState.Blindado -> "Seu Android está blindado"
    is ProtectionState.NaoConfigurado -> "Seu Android ainda não está protegido"
    is ProtectionState.Erro -> when (state.motivo) {
        ErrorReason.PERMISSAO_NEGADA -> "Permissão de VPN necessária"
        ErrorReason.PERMISSAO_REVOGADA -> "Proteção foi desativada"
        ErrorReason.FALHA_DESCONHECIDA -> "Não foi possível blindar o Android"
    }
}

private fun stateBody(state: ProtectionState): String = when (state) {
    is ProtectionState.Blindado ->
        "As consultas DNS saem criptografadas. Anúncios e rastreadores conhecidos são " +
            "bloqueados em qualquer app, não só no navegador."
    is ProtectionState.NaoConfigurado ->
        "Toque no botão abaixo — o Android vai pedir sua permissão de VPN, e a proteção liga " +
            "na hora, sem sair do app."
    is ProtectionState.Erro -> when (state.motivo) {
        ErrorReason.PERMISSAO_NEGADA ->
            "Sem a permissão de VPN o Blindado não consegue proteger seu Android. Toque em " +
                "\"Blindar meu Android\" para tentar de novo."
        ErrorReason.PERMISSAO_REVOGADA ->
            "A permissão de VPN foi revogada fora do app (outro app de VPN, ou nos Ajustes do " +
                "sistema). Toque para reativar."
        ErrorReason.FALHA_DESCONHECIDA ->
            "Algo deu errado ao ativar a proteção. Tente de novo — se persistir, isso é um bug " +
                "real, não um conflito."
    }
}
