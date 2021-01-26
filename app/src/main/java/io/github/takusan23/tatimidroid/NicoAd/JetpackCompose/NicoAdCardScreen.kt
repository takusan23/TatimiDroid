package io.github.takusan23.tatimidroid.NicoAd.JetpackCompose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.NicoAd.NicoAdViewModel

/**
 * [io.github.takusan23.tatimidroid.NicoAd.NicoAdBottomFragment]で表示しているCompose
 * */
@Composable
fun NicoAdScreen(viewModel: NicoAdViewModel) {
    // ニコニ広告データ
    val nicoAdDataLiveData = viewModel.nicoAdDataLiveData.observeAsState()
    // ニコニ広告ランキングデータ
    val nicoAdRankingLiveData = viewModel.nicoAdRankingLiveData.observeAsState()
    // ニコニ広告履歴データ
    val nicoAdHistoryLiveData = viewModel.nicoAdHistoryLiveData.observeAsState()

    Column {
        // タイトル、累計ポイント、アクティブポイント表示
        if (nicoAdDataLiveData.value != null) {
            Card(
                modifier = Modifier.padding(5.dp),
                elevation = 5.dp,
                shape = RoundedCornerShape(3.dp)
            ) {
                NicoAdTop(nicoAdData = nicoAdDataLiveData.value!!)
            }
        }
        // タブとランキング、履歴表示
        Card(
            modifier = Modifier.padding(5.dp),
            elevation = 5.dp,
            shape = RoundedCornerShape(3.dp)
        ) {
            Column {
                // 現在選択中ダブ
                val selectTab = remember { mutableStateOf(0) }
                // タブ
                NicoAdTabMenu(
                    selectTabIndex = selectTab.value,
                    onClickTabItem = { selectTab.value = it }
                )
                // どっちを表示するか
                when (selectTab.value) {
                    0 -> NicoAdRankingList(nicoAdRankingUserList = nicoAdRankingLiveData.value ?: arrayListOf())
                    1 -> NicoAdHistoryList(nicoAdHistoryUserList = nicoAdHistoryLiveData.value ?: arrayListOf())
                }
            }
        }
    }
}