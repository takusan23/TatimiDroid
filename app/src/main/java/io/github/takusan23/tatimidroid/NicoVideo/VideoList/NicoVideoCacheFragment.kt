package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.content.ComponentName
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
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheJSON
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoCacheFilterBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoCacheFragmentViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.BackgroundPlaylistCachePlayService
import io.github.takusan23.tatimidroid.databinding.FragmentNicovideoCacheBinding
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NicoVideoCacheFragment : Fragment() {

    // 必要なやつ
    lateinit var prefSetting: SharedPreferences
    lateinit var nicoVideoListAdapter: NicoVideoListAdapter

    /** バックグラウンド連続再生のMediaSessionへ接続する */
    var mediaBrowser: MediaBrowserCompat? = null

    /** ViewModel。画面回転時に再読み込みされるのつらいので */
    private val viewModel by viewModels<NicoVideoCacheFragmentViewModel>({ this })

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoCacheBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // RecyclerView
        viewModel.recyclerViewList.observe(viewLifecycleOwner) { list ->
            initRecyclerView(list)
            // 中身0だった場合
            viewBinding.fragmentNicovideoCacheEmptyMessageTextView.isVisible = list.isEmpty()
        }

        // 合計容量
        viewModel.totalUsedStorageGB.observe(viewLifecycleOwner) { gb ->
            viewBinding.fragmentNicovideoCacheStorageInfoTextView.text = "${getString(R.string.cache_usage)}：$gb GB"
        }


        // フィルター / 並び替え BottomFragment
        viewBinding.fragmentNicovideoCacheMenuFilterTextview.setOnClickListener {
            val cacheFilterBottomFragment = NicoVideoCacheFilterBottomFragment()
            cacheFilterBottomFragment.show(childFragmentManager, "filter")
            viewBinding.fragmentNicovideoCacheCardMotionLayout.transitionToStart()
        }

        viewBinding.fragmentNicovideoCacheMenuPlaylistTextview.setOnClickListener {
            // 連続再生
            if (viewModel.recyclerViewList.value != null) {
                val nicoVideoFragment = NicoVideoFragment()
                nicoVideoFragment.arguments = Bundle().apply {
                    putSerializable("video_list", viewModel.recyclerViewList.value) // BundleでNicoVideoListAdapterから渡してもらった
                }
                (requireActivity() as MainActivity).setPlayer(nicoVideoFragment, viewModel.recyclerViewList.value?.get(0)?.videoId ?: "")
            }
            // メニュー閉じる
            viewBinding.fragmentNicovideoCacheCardMotionLayout.transitionToStart()
        }

        // バックグラウンド連続再生
        viewBinding.fragmentNicovideoCacheMenuBackgroundTextview.setOnClickListener {
            lifecycleScope.launch {
                connectMediaSession()
                // このActivityに関連付けられたMediaSessionControllerを取得
                val controller = MediaControllerCompat.getMediaController(requireActivity())
                // 最後に再生した曲を なければ配列の最初。それもなければ再生しない
                val videoId = prefSetting.getString("cache_last_play_video_id", null) ?: viewModel.recyclerViewList.value?.first()?.videoId
                if (videoId != null) {
                    controller.transportControls.playFromMediaId(videoId, null)
                }
                viewBinding.fragmentNicovideoCacheCardMotionLayout.transitionToStart()
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
        Snackbar.make(viewBinding.fragmentNicovideoCacheEmptyMessageTextView, getString(R.string.filter_clear_message), Snackbar.LENGTH_SHORT).apply {
            setAction(getString(R.string.reset)) {
                CacheJSON().deleteFilterJSONFile(context)
            }
            anchorView = viewBinding.fragmentNicovideoCacheFab
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
        viewBinding.fragmentNicovideoCacheRecyclerView.apply {
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