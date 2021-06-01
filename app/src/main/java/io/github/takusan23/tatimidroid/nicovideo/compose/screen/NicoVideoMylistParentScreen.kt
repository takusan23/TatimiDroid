package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.FillLoadingScreen
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoMyListData
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoMyListViewModel

/**
 * ニコ動のマイリスト一覧画面
 *
 * @param viewModel マイリスト一覧で使うViewModel
 * @param onClickMylist マイリスト押したら呼ばれる
 * */
@Composable
fun NicoVideoMylistParentScreen(
    viewModel: NicoVideoMyListViewModel,
    onClickMylist: (NicoVideoMyListData) -> Unit,
) {
    // マイリスト一覧
    val mylistList = viewModel.myListDataLiveData.observeAsState()
    // 読み込み中ならtrue
    val isLoading = viewModel.loadingLiveData.observeAsState(initial = true)

    if (isLoading.value || mylistList.value == null) {
        // 読み込み中
        FillLoadingScreen()
    } else {
        // マイリスト一覧
        NicoVideoMylistList(
            list = mylistList.value!!,
            onClickMylist = { onClickMylist(it) }
        )
    }
}

/**
 * マイリスト一覧
 *
 * @param list マイリスト情報のデータクラスの配列
 * @param onClickMylist マイリスト押したら呼ばれる
 * */
@Composable
private fun NicoVideoMylistList(
    list: List<NicoVideoMyListData>,
    onClickMylist: (NicoVideoMyListData) -> Unit,
) {
    LazyColumn {
        items(list) { nicoVideoMyListData ->
            NicoVideoMylistListItem(
                nicoVideoMyListData = nicoVideoMyListData,
                onClickMylist = { onClickMylist(it) }
            )
            Divider()
        }
    }
}

/**
 * マイリスト一覧の一個一個の項目
 *
 * @param nicoVideoMyListData マイリスト情報のデータクラス
 * @param onClickMylist マイリスト押したら呼ばれる
 * */
@Composable
private fun NicoVideoMylistListItem(
    nicoVideoMyListData: NicoVideoMyListData,
    onClickMylist: (NicoVideoMyListData) -> Unit
) {
    Surface(modifier = Modifier.clickable { onClickMylist(nicoVideoMyListData) }) {
        Row {
            Icon(
                modifier = Modifier.padding(5.dp),
                painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
                contentDescription = null
            )
            Column(modifier = Modifier.padding(5.dp)) {
                Text(text = nicoVideoMyListData.title, fontSize = 18.sp)
                Text(text = "${nicoVideoMyListData.itemsCount} 件", fontSize = 12.sp)
            }
        }
    }
}