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
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.*
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.Tool.isConnectionInternet
import io.github.takusan23.tatimidroid.Tool.isNotLoginMode
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