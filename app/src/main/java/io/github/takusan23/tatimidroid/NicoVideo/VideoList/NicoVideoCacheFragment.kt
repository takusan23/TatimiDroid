package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheJSON
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.Activity.NicoVideoPlayListActivity
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoCacheFilterBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoCacheFragmentViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.BackgroundPlaylistCachePlayService
import kotlinx.android.synthetic.main.fragment_comment_cache.*
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NicoVideoCacheFragment : Fragment() {

    // 必要なやつ
    lateinit var prefSetting: SharedPreferences
    lateinit var nicoVideoListAdapter: NicoVideoListAdapter

    /** バックグラウンド連続再生のMediaSessionへ接続する */
    var mediaBrowser: MediaBrowserCompat? = null

    /** ViewModel。画面回転時に再読み込みされるのつらい */
    private val viewModel by viewModels<NicoVideoCacheFragmentViewModel>({ this })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_comment_cache, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // RecyclerView
        viewModel.recyclerViewList.observe(viewLifecycleOwner) { list ->
            initRecyclerView(list)
            // 中身0だった場合
            fragment_cache_empty_message.isVisible = list.isEmpty()
        }

        // 合計容量
        viewModel.totalUsedStorageGB.observe(viewLifecycleOwner) { gb ->
            fragment_cache_storage_info.text = "${getString(R.string.cache_usage)}：$gb GB"
        }


        // フィルター / 並び替え BottomFragment
        fragment_cache_menu_filter_textview.setOnClickListener {
            val cacheFilterBottomFragment = NicoVideoCacheFilterBottomFragment()
            cacheFilterBottomFragment.show(childFragmentManager, "filter")
            fragment_cache_card_motionlayout.transitionToStart()
        }

        // 連続再生
        fragment_cache_menu_playlist_textview.setOnClickListener {
            val intent = Intent(requireContext(), NicoVideoPlayListActivity::class.java)
            if (viewModel.recyclerViewList.value != null) {
                // 中身を入れる
                intent.putExtra("video_list", viewModel.recyclerViewList.value)
                intent.putExtra("name", getString(R.string.cache))
                startActivity(intent)
            }
            fragment_cache_card_motionlayout.transitionToStart()
        }

        // バックグラウンド連続再生
        fragment_cache_menu_background_textview.setOnClickListener {
            lifecycleScope.launch {
                connectMediaSession()
                // このActivityに関連付けられたMediaSessionControllerを取得
                val controller = MediaControllerCompat.getMediaController(requireActivity())
                // 最後に再生した曲を
                val videoId = prefSetting.getString("cache_last_play_video_id", "")
                controller.transportControls.playFromMediaId(videoId, null)
                fragment_cache_card_motionlayout.transitionToStart()
            }
        }

    }

    /**
     * バックグラウンド連続再生のMediaBrowserService（音楽再生サービス）（[BackgroundPlaylistCachePlayService]）へ接続する関数
     * コルーチンで使えるようにした。
     * */
    private suspend fun connectMediaSession() = suspendCoroutine<Unit> {
        val callback = object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                super.onConnected()
                if (mediaBrowser != null) {
                    // MediaSession経由で操作するやつ
                    val mediaControllerCompat = MediaControllerCompat(requireContext(), mediaBrowser!!.sessionToken)
                    // Activityと関連付けることで、同じActivityなら操作ができる？（要検証）
                    MediaControllerCompat.setMediaController(requireActivity(), mediaControllerCompat)
                    it.resume(Unit)
                }
            }
        }
        mediaBrowser = MediaBrowserCompat(requireContext(), ComponentName(requireContext(), BackgroundPlaylistCachePlayService::class.java), callback, null)
        // 忘れてた
        mediaBrowser?.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaBrowser?.disconnect()
    }

    // フィルター削除関数
    fun filterDeleteMessageShow() {
        // 本当に消していい？
        Snackbar.make(fragment_cache_empty_message, getString(R.string.filter_clear_message), Snackbar.LENGTH_SHORT).apply {
            setAction(getString(R.string.reset)) {
                CacheJSON().deleteFilterJSONFile(context)
            }
            anchorView = fragment_cache_fab
            show()
        }
    }

    /**
     * RecyclerView初期化
     * @param list NicoVideoDataの配列。RecyclerViewに表示させたい配列が別にある時だけ指定すればいいと思うよ
     * @param layoutManagerParcelable RecyclerViewのスクロール位置を復元できるらしい。RecyclerView#layoutManager#onSaveInstanceState()で取れる
     * https://stackoverflow.com/questions/27816217/how-to-save-recyclerviews-scroll-position-using-recyclerview-state
     * */
    fun initRecyclerView(list: ArrayList<NicoVideoData>, layoutManagerParcelable: Parcelable? = null) {
        fragment_cache_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            if (layoutManagerParcelable != null) {
                layoutManager?.onRestoreInstanceState(layoutManagerParcelable)
            }
            nicoVideoListAdapter = NicoVideoListAdapter(list)
            adapter = nicoVideoListAdapter
        }
    }

}