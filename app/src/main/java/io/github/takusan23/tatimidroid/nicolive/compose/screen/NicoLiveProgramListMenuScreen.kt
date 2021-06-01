package io.github.takusan23.tatimidroid.nicolive.compose

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.FillLoadingScreen
import io.github.takusan23.tatimidroid.compose.FillTextButton
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.nicolive.activity.FloatingCommentViewer
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveProgramListMenuViewModel
import io.github.takusan23.tatimidroid.service.startLivePlayService
import io.github.takusan23.tatimidroid.tool.ContentShareTool
import io.github.takusan23.tatimidroid.tool.CreateShortcutTool
import io.github.takusan23.tatimidroid.tool.toFormatTime
import kotlinx.coroutines.launch

/**
 * 番組一覧で利用するメニュー。タイムシフト予約とか
 * */
@Composable
fun NicoLiveProgramListMenu(viewModel: NicoLiveProgramListMenuViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 番組情報データクラスをLiveDataで
    val nicoLiveProgramData = viewModel.programDataLiveData.observeAsState()

    if (nicoLiveProgramData.value == null) {
        // 取得中
        FillLoadingScreen()
    } else {
        // 各メニュー
        LazyColumn {
            item {
                // 番組情報
                NicoLiveProgramListMenuProgramInfo(nicoLiveProgramData = nicoLiveProgramData.value!!)
                // 区切り線
                Divider()

                // 放送中かどうか
                val isOnAir = nicoLiveProgramData.value!!.lifeCycle == "ON_AIR"

                FillTextButton(
                    onClick = {
                        scope.launch {
                            CreateShortcutTool.createHomeScreenShortcut(
                                context = context,
                                contentId = nicoLiveProgramData.value!!.programId,
                                contentTitle = nicoLiveProgramData.value!!.communityName,
                                thumbUrl = nicoLiveProgramData.value!!.thum
                            )
                        }
                    },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_outline_add_to_home_screen_24), contentDescription = null)
                    Text(text = stringResource(id = R.string.add_homescreen))
                }
                FillTextButton(
                    onClick = { viewModel.copyCommunityId() },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_content_paste_black_24dp), contentDescription = null)
                    Text(text = stringResource(id = R.string.copy_communityid))
                }
                FillTextButton(
                    onClick = { viewModel.copyProgramId() },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_content_paste_black_24dp), contentDescription = null)
                    Text(text = stringResource(id = R.string.copy_program_id))
                }
                // ポップアップ、バックグラウンド等は放送中のみ
                if (isOnAir) {
                    FillTextButton(
                        onClick = { startLivePlayService(context = context, mode = "popup", liveId = nicoLiveProgramData.value!!.programId) },
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_popup_icon_black), contentDescription = null)
                        Text(text = stringResource(id = R.string.popup_player))
                    }
                    FillTextButton(
                        onClick = { startLivePlayService(context = context, mode = "background", liveId = nicoLiveProgramData.value!!.programId) },
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_background_icon_black), contentDescription = null)
                        Text(text = stringResource(id = R.string.background_play))
                    }
                    FillTextButton(
                        onClick = { FloatingCommentViewer.showBubbles(context = context, liveId = nicoLiveProgramData.value!!.programId, title = nicoLiveProgramData.value!!.title, thumbUrl = nicoLiveProgramData.value!!.thum) },
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_library_books_24px), contentDescription = null)
                        Text(text = stringResource(id = R.string.floating_comment_viewer))
                    }
/*
                    TextButton(
                        onClick = { },
                        modifier = Modifier.padding(5.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_comment_24px), contentDescription = null)
                        Text(text = stringResource(id = R.string.comment_list_only))
                    }
*/
                }
                FillTextButton(
                    onClick = { viewModel.registerOrUnRegisterTimeShift() },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_history_24px), contentDescription = null)
                    Text(text = stringResource(id = R.string.timeshift_reservation_button))
                }
                FillTextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            data = CalendarContract.Events.CONTENT_URI
                            putExtra(CalendarContract.Events.TITLE, nicoLiveProgramData.value!!.title)
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, nicoLiveProgramData.value!!.beginAt.toLong() * 1000) // ミリ秒らしい。
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, nicoLiveProgramData.value!!.endAt.toLong() * 1000) // ミリ秒らしい。
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_event_available_24px), contentDescription = null)
                    Text(text = stringResource(id = R.string.add_calendar))
                }
                FillTextButton(
                    onClick = {
                        ContentShareTool(context).showShareContent(programId = nicoLiveProgramData.value!!.programId, programName = nicoLiveProgramData.value!!.title, fromTimeSecond = null, uri = null, message = null)
                    },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_share), contentDescription = null)
                    Text(text = stringResource(id = R.string.share))
                }
            }
        }
    }
}

/**
 * 番組情報表示部分
 * @param nicoLiveProgramData 番組情報
 * */
@Composable
private fun NicoLiveProgramListMenuProgramInfo(nicoLiveProgramData: NicoLiveProgramData) {
    Column(modifier = Modifier.padding(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // タイトル
                Text(
                    text = nicoLiveProgramData.title,
                    style = TextStyle(fontSize = 18.sp),
                )
                // 生放送ID
                Text(
                    text = nicoLiveProgramData.programId,
                    style = TextStyle(fontSize = 12.sp),
                )
            }
        }
        // 番組開始、終了時刻
        Row {
            Icon(painter = painterResource(id = R.drawable.ic_outline_meeting_room_24px), contentDescription = null)
            Text(text = "${stringResource(id = R.string.nicolive_begin_time)}：${toFormatTime(nicoLiveProgramData.beginAt.toLong() * 1000)}")
        }
        Row {
            Icon(painter = painterResource(id = R.drawable.ic_outline_no_meeting_room_24), contentDescription = null)
            Text(text = "${stringResource(id = R.string.nicolive_end_time)}：${toFormatTime(nicoLiveProgramData.endAt.toLong() * 1000)}")
        }
    }
}