package io.blindado.android.protection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import javax.net.ssl.HttpsURLConnection

/**
 * Cliente DoH (RFC 8484) via `HttpsURLConnection` nativo — sem OkHttp/Retrofit (Constituição,
 * Princípio II). POST com corpo binário `application/dns-message` (research.md #4).
 */
class DohResolver {

    class DohException(message: String, cause: Throwable? = null) : Exception(message, cause)

    /**
     * Envia [dnsQuery] (mensagem DNS em wire format) ao [dohEndpoint] e retorna a resposta em
     * wire format, ou lança [DohException] com a causa real — nunca um motivo inventado.
     */
    suspend fun resolve(dohEndpoint: String, dnsQuery: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        val connection: URLConnection = URL(dohEndpoint).openConnection()
        val httpsConnection = connection as? HttpsURLConnection
            ?: throw DohException("Endpoint DoH não é HTTPS: $dohEndpoint")

        try {
            httpsConnection.requestMethod = "POST"
            httpsConnection.doOutput = true
            httpsConnection.setRequestProperty("Content-Type", "application/dns-message")
            httpsConnection.setRequestProperty("Accept", "application/dns-message")
            httpsConnection.connectTimeout = 5_000
            httpsConnection.readTimeout = 5_000

            httpsConnection.outputStream.use { it.write(dnsQuery) }

            val status = httpsConnection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                throw DohException("DoH retornou HTTP $status para $dohEndpoint")
            }

            httpsConnection.inputStream.use { it.readBytes() }
        } catch (e: DohException) {
            throw e
        } catch (e: Exception) {
            throw DohException("Falha ao resolver via DoH ($dohEndpoint): ${e.message}", e)
        } finally {
            httpsConnection.disconnect()
        }
    }
}
