package io.github.takusan23.tatimidroid.nicolive.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveProgramListMenuViewModel

/**
 * 番組一覧メニューViewModelの初期化でつかう
 * */
class NicoLiveProgramListMenuViewModelFactory(val application: Application, val liveId: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoLiveProgramListMenuViewModel(application, liveId) as T
    }

}