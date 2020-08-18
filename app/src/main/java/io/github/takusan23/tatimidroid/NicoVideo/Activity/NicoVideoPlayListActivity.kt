package io.github.takusan23.tatimidroid.NicoVideo.Activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoPlayListFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.Tool.LanguageTool

/**
 * [NicoVideoPlayListFragment]を置くためのActivity
 *
 * 入れてほしいもの
 * video_list    | NicoVideoDataの配列 | 動画リストです
 * 任意
 * name          | String              |一覧名。無くてもいい
 * */
class NicoVideoPlayListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_nico_video_play_list)

        supportActionBar?.hide()

        // Fragment設置
        if (savedInstanceState == null) {
            val nicoVideoFragment = NicoVideoPlayListFragment()
            val videoList = intent.getSerializableExtra("video_list") as ArrayList<NicoVideoData>
            val bundle = Bundle().apply {
                putSerializable("video_list", videoList)
                putStringArrayList("video_id_list", ArrayList(videoList.map { nicoVideoData -> nicoVideoData.videoId }))
                putSerializable("name", intent.getStringExtra("name"))
            }
            nicoVideoFragment.arguments = bundle
            supportFragmentManager.beginTransaction().replace(R.id.nicovideo_playlist_activity_main, nicoVideoFragment, FRAGMENT_TAG).commit()
        }

    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }

    companion object {
        /** このFragmentを置くときに付けたタグ */
        const val FRAGMENT_TAG = "playlist_fragment"
    }

}