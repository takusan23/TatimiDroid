package io.github.takusan23.tatimidroid.FregmentData

import android.os.Bundle
import java.io.Serializable

/**
 * ViewPagerに動的に追加した際に、画面回転してもFragmentを再生成するための値を置いておくデータクラス。
 * 多分画面回転時の受け渡し以外で使うことはない。
 * @param bundle Fragment#getArgments()の値。Parcelableだから多分行ける
 * @param text TabLayoutに表示するテキスト。
 * @param type DevNicoVideoRecyclerPagerAdapter#getType(Fragment)の返り値。post / mylist / search のどれかだと思う。
 * */
data class TabLayoutData(
    val type: String,
    val text: String,
    val bundle: Bundle?
) : Serializable