package io.github.takusan23.tatimidroid.compose

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.tool.isDarkMode

/**
 * MainActivityのComposeで使うBottomNavigation
 * @param onSelect メニュー押したとき。引数はそのままroute()に渡せる
 * */
@Composable
fun MainActivityNavigation(onSelect: (route: String) -> Unit) {
    BottomNavigation(backgroundColor = if (isDarkMode(LocalContext.current)) Color.Black else Color.White) {
        BottomNavigationItem(
            selected = false,
            onClick = { onSelect("nicolive") },
            label = { Text(text = stringResource(id = R.string.nicolive)) },
            icon = { Icon(painter = painterResource(id = R.drawable.live_icon), contentDescription = "生放送") }
        )
        BottomNavigationItem(
            selected = false,
            onClick = { onSelect("nicovideo") },
            label = { Text(text = stringResource(id = R.string.nicovideo)) },
            icon = { Icon(painter = painterResource(id = R.drawable.video_icon), contentDescription = "動画") }
        )
        BottomNavigationItem(
            selected = false,
            onClick = { onSelect("cache") },
            label = { Text(text = stringResource(id = R.string.cache)) },
            icon = { Icon(painter = painterResource(id = R.drawable.ic_cache_icon_list), contentDescription = "キャッシュ") }
        )
        BottomNavigationItem(
            selected = false,
            onClick = { onSelect("login") },
            label = { Text(text = stringResource(id = R.string.login)) },
            icon = { Icon(painter = painterResource(id = R.drawable.ic_lock_outline_black_24dp), contentDescription = "ログイン") }
        )
        BottomNavigationItem(
            selected = false,
            onClick = { onSelect("setting") },
            label = { Text(text = stringResource(id = R.string.setting)) },
            icon = { Icon(painter = painterResource(id = R.drawable.ic_outline_settings_24px), contentDescription = "設定") }
        )
    }
}
