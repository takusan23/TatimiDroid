package io.github.takusan23.tatimidroid.nicolive.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.nicovideo.compose.getBitmapCompose
import io.github.takusan23.tatimidroid.tool.TimeFormatTool

/**
 * 番組を一覧表示する部品
 *
 * @param list 番組情報の配列
 * @param onClickProgram 番組を押したときに呼ばれる。放送中じゃなくても押したら呼ばれる
 * @param onClickMenu メニューを押したときに呼ばれる
 * */
@ExperimentalMaterialApi
@Composable
fun NicoLiveProgramList(
    list: List<NicoLiveProgramData>,
    onClickProgram: (NicoLiveProgramData) -> Unit,
    onClickMenu: (NicoLiveProgramData) -> Unit,
) {
    LazyColumn {
        items(list) { programData ->
            NicoLiveProgramListItem(
                nicoLiveProgramData = programData,
                onClickProgram = { onClickProgram(it) },
                onClickMenu = { onClickMenu(it) }
            )
            Divider()
        }
    }
}

/**
 * 番組一覧表示の一つ一つの部品
 *
 * @param nicoLiveProgramData 番組情報
 * @param onClickProgram 番組を押したときに呼ばれる
 * @param onClickMenu メニューを押したときに呼ばれる
 * */
@ExperimentalMaterialApi
@Composable
fun NicoLiveProgramListItem(
    nicoLiveProgramData: NicoLiveProgramData,
    onClickProgram: (NicoLiveProgramData) -> Unit,
    onClickMenu: (NicoLiveProgramData) -> Unit,
) {
    Surface(onClick = { onClickProgram(nicoLiveProgramData) }) {
        Row(
            modifier = Modifier
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            val thumb = getBitmapCompose(url = nicoLiveProgramData.thum)?.asImageBitmap()

            // 放送中か
            val isOpen = nicoLiveProgramData.lifeCycle == "ON_AIR"
            // 放送中なら赤色を使いたいので
            val lifecycleColor = if (isOpen) Color(0xffe53935) else Color(0xff1e88e5)

            // さむね
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(1.7f)
                    .clip(shape = RoundedCornerShape(5.dp))
            ) {
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
                        .background(lifecycleColor.copy(alpha = 0.5f)),
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
            // タイトル名など
            Column(
                modifier = Modifier
                    .padding(5.dp)
                    .weight(1f)
            ) {
                Text(
                    text = TimeFormatTool.unixTimeToFormatDate(nicoLiveProgramData.beginAt.toLong()),
                    fontSize = 12.sp,
                    color = lifecycleColor
                )
                Text(text = nicoLiveProgramData.title, maxLines = 2, fontWeight = FontWeight.Bold)
                Text(text = nicoLiveProgramData.communityName, fontSize = 14.sp)
            }
            // メニューボタン
            IconButton(onClick = { onClickMenu(nicoLiveProgramData) }) {
                Icon(painter = painterResource(id = R.drawable.ic_more_vert_24px), contentDescription = "メニュー")
            }
        }
    }
}
