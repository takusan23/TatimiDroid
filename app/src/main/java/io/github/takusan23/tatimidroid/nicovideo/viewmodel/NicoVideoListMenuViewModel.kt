package io.github.takusan23.tatimidroid.nicovideo.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R

/**
 * ニコ動一覧のメニューで使うViewModel
 * */
class NicoVideoListMenuViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    /** 動画IDコピー */
    fun copyVideoId(videoId: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(videoId, videoId))
        showToast("${getString(R.string.video_id_copy)}：$videoId")
    }

    /** Context.getStringを短く */
    private fun getString(resourceId: Int): String {
        return context.getString(resourceId)
    }

    private fun showToast(s: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
        }
    }

}