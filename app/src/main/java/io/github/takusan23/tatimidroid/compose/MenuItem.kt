package io.github.takusan23.tatimidroid.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

/**
 * メニューのボタン一つ一つ
 *
 * @param onClick メニューを押した時。引数は[text]が入る
 * @param painter アイコン
 * @param text 表示させるテキスト
 * @param isSelected 選択中の場合はtrue
 * */
@Composable
fun MenuItem(
    text: String,
    painter: Painter,
    isSelected: Boolean,
    onClick: (String) -> Unit
) {
    val colors = MaterialTheme.colors
    val textColor = if (isSelected) colors.primary else colors.onSurface.copy(alpha = 0.6f)
    val backgroundColor = if (isSelected) colors.primary.copy(alpha = 0.1f) else Color.Transparent
    Surface(
        color = backgroundColor,
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(5.dp),
        contentColor = textColor,
    ) {
        TextButton(onClick = { onClick(text) }) {
            Icon(
                painter = painter,
                contentDescription = text,
                tint = textColor,
            )
            Text(
                text = text,
                modifier = Modifier
                    .padding(5.dp)
                    .weight(1f),
                color = textColor
            )
        }
    }
}