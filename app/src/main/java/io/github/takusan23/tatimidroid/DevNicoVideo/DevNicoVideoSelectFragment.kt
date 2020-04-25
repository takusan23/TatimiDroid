package io.github.takusan23.tatimidroid.DevNicoVideo

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.*
import io.github.takusan23.tatimidroid.isConnectionInternet
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_dev_nicovideo_select.*


class DevNicoVideoSelectFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dev_nicovideo_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ダークモード
        initDarkMode()

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        // モバイルデータ接続のときは常にキャッシュ一覧を開くの設定が有効かどうか
        val isMobileDataShowCacheList =
            prefSetting.getBoolean("setting_mobile_data_show_cache", false)

        // インターネット接続確認
        when {
            // App Shortcutで起動したのでキャッシュを開く
            arguments?.getBoolean("cache", false) ?: false -> {
                setFragment(DevNicoVideoCacheFragment())
            }
            // モバイルデータでWi-Fiは未接続
            isMobileDataShowCacheList -> {
                if (isConnectionMobileDataInternet(context)) {
                    // モバイルデータ接続のときは常にキャッシュ一覧を開くの設定が有効のとき
                    setFragment(DevNicoVideoCacheFragment())
                } else {
                    // ランキング
                    setFragment(DevNicoVideoRankingFragment())
                }
            }
            // 何らかの方法でインターネットにはつながっている
            isConnectionInternet(context) -> {
                // ランキング
                setFragment(DevNicoVideoRankingFragment())
            }
            // インターネットに繋がっていない。自分だけどこか取り残された
            else -> {
                // キャッシュ一覧。インターネット接続無いとき
                fragment_nicovideo_ranking.visibility = View.GONE
                fragment_nicovideo_post.visibility = View.GONE
                fragment_nicovideo_mylist.visibility = View.GONE
                fragment_nicovideo_search.visibility = View.GONE
                fragment_nicovideo_history.visibility = View.GONE
                fragment_nicovideo_nicorepo.visibility = View.GONE
                setFragment(DevNicoVideoCacheFragment())
                // インターネット接続無いよメッセージ
                Snackbar.make(fragment_nicovideo_ranking, R.string.internet_not_connection_cache, Snackbar.LENGTH_SHORT)
                    .apply {
                        anchorView = (activity as MainActivity).main_activity_bottom_navigationview
                        show()
                    }
            }
        }

        // 未ログインで利用する場合
        if (isNotLoginMode(context)) {
            // ログインが必要なやつを非表示に
            fragment_nicovideo_post.visibility = View.GONE
            fragment_nicovideo_mylist.visibility = View.GONE
            fragment_nicovideo_history.visibility = View.GONE
            fragment_nicovideo_nicorepo.visibility = View.GONE
        }

        fragment_nicovideo_ranking.setOnClickListener {
            setFragment(DevNicoVideoRankingFragment())
        }

        fragment_nicovideo_post.setOnClickListener {
            setFragment(DevNicoVideoPOSTFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("my", true)
                }
            })
        }

        fragment_nicovideo_mylist.setOnClickListener {
            setFragment(DevNicoVideoMyListFragment())
        }

        fragment_nicovideo_history.setOnClickListener {
            setFragment(DevNicoVideoHistoryFragment())
        }

        fragment_nicovideo_search.setOnClickListener {
            setFragment(DevNicoVideoSearchFragment())
        }

        fragment_nicovideo_cache.setOnClickListener {
            setFragment(DevNicoVideoCacheFragment())
        }

        fragment_nicovideo_nicorepo.setOnClickListener {
            setFragment(DevNicoVideoNicoRepoFragment())
        }

    }

    private fun initDarkMode() {
        val darkModeSupport = DarkModeSupport(context!!)
        fragment_video_list_linearlayout.background = ColorDrawable(darkModeSupport.getThemeColor())
        fragment_video_bar?.background = ColorDrawable(darkModeSupport.getThemeColor())
    }

    fun setFragment(fragment: Fragment) {
        // Handler(UIスレッド指定)で実行するとダークモード、画面切り替えに耐えるアプリが作れる。
        Handler(Looper.getMainLooper()).post {
            if (fragment_video_motionlayout != null) {
                fragment_video_motionlayout.transitionToStart()
            }
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(fragment_video_list_linearlayout.id, fragment)?.commit()
        }
    }

/*
    */
    /**
     * ネットワーク接続確認
     * https://stackoverflow.com/questions/57277759/getactivenetworkinfo-is-deprecated-in-api-29
     * *//*

    fun isConnectionInternet(): Boolean {
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
*/

}