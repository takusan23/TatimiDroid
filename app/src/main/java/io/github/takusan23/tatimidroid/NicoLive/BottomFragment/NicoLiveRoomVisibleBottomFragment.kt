package io.github.takusan23.tatimidroid.NicoLive.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel

/**
 * ニコ生の表示する部屋の選択（といっても部屋統合と流量制限しか無いけど）
 *
 * ついでに簡易的な説明付き
 *
 * を選択するやつ。[io.github.takusan23.tatimidroid.NicoLive.JetpackCompose.JCNicoLiveCommentListFragment]には切り替えボタン置く場所なくなったので
 *
 * [requireParentFragment]は[io.github.takusan23.tatimidroid.NicoLive.JetpackCompose.JCNicoLiveFragment]を指すようにしてください。
 * */
class NicoLiveRoomVisibleBottomFragment : BottomSheetDialogFragment() {

    /** ViewModelを通じて設定を適用させる */
    private val viewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                // LiveData
                val isReceiveArenaComment = viewModel.isReceiveArenaCommentLiveData.observeAsState(initial = true)
                val isReceiveLimitComment = viewModel.isReceiveLimitCommentLiveData.observeAsState(initial = true)
                NicoLiveRoomSelectScreen(
                    isVisibleArena = isReceiveArenaComment.value,
                    isVisibleLimit = isReceiveLimitComment.value,
                    onChangeArena = { viewModel.isReceiveArenaCommentLiveData.postValue(it) },
                    onChangeLimit = { viewModel.isReceiveLimitCommentLiveData.postValue(it) },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * 表示するコメント鯖（部屋統合、流量制限）を選ぶUIをJetpack Composeで作る
     *
     * @param isVisibleArena コメント一覧に部屋統合を表示する場合はtrue
     * @param isVisibleLimit コメント一覧に流量制限を表示する場合はtrue
     * @param onChangeArena 部屋統合のチェックを入れたときに呼ばれる
     * @param onChangeLimit 流量制限のチェックを入れたときに呼ばれる
     * */
    @Composable
    private fun NicoLiveRoomSelectScreen(
        isVisibleArena: Boolean,
        onChangeArena: (Boolean) -> Unit,
        isVisibleLimit: Boolean,
        onChangeLimit: (Boolean) -> Unit,
    ) {
        Column(modifier = Modifier.padding(5.dp)) {
            // タイトル
            Row(modifier = Modifier.padding(5.dp)) {
                Icon(imageVector = Icons.Outlined.CallMerge)
                Text(text = "表示するコメントサーバーの設定", fontSize = 20.sp)
            }
            // 部屋統合の説明
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .clickable(onClick = { onChangeArena(!isVisibleArena) }, indication = rememberRipple()) // Rowにクリックイベント設置
            ) {
                Column(
                    modifier = Modifier
                        .padding(5.dp)
                        .weight(1f)
                ) {
                    Row(modifier = Modifier.padding(5.dp)) {
                        Icon(imageVector = Icons.Outlined.Dns)
                        Text(text = "部屋統合", fontSize = 18.sp)
                    }
                    Text(
                        text = """
                        |ニコ生のPC版やスマホ版はここのコメントサーバーに接続されてます。
                        |コメントの量が多くなった場合は プレ垢関係なく コメントの選別が行われるため、一部のコメントは溢れて他からは見えなくなります。
                    """.trimMargin()
                    )
                }
                Checkbox(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.CenterVertically),
                    checked = isVisibleArena,
                    onCheckedChange = { onChangeArena(it) }
                )
            }
            // 区切り線
            Divider()
            // 流量制限説明
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .clickable(onClick = { onChangeLimit(!isVisibleLimit) }, indication = rememberRipple()) // Rowにクリックイベント設置
            ) {
                Column(
                    modifier = Modifier
                        .padding(5.dp)
                        .weight(1f)
                ) {
                    Row(modifier = Modifier.padding(5.dp)) {
                        Icon(imageVector = Icons.Outlined.Dns)
                        Text(text = "流量制限", fontSize = 18.sp)
                    }
                    Text(
                        text = """
                        |部屋統合のコメントサーバーで溢れてしまったコメントを流してくれるコメントサーバーです。他のコメビュだとハブられたコメント？
                        |コメントの量が多くなってコメントの選別が発動した場合でも、すべてのコメントを拾うことができます。
                        |ただし公式番組ではAPIが対応してないため利用できません。
                    """.trimMargin()
                    )
                }
                Checkbox(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.CenterVertically),
                    checked = isVisibleLimit,
                    onCheckedChange = { onChangeLimit(it) }
                )
            }

        }
    }
}
