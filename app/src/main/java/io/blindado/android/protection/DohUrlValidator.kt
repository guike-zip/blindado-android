package io.blindado.android.protection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Valida uma URL DoH personalizada (HTTPS + alcançável) antes de salvar (FR-007). */
class DohUrlValidator {

    sealed interface Result {
        data object Valid : Result
        data class Invalid(val reason: String) : Result
    }

    suspend fun validate(url: String): Result = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://")) {
            return@withContext Result.Invalid("O endereço precisa começar com https://")
        }

        val parsed = try {
            URL(url)
        } catch (e: Exception) {
            return@withContext Result.Invalid("Endereço inválido: ${e.message}")
        }

        try {
            val connection = parsed.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.connect()
            val reachable = connection.responseCode in 200..499 // qualquer resposta HTTP real
            connection.disconnect()
            if (reachable) Result.Valid else Result.Invalid("Servidor não respondeu corretamente")
        } catch (e: Exception) {
            Result.Invalid("Não foi possível alcançar o servidor: ${e.message}")
        }
    }
}
