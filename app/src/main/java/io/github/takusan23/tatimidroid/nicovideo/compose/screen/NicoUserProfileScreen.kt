package io.github.takusan23.tatimidroid.nicovideo.compose.screen

import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.FillLoadingScreen
import io.github.takusan23.tatimidroid.compose.FillTextButton
import io.github.takusan23.tatimidroid.compose.PlaceholderImage
import io.github.takusan23.tatimidroid.nicoapi.user.UserData
import io.github.takusan23.tatimidroid.nicovideo.compose.getBitmapCompose
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoAccountViewModel

/**
 * プロフィール画面。Composeでできている
 *
 * @param viewModel ViewModel
 * */
@Composable
fun NicoUserProfileScreen(viewModel: NicoAccountViewModel) {
    // ユーザーデータ
    val userData = viewModel.userDataLiveData.observeAsState()
    // フォロー状態変更通知LiveData
    val isFollowing = viewModel.followStatusLiveData.observeAsState(initial = false)

    if (userData.value != null) {
        Column {
            // 情報
            NicoUserProfileHeader(
                userData = userData.value!!,
                isMyUserProfile = false,
                isFollowing = isFollowing.value
            )
            Divider()
            // メニュー
            NicoUserProfileMenu(
                follow = userData.value!!.followeeCount,
                follower = userData.value!!.followerCount
            )
        }
    } else {
        FillLoadingScreen()
    }
}

/**
 * ヘッダー部分。
 *
 * @param userData ニコ動ユーザーデータクラス
 * */
@Composable
private fun NicoUserProfileHeader(userData: UserData, isMyUserProfile: Boolean, isFollowing: Boolean) {
    // アイコン
    val userIcon = getBitmapCompose(url = userData.largeIcon)?.asImageBitmap()

    Column {
        Row {
            PlaceholderImage(
                modifier = Modifier.size(100.dp),
                isLoading = userIcon == null,
                imageBitmap = userIcon
            )
            Column(modifier = Modifier.padding(2.dp)) {
                Text(text = userData.nickName, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "ID : ${userData.userId}")
                    NicoUserProfilePremiumLabel(isPremium = userData.isPremium)
                }
                Text(text = "${userData.niconicoVersion} (15番目のバージョン)")
            }
        }
        // フォローボタン。自分の場合は隠す
        if (!isMyUserProfile) {
            Button(onClick = { }, Modifier.align(alignment = Alignment.CenterHorizontally)) {
                Icon(painter = if (userData.isFollowing) painterResource(id = R.drawable.ic_baseline_done_24) else painterResource(id = R.drawable.ic_outline_star_border_24), contentDescription = null)
                Text(text = if (userData.isFollowing) "フォロー中" else "フォローする")
            }
        }
        // HtmlをサポートするためAndroidViewでTextViewを使う
        AndroidView(
            modifier = Modifier.padding(5.dp),
            factory = { context ->
                TextView(context).apply {
                    text = HtmlCompat.fromHtml(userData.description, HtmlCompat.FROM_HTML_MODE_COMPACT)
                    autoLinkMask = Linkify.WEB_URLS
                }
            }
        )
    }
}

/**
 * マイリスト、投稿動画、ニコレポのメニュー一覧
 *
 * @param follow フォロー中人数
 * @param follower フォロワー人数
 * */
@Composable
private fun NicoUserProfileMenu(
    follow: Int,
    follower: Int
) {
    Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
        FillTextButton(onClick = { }, modifier = Modifier.padding(5.dp)) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_people_outline_24px), contentDescription = null)
            Text(text = stringResource(id = R.string.nicorepo))
        }
        FillTextButton(onClick = { }, modifier = Modifier.padding(5.dp)) {
            Icon(painter = painterResource(id = R.drawable.ic_cloud_upload_black_24dp), contentDescription = null)
            Text(text = stringResource(id = R.string.post_video))
        }
        FillTextButton(onClick = { }, modifier = Modifier.padding(5.dp)) {
            Icon(painter = painterResource(id = R.drawable.ic_folder_open_black_24dp), contentDescription = null)
            Text(text = stringResource(id = R.string.mylist))
        }
        FillTextButton(onClick = { }, modifier = Modifier.padding(5.dp)) {
            Icon(painter = painterResource(id = R.drawable.ic_folder_open_black_24dp), contentDescription = null)
            Text(text = stringResource(id = R.string.series))
        }
        Divider()
        FillTextButton(onClick = { }, modifier = Modifier.padding(5.dp)) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_people_outline_24px), contentDescription = null)
            Text(text = "${stringResource(id = R.string.follow_count)} : $follow")
        }
        FillTextButton(onClick = { }, modifier = Modifier.padding(5.dp)) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_people_outline_24px), contentDescription = null)
            Text(text = "${stringResource(id = R.string.follower_count)} : $follower")
        }
    }
}

/**
 * プレミアム会員ラベル
 *
 * @param isPremium プレ垢ならtrue
 * */
@Composable
private fun NicoUserProfilePremiumLabel(isPremium: Boolean = true) {
    Surface(
        modifier = Modifier.padding(5.dp),
        color = if (isPremium) Color(0xfffbc02d) else Color.Gray,
        shape = RoundedCornerShape(5.dp)
    ) {
        Text(
            text = if (isPremium) "\uD83C\uDD7F プレミアム会員" else "一般会員",
            modifier = Modifier.padding(start = 5.dp, end = 5.dp)
        )
    }
}

