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

/**
 * モバイルデータ接続かどうかを返す関数。モバイルデータ接続の場合はtrue
 * */
internal fun isConnectionMobileDataInternet(context: Context?): Boolean {
    //今の接続状態を取得
    val connectivityManager =
        context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    //ろりぽっぷとましゅまろ以上で分岐
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
            //モバイルデータ通信なら画質変更メッセージ送信
            return true
        }
    } else {
        if (connectivityManager.activeNetworkInfo.type == ConnectivityManager.TYPE_MOBILE) {
            //モバイルデータ通信なら画質変更メッセージ送信
            return true
        }
    }
    return false
}