package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * [NicoVideoViewModel]は動画IDを引数に欲しいので独自のファクトリークラス？を作成する
 * */
class NicoVideoViewModelFactory(val application: Application, val videoId: String, val isCache: Boolean, val isEco: Boolean, val useInternet: Boolean, val startFullScreen: Boolean) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoVideoViewModel(application, videoId, isCache, isEco, useInternet,startFullScreen) as T
    }

}