package io.blindado.android.protection

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parsing/encoding mínimo de pacotes IPv4/UDP/DNS lidos e escritos diretamente na interface TUN
 * da [BlindadoVpnService]. Cobre só o necessário para interceptar e responder consultas DNS —
 * qualquer outro tráfego nunca chega aqui, porque a VpnService só captura pacotes destinados ao
 * endereço DNS virtual (research.md #3).
 *
 * Suporta IPv4 + UDP porta 53 nesta v1 (ver `spec.md`/`research.md` — IPv6 e TCP DNS ficam para
 * uma versão futura, não estavam no escopo decidido).
 */
object DnsPacketCodec {

    private const val IPV4_VERSION_IHL = 0x45 // versão 4, IHL 5 (20 bytes, sem opções)
    private const val PROTOCOL_UDP = 17
    const val DNS_PORT = 53

    /** Uma consulta DNS extraída de um pacote IPv4/UDP recebido da TUN. */
    data class ParsedDnsQuery(
        val sourceIp: ByteArray,
        val destIp: ByteArray,
        val sourcePort: Int,
        val destPort: Int,
        val dnsPayload: ByteArray,
    )

    /**
     * Tenta interpretar [packet] como uma consulta DNS IPv4/UDP destinada à porta 53. Retorna
     * `null` se não for (não deveria acontecer dado o roteamento da VpnService, mas o código
     * defende contra pacotes inesperados em vez de assumir).
     */
    fun tryParseDnsQuery(packet: ByteArray, length: Int): ParsedDnsQuery? {
        if (length < 28) return null // 20 (IPv4) + 8 (UDP) mínimo, sem payload DNS

        val versionAndIhl = packet[0].toInt() and 0xFF
        val version = versionAndIhl shr 4
        if (version != 4) return null // só IPv4 nesta v1

        val ihl = (versionAndIhl and 0x0F) * 4
        if (ihl < 20 || length < ihl + 8) return null

        val protocol = packet[9].toInt() and 0xFF
        if (protocol != PROTOCOL_UDP) return null

        val sourceIp = packet.copyOfRange(12, 16)
        val destIp = packet.copyOfRange(16, 20)

        val udpStart = ihl
        val sourcePort = readUShort(packet, udpStart)
        val destPort = readUShort(packet, udpStart + 2)
        if (destPort != DNS_PORT) return null

        val udpLength = readUShort(packet, udpStart + 4)
        val dnsStart = udpStart + 8
        val dnsLength = (udpLength - 8).coerceAtMost(length - dnsStart)
        if (dnsLength <= 0) return null

        val dnsPayload = packet.copyOfRange(dnsStart, dnsStart + dnsLength)
        return ParsedDnsQuery(sourceIp, destIp, sourcePort, destPort, dnsPayload)
    }

    /**
     * Constrói um pacote IPv4/UDP de resposta a partir de uma [ParsedDnsQuery] e o payload DNS
     * de resposta (real, vindo do [DohResolver], ou sintético de bloqueio do [BlockList]).
     * Origem/destino são invertidos em relação à consulta original.
     */
    fun buildDnsResponsePacket(query: ParsedDnsQuery, dnsResponsePayload: ByteArray): ByteArray {
        val udpLength = 8 + dnsResponsePayload.size
        val totalLength = 20 + udpLength
        val buffer = ByteBuffer.allocate(totalLength).order(ByteOrder.BIG_ENDIAN)

        // --- Cabeçalho IPv4 ---
        buffer.put(IPV4_VERSION_IHL.toByte())
        buffer.put(0) // DSCP/ECN
        buffer.putShort(totalLength.toShort())
        buffer.putShort(0) // identification
        buffer.putShort(0x4000.toShort()) // flags: Don't Fragment
        buffer.put(64) // TTL
        buffer.put(PROTOCOL_UDP.toByte())
        val checksumPosition = buffer.position()
        buffer.putShort(0) // checksum — calculado depois
        buffer.put(query.destIp) // origem da resposta = destino da consulta (o IP virtual local)
        buffer.put(query.sourceIp) // destino da resposta = origem da consulta

        val ipHeader = buffer.array().copyOfRange(0, 20)
        val ipChecksum = computeChecksum(ipHeader)
        buffer.putShort(checksumPosition, ipChecksum)

        // --- Cabeçalho UDP (checksum 0 = não verificado, válido para IPv4 por RFC 768) ---
        buffer.putShort(query.destPort.toShort())
        buffer.putShort(query.sourcePort.toShort())
        buffer.putShort(udpLength.toShort())
        buffer.putShort(0)

        buffer.put(dnsResponsePayload)

        return buffer.array()
    }

    /**
     * Extrai o nome de domínio pedido na seção Question de uma mensagem DNS (formato wire,
     * RFC 1035 §4.1.2 — sequência de labels prefixados por tamanho, terminada em 0x00). Usado
     * pelo [BlockList] para decidir bloquear/permitir. Retorna `null` se a mensagem estiver mal
     * formada — nunca lança, já que dados de rede não confiáveis não devem derrubar o serviço.
     */
    fun extractQueryName(dnsPayload: ByteArray): String? {
        if (dnsPayload.size < 12) return null // cabeçalho DNS tem 12 bytes fixos
        val labels = mutableListOf<String>()
        var pos = 12
        while (pos < dnsPayload.size) {
            val len = dnsPayload[pos].toInt() and 0xFF
            if (len == 0) break
            pos += 1
            if (pos + len > dnsPayload.size) return null
            labels.add(String(dnsPayload, pos, len, Charsets.US_ASCII))
            pos += len
        }
        if (labels.isEmpty()) return null
        return labels.joinToString(".").lowercase()
    }

    /**
     * Monta uma resposta DNS sintética de bloqueio (NXDOMAIN) para a consulta em [dnsQuery],
     * preservando o ID da transação original (exigido pelo cliente para casar pergunta/resposta).
     */
    fun buildBlockedResponse(dnsQuery: ByteArray): ByteArray {
        val response = dnsQuery.copyOf()
        // Flags (bytes 2-3): QR=1 (resposta), Opcode=0, AA=0, TC=0, RD=copiado da pergunta,
        // RA=1, Z=0, RCODE=3 (NXDOMAIN).
        val rd = dnsQuery.getOrElse(2) { 0 }.toInt() and 0x01
        response[2] = (0x80 or rd).toByte()
        response[3] = 0x83.toByte() // RA=1, RCODE=3 (NXDOMAIN)
        // ANCOUNT/NSCOUNT/ARCOUNT = 0 (nenhum registro de resposta) — QDCOUNT (bytes 4-5)
        // permanece o mesmo da pergunta original.
        if (response.size >= 10) {
            response[6] = 0; response[7] = 0
            response[8] = 0; response[9] = 0
            response[10] = 0; response[11] = 0
        }
        return response
    }

    private fun readUShort(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
    }

    private fun computeChecksum(header: ByteArray): Short {
        var sum = 0L
        var i = 0
        while (i < header.size) {
            val word = ((header[i].toInt() and 0xFF) shl 8) or
                (if (i + 1 < header.size) header[i + 1].toInt() and 0xFF else 0)
            sum += word
            i += 2
        }
        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv().toShort()
    }
}
