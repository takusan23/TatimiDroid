package io.github.takusan23.tatimidroid.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material DesignのChip。Composeにはないので作る
 *
 * [content]にはPaddingをかけてないので各自かけてください。
 *
 * @param modifier Modifier
 * @param boarderColor 枠の色
 * @param content Chipの中身
 * @param cornerShape 角どれだけ丸くするか
 * @param elevation 影
 * */
@Composable
fun ComposeChip(
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
    cornerShape: Dp = 5.dp,
    boarderColor: Color = MaterialTheme.colors.primary,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        elevation = elevation,
        shape = RoundedCornerShape(size = cornerShape),
        border = BorderStroke(width = 1.dp, color = boarderColor)
    ) {
        content()
    }
}

/**
 * チェックボックス付きChip
 *
 * @param modifier Modifier
 * @param boarderColor 枠の色
 * @param content Chipの中身
 * @param cornerShape 角どれだけ丸くするか
 * @param elevation 影
 * @param isChecked チェックを付ける場合はtrue
 * @param onCheckedChange チェックボックスの値が変わったら呼ばれる
 * @param label チェックボックスの隣に表示するUI。Text()とかね
 * */
@Composable
fun ComposeCheckableChip(
    modifier: Modifier = Modifier,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    elevation: Dp = 2.dp,
    cornerShape: Dp = 5.dp,
    boarderColor: Color = MaterialTheme.colors.primary,
    label: @Composable () -> Unit,
) {
    ComposeChip(
        modifier = modifier.clickable { onCheckedChange(!isChecked) },
        elevation = elevation,
        cornerShape = cornerShape,
        boarderColor = boarderColor,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                modifier = Modifier.padding(5.dp),
                checked = isChecked,
                onCheckedChange = { onCheckedChange(it) }
            )
            Box(modifier = Modifier.padding(5.dp)){
                label()
            }
        }
    }
}

@Preview
@Composable
fun ComposeChipPreview() {
    ComposeCheckableChip(isChecked = false, onCheckedChange = {  }) {
        Text(text = "生放送")
    }
}