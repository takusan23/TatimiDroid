package io.github.takusan23.tatimidroid.compose

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.viewinterop.AndroidView
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.ReCommentCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ReCommentCanvasをComposeで。略してCCC
 *
 * @param commentList コメント一覧
 * @param currentPosition 再生位置
 * @param isPlaying 再生状態
 * @param videoDuration 動画時間
 * */
@Composable
fun ComposeCommentCanvas(
    commentList: List<CommentJSONParse>,
    currentPosition: Long,
    isPlaying: Boolean,
    videoDuration: Long,
) {
    val scope = rememberCoroutineScope()

    AndroidView(
        factory = {
            println("初期化")
            ReCommentCanvas(it, null).apply {
                initCommentList(commentList, videoDuration)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        },
        update = {
            scope.launch {
                while (isActive) {
                    it.currentPos = currentPosition
                    it.isPlaying = isPlaying
                    delay(100)
                }
            }
        }
    )
}
