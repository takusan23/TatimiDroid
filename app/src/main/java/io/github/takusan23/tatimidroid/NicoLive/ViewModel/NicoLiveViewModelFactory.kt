package io.github.takusan23.tatimidroid.NicoLive.ViewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel

/**
 * [NicoVideoViewModel]は動画IDを引数に欲しいので独自のファクトリークラス？を作成する
 * */
class NicoLiveViewModelFactory(val application: Application, val videoId: String, val isCache: Boolean, val isEco:Boolean) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoLiveViewModel(application, videoId, isCache,isEco) as T
    }

}