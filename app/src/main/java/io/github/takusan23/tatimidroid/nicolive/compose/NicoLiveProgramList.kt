package io.github.takusan23.tatimidroid.nicolive.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.nicovideo.compose.getBitmapCompose
import io.github.takusan23.tatimidroid.tool.TimeFormatTool

/**
 * 番組を一覧表示する部品
 *
 * @param list 番組情報の配列
 * @param onClickProgram 番組を押したときに呼ばれる
 * */
@Composable
fun NicoLiveProgramList(list: List<NicoLiveProgramData>, onClickProgram: (NicoLiveProgramData) -> Unit) {
    LazyColumn {
        items(list) { programData ->
            NicoLiveProgramListItem(nicoLiveProgramData = programData, onClickProgram = { onClickProgram(it) })
            Divider()
        }
    }
}

/**
 * 番組一覧表示の一つ一つの部品
 *
 * @param nicoLiveProgramData 番組情報
 * @param onClickProgram 番組を押したときに呼ばれる
 * */
@Composable
fun NicoLiveProgramListItem(nicoLiveProgramData: NicoLiveProgramData, onClickProgram: (NicoLiveProgramData) -> Unit) {
    Row(
        modifier = Modifier
            .padding(5.dp)
            .clickable { onClickProgram(nicoLiveProgramData) },
        verticalAlignment = Alignment.CenterVertically,
    ) {

        val thumb = getBitmapCompose(url = nicoLiveProgramData.thum)?.asImageBitmap()
        val thumbModifier = Modifier
            .width(100.dp)
            .aspectRatio(1.7f)
            .clip(RoundedCornerShape(5.dp))

        // さむね
        Box(modifier = thumbModifier) {
            if (thumb != null) {
                Image(
                    bitmap = thumb,
                    contentDescription = nicoLiveProgramData.thum,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 放送中？
            Text(
                text = nicoLiveProgramData.lifeCycle,
                Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .background(Color.Red.copy(alpha = 0.5f)),
                fontSize = 10.sp,
                color = Color.White
            )
        }
        // その他
        Column(
            modifier = Modifier
                .padding(5.dp)
                .weight(1f)
        ) {
            val isOpen = nicoLiveProgramData.lifeCycle == "ON_AIR"
            Text(text = TimeFormatTool.unixTimeToFormatDate(nicoLiveProgramData.beginAt.toLong()), fontSize = 12.sp, color = if (isOpen) Color.Red else Color.Black)
            Text(text = nicoLiveProgramData.title, maxLines = 2)
            Text(text = nicoLiveProgramData.communityName, fontSize = 12.sp)
        }
    }
}
