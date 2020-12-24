package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * [NicoVideoUploadVideoViewModel]を初期化するくらす
 * */
class NicoVideoUploadVideoViewModelFactory(val application: Application, val userId: String?) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoVideoUploadVideoViewModel(application, userId) as T
    }

}