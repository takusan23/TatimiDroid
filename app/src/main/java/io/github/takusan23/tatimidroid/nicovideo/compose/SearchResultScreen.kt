package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoTagItemData
import io.github.takusan23.tatimidroid.nicovideo.adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSearchResultViewModel

/**
 * 検索結果のUI。Composeで作成する
 *
 * @param searchResultViewModel 検索結果FragmentのViewModel
 * @param onBackScreen 前のFragmentに戻ってほしいときに呼ばれる関数
 * */
@Composable
fun SearchResultScreen(searchResultViewModel: NicoVideoSearchResultViewModel, onBackScreen: () -> Unit) {

    // 読み込み中
    val isLoading = searchResultViewModel.isLoadingLiveData.observeAsState(initial = false)
    // 検索結果動画一覧
    val videoSearchResult = searchResultViewModel.searchResultNicoVideoDataListLiveData.observeAsState()

    MaterialTheme {
        Scaffold(
            topBar = {
                // 戻るボタンとか関連タグとか
                SearchResultAppBar(onBackScreen = { onBackScreen() })
            }
        ) {
            Column(Modifier.fillMaxWidth()) {
                if (isLoading.value) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                // AndroidView
                if (videoSearchResult.value != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { context ->
                            RecyclerView(context).apply {
                                setHasFixedSize(true)
                                layoutManager = LinearLayoutManager(context)
                                adapter = NicoVideoListAdapter(videoSearchResult.value!!, true)
                                isVerticalScrollBarEnabled
                            }
                        }
                    )
                }
            }
        }
    }
}