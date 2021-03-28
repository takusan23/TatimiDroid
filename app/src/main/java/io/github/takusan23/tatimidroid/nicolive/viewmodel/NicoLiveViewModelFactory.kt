package io.github.takusan23.tatimidroid.nicolive.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * [NicoLiveViewModel]は生放送ID（とかいろいろ）を引数にほしいので独自に用意
 * */
class NicoLiveViewModelFactory(val application: Application, val liveId: String, val isLoginMode: Boolean) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoLiveViewModel(application, liveId, isLoginMode) as T
    }

}