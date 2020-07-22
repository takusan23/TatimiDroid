package io.github.takusan23.tatimidroid.DevNicoVideo

import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.*
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import io.github.takusan23.tatimidroid.Tool.isConnectionInternet
import io.github.takusan23.tatimidroid.Tool.isNotLoginMode
import kotlinx.android.synthetic.main.fragment_dev_nicovideo_select.*

/**
 * ランキング、マイリスト等を表示するFragmentを乗せるFragment。
 * BottonNavBar押した時に切り替わるFragmentはこれ
 * */
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

        // インターネット接続確認
        // 何らかの方法でインターネットにはつながっている
        if (isConnectionInternet(context)) {
            // とりあえずランキング
            if (savedInstanceState == null) {
                // 画面回転時に回転前のFragmentをそのまま残しておくにはsavedInstanceStateがnullのときだけFragmentを生成する必要がある。
                setFragment(DevNicoVideoRankingFragment())
            }
        }

        // 未ログインで利用する場合
        if (isNotLoginMode(context)) {
            // ログインが必要なやつを非表示に
            fragment_nicovideo_select_menu.menu.findItem(R.id.nicovideo_select_menu_post).isVisible = false
            fragment_nicovideo_select_menu.menu.findItem(R.id.nicovideo_select_menu_mylist).isVisible = false
            fragment_nicovideo_select_menu.menu.findItem(R.id.nicovideo_select_menu_history).isVisible = false
            fragment_nicovideo_select_menu.menu.findItem(R.id.nicovideo_select_menu_nicorepo).isVisible = false
        }

        // メニュー押したとき
        fragment_nicovideo_select_menu.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nicovideo_select_menu_ranking -> setFragment(DevNicoVideoRankingFragment())
                R.id.nicovideo_select_menu_mylist -> setFragment(DevNicoVideoMyListFragment())
                R.id.nicovideo_select_menu_history -> setFragment(DevNicoVideoHistoryFragment())
                R.id.nicovideo_select_menu_search -> setFragment(DevNicoVideoSearchFragment())
                R.id.nicovideo_select_menu_nicorepo -> setFragment(DevNicoVideoNicoRepoFragment())
                R.id.nicovideo_select_menu_post -> {
                    setFragment(DevNicoVideoPOSTFragment().apply {
                        arguments = Bundle().apply {
                            putBoolean("my", true)
                        }
                    })
                }
            }
            true
        }

    }

    private fun initDarkMode() {
        val darkModeSupport = DarkModeSupport(requireContext())
        fragment_video_list_linearlayout.background = ColorDrawable(getThemeColor(darkModeSupport.context))
        fragment_video_bar?.background = ColorDrawable(getThemeColor(darkModeSupport.context))
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