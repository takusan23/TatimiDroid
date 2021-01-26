package io.github.takusan23.tatimidroid.NicoAd

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel

/**
 * [NicoLiveViewModel]は生放送ID（とかいろいろ）を引数にほしいので独自に用意
 * */
class NicoAdViewModelFactory(val application: Application, val contentId: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoAdViewModel(application, contentId) as T
    }

}