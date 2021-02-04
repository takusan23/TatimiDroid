package io.github.takusan23.tatimidroid.JetpackCompose

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * タブレイアウトの一つ一つのTab。
 * @param index 何個目のTabかどうか
 * @param tabIcon アイコン
 * @param tabName 名前
 * @param selectedIndex 現在選択中のタブの位置
 * @param tabClick タブを押した時
 * */
@Composable
fun TabPadding(index: Int, tabName: String, tabIcon: ImageVector, selectedIndex: Int, tabClick: (Int) -> Unit) {
    Tab(
        modifier = Modifier.padding(5.dp),
        selected = selectedIndex == index,
        onClick = {
            tabClick(index)
        }
    ) {
        Icon(imageVector = tabIcon, contentDescription = tabName)
        Text(text = tabName)
    }
}