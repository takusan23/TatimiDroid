package io.github.takusan23.tatimidroid.compose

import android.view.SurfaceView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.coroutines.delay
import java.util.*

/**
 * Composeで使えるExoPlayer
 * @param contentUrl 動画URL
 * */
@Composable
fun ComposeExoPlayer(
    contentUrl: String,
    isPlaying: Boolean = false,
    seek: Long = 0,
    onVideoDuration: (duration: Long) -> Unit = {},
    onUpdate: (currentPos: Long, isPlaying: Boolean) -> Unit,
) {
    // アスペクト比
    val aspectCalc = remember { mutableStateOf(1.7f) }

    val context = LocalContext.current
    val exoPlayer = remember {
        val exoPlayer = SimpleExoPlayer.Builder(context).build()
        // データソース
        val mediaItem = MediaItem.fromUri(contentUrl.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        // 動画情報を取得する
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                // 動画時間をセットする
                onVideoDuration(exoPlayer.duration)
            }
        })
        // アスペクト比
        exoPlayer.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                aspectCalc.value = width.toFloat() / height.toFloat()
            }
        })
        return@remember exoPlayer
    }
    // 100msごとに再生位置を外部に公開
    LaunchedEffect(key1 = onUpdate) {
        while (true) {
            onUpdate.invoke(exoPlayer.currentPosition, exoPlayer.playWhenReady)
            delay(100)
        }
    }
    // 外部の情報を反映
    remember(seek) { exoPlayer.seekTo(seek) }
    remember(isPlaying) { exoPlayer.playWhenReady = isPlaying }
    // SurfaceView
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .aspectRatio(aspectCalc.value), // アスペクト比の設定クソ有能
            factory = { context ->
                SurfaceView(context).apply {
                    exoPlayer.setVideoSurfaceView(this)
                }
            }
        )
    }
    // Composeの世界のonDestroy。
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}