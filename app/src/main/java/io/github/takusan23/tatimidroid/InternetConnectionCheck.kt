package io.github.takusan23.tatimidroid

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * インターネットに接続できるか。接続してればtrue
 * */
internal fun isConnectionInternet(context: Context?): Boolean {
    val connectivityManager =
        context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10時代のネットワーク接続チェック
        val network = connectivityManager?.activeNetwork
        val networkCapabilities = connectivityManager?.getNetworkCapabilities(network)
        return when {
            networkCapabilities == null -> false
            // Wi-Fi / MobileData / EtherNet / Bluetooth のどれかでつながっているか
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    } else {
        // 今までのネットワーク接続チェック
        return connectivityManager?.activeNetworkInfo != null && connectivityManager.activeNetworkInfo.isConnected
    }
}