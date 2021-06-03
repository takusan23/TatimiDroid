package io.github.takusan23.tatimidroid.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 読み込み中は代替UIを表示するするImage
 *
 * @param imageBitmap 表示する画像
 * @param modifier Modifier
 * @param placeHolder 読み込み中のときに表示するUI。初期時はグレーの背景をセットしたBox。別にProgressでもいい
 * */
@Composable
fun PlaceholderImage(
    modifier: Modifier,
    isLoading: Boolean,
    imageBitmap: ImageBitmap?,
    placeHolder: @Composable BoxScope.() -> Unit = { Box(modifier = Modifier.background(Color.Gray)) }
) {
    Box(modifier = modifier) {
        if (isLoading || imageBitmap == null) {
            placeHolder()
        } else {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}