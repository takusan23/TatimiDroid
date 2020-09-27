package io.github.takusan23.tatimidroid.NicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoPlayListBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoPlayListViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_playlist.*

/**
 * ニコ動連続再生Fragment
 * この上に[NicoVideoFragment]を載せて使う
 *
 * なおこのFragmentは[io.github.takusan23.tatimidroid.NicoVideo.Activity.NicoVideoPlayListActivity.FRAGMENT_TAG]のタグを付けてます。
 * Fragmentを探すときに使って。
 *
 * いれるもの
 *
 * video_list       | [NicoVideoData]の配列         | 連続再生リスト
 * video_id_list    | [NicoVideoData.videoId]の配列 | シャッフルした時に戻せるように、IDだけの配列をください。
 * name             | String                        | タイトル
 * start_id         | String                        | 再生開始位置
 *
 * あと多分[Activity]のテーマ設定が必要だと思われ
 *
 * ```
 *  val darkModeSupport = DarkModeSupport(this)
 *  darkModeSupport.setActivityTheme(this)
 * ```
 *
 * */
class NicoVideoPlayListFragment : Fragment() {

    /** データ置き場。他Fragmentと連携取る時はViewModel使おうね */
    private lateinit var viewModel: NicoVideoPlayListViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel初期化
        viewModel = ViewModelProvider(this).get(NicoVideoPlayListViewModel::class.java)


        // 画面回転復帰後はFragment置かない（誰も教えてくれないAndroid）。つまりここの条件分岐は初回のみ動く
        if (savedInstanceState == null) {
            // データ入れる
            viewModel.playListVideoList.value = arguments?.getSerializable("video_list") as ArrayList<NicoVideoData>
            viewModel.playListVideoIdList.value = arguments?.getStringArrayList("video_id_list")
            viewModel.playListName.value = arguments?.getString("name")
            viewModel.startVideoId.value = arguments?.getString("start_id")

            // JavaBinder: !!! FAILED BINDER TRANSACTION !!!  (parcel size = 1060608) 対策で値をViewModelに移動させたら消す。
            arguments?.clear()

            // Fragment設置
            (viewModel.playListVideoList.value)?.apply {
                val pos = if (viewModel.startVideoId.value != null) {
                    this.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == viewModel.startVideoId.value }
                } else {
                    // 開始位置無指定
                    0
                }
                setVideo(this[pos].videoId, this[pos].isCache)
            }

        }

        // 連続再生動画一覧表示など
        fragment_nicovideo_playlist.setOnClickListener {
            val bottomFragment = NicoVideoPlayListBottomFragment()
            bottomFragment.show(parentFragmentManager, "list")
        }

    }

    /**
     * 動画再生Fragmentをセットする
     * @param videoId 動画ID
     * */
    fun setVideo(videoId: String, isCache: Boolean = false) {
        val nicoVideoFragment = NicoVideoFragment()
        // 動画ID詰めて
        val bundle = Bundle().apply {
            putString("id", videoId)
            putBoolean("cache", isCache)
        }
        nicoVideoFragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_nicovideo_playlist_main, nicoVideoFragment, videoId)
            .commit()
        viewModel.playingVideoId.value = videoId
    }

    /** 次の動画へ切り替える */
    fun nextVideo() {
        val videoList = viewModel.playListVideoList.value ?: return
        // 今の位置
        val pos = getCurrentItemPos() ?: 0
        val nextPos = pos + 1
        if (nextPos < videoList.size) {
            // 動画がある
            setVideo(videoList[nextPos].videoId, videoList[nextPos].isCache)
        } else {
            // 終点。最初から？
            setVideo(videoList[0].videoId, videoList[0].isCache)
        }
    }

    /** 現在再生中の位置を返す */
    fun getCurrentItemPos() = viewModel.playListVideoList.value?.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == viewModel.playingVideoId.value }

    /**
     * Fabを消す関数
     * @param isShow 表示する際はtrue
     * */
    fun setFabVisibility(isShow: Boolean) {
        if (isShow) {
            fragment_nicovideo_playlist?.hide()
        } else {
            fragment_nicovideo_playlist?.show()
        }
    }

    /** Fabが表示されているか */
    fun isShowFab() = fragment_nicovideo_playlist?.isVisible ?: false

}