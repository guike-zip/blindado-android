package io.blindado.android.protection

import android.content.Context
import io.blindado.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Carrega `res/raw/blocklist.txt` (research.md #5 — lista estática embutida, sem download
 * remoto nesta v1) e expõe checagem de domínio bloqueado por sufixo: `ads.example.com` é
 * bloqueado se `example.com` (ou o próprio `ads.example.com`) estiver na lista.
 */
class BlockList {
    private var domains: Set<String> = emptySet()

    suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        domains = context.resources.openRawResource(R.raw.blocklist).bufferedReader().useLines { lines ->
            lines
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toSet()
        }
    }

    fun isBlocked(domain: String): Boolean {
        val normalized = domain.trim().lowercase().removeSuffix(".")
        if (normalized in domains) return true
        var suffix = normalized
        while (true) {
            val dotIndex = suffix.indexOf('.')
            if (dotIndex < 0) break
            suffix = suffix.substring(dotIndex + 1)
            if (suffix in domains) return true
        }
        return false
    }
}
