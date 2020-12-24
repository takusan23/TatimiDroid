package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData

/**
 * [NicoAccountViewModel]を初期化するFactoryクラス
 * */
class NicoAccountViewModelFactory(val application: Application, val userId: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoAccountViewModel(application, userId) as T
    }

}