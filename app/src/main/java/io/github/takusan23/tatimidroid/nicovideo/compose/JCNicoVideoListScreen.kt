package io.github.takusan23.tatimidroid.nicovideo.compose

import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import io.github.takusan23.tatimidroid.databinding.FragmentNicovideoListBinding

/**
 * ニコ動の動画一覧表示画面。Composeでできている
 * */
@Composable
fun JCNicoVideoListScreen() {
    AndroidView(
        factory = { context -> FragmentNicovideoListBinding.inflate(LayoutInflater.from(context)).root }
    )
}