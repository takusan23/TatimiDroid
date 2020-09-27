package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData

/**
 * ニコ動連続再生用ViewModel。MVVMってやつ。データ保持担当さん。Activityはデータ表示担当
 *
 * スコープは[io.github.takusan23.tatimidroid.NicoVideo.NicoVideoPlayListFragment]（Activityではない）
 *
 * */
class NicoVideoPlayListViewModel(application: Application) : AndroidViewModel(application) {

    /** 連続再生一覧 */
    val playListVideoList = MutableLiveData<ArrayList<NicoVideoData>>()

    /** 連続再生の動画だけの配列 */
    val playListVideoIdList = MutableLiveData<ArrayList<String>>()

    /** 連続再生の名前。マイリスト名とか？ */
    val playListName = MutableLiveData<String>()

    /** 現在再生中の動画ID */
    val playingVideoId = MutableLiveData<String>()

    /** シャッフル再生が有効？ */
    val isEnableShuffleMode = MutableLiveData<Boolean>()

    /** 逆順が有効の場合 */
    val isReverseMode = MutableLiveData<Boolean>()

    /** 動画再生開始位置。動画IDで */
    val startVideoId = MutableLiveData<String>()

}
