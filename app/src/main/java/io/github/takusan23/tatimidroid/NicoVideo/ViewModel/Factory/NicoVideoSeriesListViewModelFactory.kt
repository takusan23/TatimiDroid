package io.github.takusan23.tatimidroid.NicoVideo.ViewModel.Factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoSeriesListViewModel

/**
 * [NicoVideoSeriesListViewModel]を初期化するクラス
 * */
class NicoVideoSeriesListViewModelFactory(val application: Application, val userId: String?) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoVideoSeriesListViewModel(application, userId) as T
    }

}