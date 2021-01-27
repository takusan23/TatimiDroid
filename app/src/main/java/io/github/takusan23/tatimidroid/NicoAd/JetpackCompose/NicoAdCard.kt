package io.github.takusan23.tatimidroid.NicoAd.JetpackCompose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.JetpackCompose.TabPadding
import io.github.takusan23.tatimidroid.NicoAPI.NicoAd.NicoAdData
import io.github.takusan23.tatimidroid.NicoAPI.NicoAd.NicoAdHistoryUserData
import io.github.takusan23.tatimidroid.NicoAPI.NicoAd.NicoAdRankingUserData
import io.github.takusan23.tatimidroid.NicoLive.Adapter.NicoAdHistoryAdapter
import io.github.takusan23.tatimidroid.NicoLive.Adapter.NicoAdRankingAdapter
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.getBitmapCompose
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.isDarkMode

/**
 * ニコニ広告の累計ポイント、期間中ポイントを表示する
 *
 * @param nicoAdData ニコニ広告のデータ
 * */
@Composable
fun NicoAdTop(nicoAdData: NicoAdData) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 画像
            val bitmap = getBitmapCompose(url = nicoAdData.thumbnailUrl)?.asImageBitmap()
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    modifier = Modifier
                        .width(160.dp)
                        .height(90.dp)
                        .padding(5.dp)
                )
            }
            // タイトル
            Column {
                Text(
                    text = nicoAdData.contentTitle,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(5.dp)
                )
                Text(
                    text = nicoAdData.contentId,
                    modifier = Modifier.padding(5.dp)
                )
            }
        }
        // 区切り
        Divider(modifier = Modifier.padding(5.dp))
        // 広告ポイント
        Row(modifier = Modifier.padding(5.dp)) {
            // 累計ポイント
            Column(
                Modifier
                    .weight(1f)
                    .padding(5.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(id = R.string.nicoad_total),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${nicoAdData.totalPoint} pt",
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            // 期間中ポイント
            Column(
                Modifier
                    .weight(1f)
                    .padding(5.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(id = R.string.nicoad_active),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${nicoAdData.activePoint} pt",
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * ニコニ広告のランキング一覧表示
 *
 * @param nicoAdRankingUserList ニコニ広告のランキングユーザー配列
 * */
@Composable
fun NicoAdRankingList(nicoAdRankingUserList: ArrayList<NicoAdRankingUserData>) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        viewBlock = { context ->
            return@AndroidView RecyclerView(context).apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = NicoAdRankingAdapter(nicoAdRankingUserList)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    )
}

/**
 * ニコニ広告の履歴一覧表示
 *
 * @param nicoAdHistoryUserList ニコニ広告のユーザー履歴配列
 * */
@Composable
fun NicoAdHistoryList(nicoAdHistoryUserList: ArrayList<NicoAdHistoryUserData>) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        viewBlock = { context ->
            return@AndroidView RecyclerView(context).apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = NicoAdHistoryAdapter(nicoAdHistoryUserList)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    )
}

/**
 * ニコニ広告の貢献度ランキング、履歴選択用タブレイアウト
 *
 * @param selectTabIndex 現座選択中のタブのいち
 * @param onClickTabItem タブを押した時
 * */
@Composable
fun NicoAdTabMenu(
    selectTabIndex: Int,
    onClickTabItem: (Int) -> Unit,
) {
    TabRow(
        backgroundColor = if (isDarkMode(AmbientContext.current)) Color.Black else Color.White,
        selectedTabIndex = selectTabIndex
    ) {
        TabPadding(
            index = 0,
            tabName = stringResource(id = R.string.nico_ad_ranking),
            tabIcon = Icons.Outlined.SignalCellularAlt,
            selectedIndex = selectTabIndex,
            tabClick = { onClickTabItem(it) }
        )
        TabPadding(
            index = 1,
            tabName = stringResource(id = R.string.nico_ad_history),
            tabIcon = Icons.Outlined.History,
            selectedIndex = selectTabIndex,
            tabClick = { onClickTabItem(it) }
        )
    }
}