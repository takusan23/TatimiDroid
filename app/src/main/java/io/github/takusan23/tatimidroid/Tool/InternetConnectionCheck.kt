package io.github.takusan23.tatimidroid.Tool

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * インターネットに接続できるか。接続してればtrue
 * */
fun isConnectionInternet(context: Context?): Boolean {
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
        return connectivityManager?.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnected
    }
}

/**
 * モバイルデータ接続かどうかを返す関数。モバイルデータ接続の場合はtrue
 * */
fun isConnectionMobileDataInternet(context: Context?): Boolean {
    //今の接続状態を取得
    val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    //ろりぽっぷとましゅまろ以上で分岐
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
            return true
        }
    } else {
        if (connectivityManager.activeNetworkInfo!!.type == ConnectivityManager.TYPE_MOBILE) {
            return true
        }
    }
    return false
}

/**
 * Wi-Fiでネットワークに接続している場合はtrueを返す
 * @param context コンテキスト
 * @return Wi-Fi接続時ならtrue
 * */
fun isConnectionWiFiInternet(context: Context?): Boolean {
    //今の接続状態を取得
    val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    //ろりぽっぷとましゅまろ以上で分岐
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    } else {
        connectivityManager.activeNetworkInfo!!.type == ConnectivityManager.TYPE_WIFI
    }
}


/**
 * 公式ドキュメント眺めてたら面白そうなもの発見した。Android 6以上で利用できる。
 *
 * インターネット接続が定額制（一度払えば使い放題。固定回線みたいな）か従量制（使った分だけ払う。パケ死ってやつですか）かどうかを判断する
 *
 * 私の環境ではWi-Fiのときはtrue(定額制設定)。モバイルデータ時はfalseだった。5Gの使い放題とかはどっちが返ってくるのか気になるわ
 *
 * @param context コンテキスト
 * @return 定額制ネットワークならtrue。そうじゃないならfalse
 * */
@RequiresApi(Build.VERSION_CODES.M)
fun isConnectionNetworkTypeUnlimited(context: Context?): Boolean {
    val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
    return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) || networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}
