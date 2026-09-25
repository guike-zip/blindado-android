package io.blindado.android.protection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import io.blindado.android.MainActivity
import io.blindado.android.R
import io.blindado.android.domain.ErrorReason
import io.blindado.android.domain.ProtectionProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * `VpnService` local — nenhum servidor remoto do Blindado, só intercepta e resolve DNS no
 * próprio aparelho (research.md #3). Registra-se como o resolvedor DNS do sistema
 * (`addDnsServer` + rota só para esse endereço virtual), então o Android só entrega a esta
 * interface TUN o tráfego DNS — nenhum outro tráfego dos outros apps passa por aqui.
 *
 * `addDisallowedApplication(packageName)` exclui o PRÓPRIO Blindado do seu túnel (research.md
 * #7 update, achado em teste em dispositivo físico real): sem isso, a resolução do hostname do
 * provedor DoH (ex. `dns.adguard-dns.com`, `DohResolver`/`ProviderCatalog`) vira, ela mesma,
 * uma consulta DNS que o sistema roteia de volta para este túnel — mas o único código capaz de
 * responder a essa consulta (`runPacketLoop`, single-threaded) já está bloqueado esperando essa
 * mesma resolução terminar. Deadlock circular que derruba TODA resolução DNS do aparelho
 * (inclusive de outros apps, ex. Play Store), não só domínios bloqueados.
 *
 * PENDÊNCIA (research.md #2, tasks.md T046): o `foregroundServiceType="specialUse"` declarado
 * no Manifest ainda precisa ser confirmado em dispositivo físico Android 14+ antes de considerar
 * este serviço pronto para produção.
 */
class BlindadoVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "io.blindado.android.action.CONNECT"
        const val ACTION_DISCONNECT = "io.blindado.android.action.DISCONNECT"
        const val EXTRA_DOH_ENDPOINT = "extra_doh_endpoint"

        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ADDRESS_PREFIX_LENGTH = 32
        private const val NOTIFICATION_CHANNEL_ID = "blindado_protection"
        private const val NOTIFICATION_ID = 1

        /**
         * Estado observável do serviço, lido por [RealProtectionManaging]. `null` = serviço não
         * está rodando. Companion porque só pode existir uma `VpnService` ativa por vez no
         * Android — não há necessidade de instância injetável aqui (Constituição, Princípio VII).
         */
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        private val _lastError = MutableStateFlow<ErrorReason?>(null)
        val lastError: StateFlow<ErrorReason?> = _lastError
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunInterface: ParcelFileDescriptor? = null
    private var packetLoopJob: Job? = null
    private val blockList = BlockList()
    private val dohResolver = DohResolver()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val dohEndpoint = intent.getStringExtra(EXTRA_DOH_ENDPOINT)
                if (dohEndpoint == null) {
                    _lastError.value = ErrorReason.FALHA_DESCONHECIDA
                    stopSelf()
                } else {
                    connect(dohEndpoint)
                }
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_NOT_STICKY
    }

    private fun connect(dohEndpoint: String) {
        try {
            val builder = Builder()
                .addAddress(VPN_ADDRESS, VPN_ADDRESS_PREFIX_LENGTH)
                .addDnsServer(VPN_ADDRESS)
                .addRoute(VPN_ADDRESS, 32) // só o endereço DNS virtual — nada de 0.0.0.0/0
                .addDisallowedApplication(packageName) // ver nota abaixo — achado em teste real de hardware
                .setSession("Blindado")
                .setBlocking(false)

            tunInterface = builder.establish()
            if (tunInterface == null) {
                _lastError.value = ErrorReason.PERMISSAO_NEGADA
                stopSelf()
                return
            }

            startForeground(NOTIFICATION_ID, buildNotification())

            serviceScope.launch {
                blockList.load(this@BlindadoVpnService)
            }

            packetLoopJob = serviceScope.launch {
                runPacketLoop(dohEndpoint)
            }

            _isRunning.value = true
            _lastError.value = null
        } catch (e: Exception) {
            _lastError.value = ErrorReason.FALHA_DESCONHECIDA
            _isRunning.value = false
            stopSelf()
        }
    }

    private suspend fun runPacketLoop(dohEndpoint: String) {
        val fd = tunInterface ?: return
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(32_767)

        while (_isRunning.value) {
            val length = try {
                input.read(buffer)
            } catch (e: IOException) {
                break // interface fechada (disconnect) — sai do loop
            }
            if (length <= 0) continue

            val query = DnsPacketCodec.tryParseDnsQuery(buffer, length) ?: continue
            val queryName = DnsPacketCodec.extractQueryName(query.dnsPayload)

            val responsePayload = if (queryName != null && blockList.isBlocked(queryName)) {
                DnsPacketCodec.buildBlockedResponse(query.dnsPayload)
            } else {
                try {
                    dohResolver.resolve(dohEndpoint, query.dnsPayload)
                } catch (e: DohResolver.DohException) {
                    // Falha real de rede/DoH — não bloqueia nem trava, só não responde a essa
                    // consulta específica (o cliente original tentará de novo).
                    continue
                }
            }

            val responsePacket = DnsPacketCodec.buildDnsResponsePacket(query, responsePayload)
            try {
                output.write(responsePacket)
            } catch (e: IOException) {
                break
            }
        }
    }

    private fun disconnect() {
        _isRunning.value = false
        packetLoopJob?.cancel()
        tunInterface?.let {
            try {
                it.close()
            } catch (e: IOException) {
                // Interface já fechada — nada a fazer.
            }
        }
        tunInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    override fun onRevoke() {
        // O sistema revogou a permissão de VPN (outro app assumiu, ou o usuário revogou nos
        // Ajustes) — FR-004. RealProtectionManaging detecta isso via isRunning=false + o
        // onDestroy/onRevoke do sistema, refletindo PERMISSAO_REVOGADA no próximo currentState().
        _lastError.value = ErrorReason.PERMISSAO_REVOGADA
        disconnect()
        super.onRevoke()
    }

    override fun onDestroy() {
        disconnect()
        serviceScope.cancel()
        super.onDestroy()
    }
}
