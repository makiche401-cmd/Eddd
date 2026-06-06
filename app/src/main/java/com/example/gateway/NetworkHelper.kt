package com.example.gateway

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object NetworkHelper {
    suspend fun hasRealInternetConnection(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val hasTransport = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            } else {
                @Suppress("DEPRECATION")
                val info = cm.activeNetworkInfo
                info != null && info.isConnected
            }

            if (!hasTransport) return@withContext false

            // Attempt DNS resolution for google.com
            var resolved = false
            try {
                val address = InetAddress.getByName("google.com")
                resolved = address.hostAddress != null && address.hostAddress.isNotEmpty()
            } catch (e: Exception) {
                // Resolution exception, fallback
            }

            if (resolved) {
                return@withContext true
            }

            // Fallback socket check on port 53 (Google DNS)
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                socket.close()
                true
            } catch (e: Exception) {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress("1.1.1.1", 53), 1500)
                    socket.close()
                    true
                } catch (ex: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}
