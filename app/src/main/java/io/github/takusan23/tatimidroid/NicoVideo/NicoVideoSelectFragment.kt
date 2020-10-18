package io.github.takusan23.tatimidroid.NicoVideo

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
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.*
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
class NicoVideoSelectFragment : Fragment() {

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
                setFragment(NicoVideoRankingFragment())
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
                R.id.nicovideo_select_menu_ranking -> setFragment(NicoVideoRankingFragment())
                R.id.nicovideo_select_menu_mylist -> setFragment(NicoVideoMyListFragment())
                R.id.nicovideo_select_menu_history -> setFragment(NicoVideoHistoryFragment())
                R.id.nicovideo_select_menu_search -> setFragment(NicoVideoSearchFragment())
                R.id.nicovideo_select_menu_nicorepo -> setFragment(NicoVideoNicoRepoFragment())
                R.id.nicovideo_select_menu_post -> {
                    setFragment(NicoVideoPOSTFragment().apply {
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
        // fragment_video_bar?.background = ColorDrawable(getThemeColor(darkModeSupport.context))
    }

    fun setFragment(fragment: Fragment) {
        // Handler(UIスレッド指定)で実行するとダークモード、画面切り替えに耐えるアプリが作れる。
        Handler(Looper.getMainLooper()).post {
            fragment_video_motionlayout?.transitionToStart()
            activity?.supportFragmentManager?.beginTransaction()?.replace(fragment_video_list_linearlayout.id, fragment)?.commit()
        }
    }

}