package io.github.takusan23.tatimidroid.NicoLive.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/**
 * [io.github.takusan23.tatimidroid.NicoLive.CommentFragment]のViewModel
 * */
class NicoLiveViewModel(application: Application, val videoId: String, val isCache: Boolean, val isEco: Boolean) : AndroidViewModel(application) {

}