package io.github.takusan23.tatimidroid.NicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.FregmentData.NicoVideoPlayListFragmentData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoPlayListBottomFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_playlist.*

/**
 * ニコ動連続再生Fragment
 * この上に[NicoVideoFragment]を載せて使う
 *
 * なおこのFragmentは[io.github.takusan23.tatimidroid.NicoVideo.Activity.NicoVideoPlayListActivity.FRAGMENT_TAG]のタグを付けてます。
 * Fragmentを探すときに使って。
 * */
class NicoVideoPlayListFragment : Fragment() {

    /** 動画リスト */
    val videoList by lazy { arguments?.getSerializable("video_list") as ArrayList<NicoVideoData> }

    /** 今再生中の動画 */
    var currentVideoId = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 画面回転復帰後
        if (savedInstanceState != null) {
            val data = savedInstanceState.getSerializable("data") as NicoVideoPlayListFragmentData
            currentVideoId = data.currentVideoId
        }

        // Fragment設置
        setVideo(videoList[0].videoId)

        // 動画一覧表示など
        fragment_nicovideo_playlist.setOnClickListener {
            val bottomFragment = NicoVideoPlayListBottomFragment()
            bottomFragment.show(parentFragmentManager, "list")
        }

    }

    /**
     * 動画再生Fragmentをセットする
     * @param videoId 動画ID
     * */
    fun setVideo(videoId: String) {
        val nicoVideoFragment = NicoVideoFragment()
        // 動画ID詰めて
        val bundle = Bundle().apply {
            putString("id", videoId)
        }
        nicoVideoFragment.arguments = bundle
        parentFragmentManager.beginTransaction().replace(R.id.fragment_nicovideo_playlist_main, nicoVideoFragment, videoId).commit()
        currentVideoId = videoId
    }

    /**
     * 次の動画へ切り替える
     * */
    fun nextVideo() {
        // 今の位置
        val pos = videoList.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == currentVideoId }
        val nextPos = pos + 1
        if (nextPos < videoList.size) {
            // 動画がある
            setVideo(videoList[nextPos].videoId)
        } else {
            // 終点。最初から？
            setVideo(videoList[0].videoId)
        }
    }

    /** 画面回転後もデータを保持しておく。動画一覧はFragmentの[setArguments]を使っていれば勝手に復元してくれる */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val data = NicoVideoPlayListFragmentData(currentVideoId)
        outState.putSerializable("data", data)
    }

}