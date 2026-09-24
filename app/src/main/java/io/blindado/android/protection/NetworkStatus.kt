package io.blindado.android.protection

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Checagem de conectividade real (FR-012 — nunca inferir Protegido/Desprotegido sem rede).
 * Interface para permitir uma fake de teste sem `ConnectivityManager` real (Princípio V).
 */
interface ConnectivityChecking {
    fun hasActiveConnection(): Boolean
}

class NetworkStatus(private val context: Context) : ConnectivityChecking {

    override fun hasActiveConnection(): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
