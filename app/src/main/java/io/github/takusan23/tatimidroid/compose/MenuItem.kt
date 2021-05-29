package io.github.takusan23.tatimidroid.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

/**
 * メニューのボタン一つ一つ
 *
 * @param onClick メニューを押した時。引数は[text]が入る
 * @param painter アイコン
 * @param text 表示させるテキスト
 * */
@Composable
fun MenuItem(text: String, painter: Painter, onClick: (String) -> Unit) {
    TextButton(onClick = { onClick(text) }) {
        Icon(painter = painter, contentDescription = text)
        Text(
            text = text,
            modifier = Modifier
                .weight(1f)
                .padding(10.dp),
        )
    }
}