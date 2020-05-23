package io.github.takusan23.tatimidroid.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.Fragment.CommentFragment
import io.github.takusan23.tatimidroid.R

/*
* わざわざFloatingなコメントビューあーを作るためにクラスを作ったのかって？
* AndroidManifestに属性を設定しないといけないんですけどそれをCommentActivityで適用して
* BubblesAPIを使おうとするとアプリが履歴で大増殖します。
* 多分こいつ（属性）のせい。複数インスタンスを許可するには必要らしい。→android:documentLaunchMode
* というわけでわざわざ作ったわけです。
* */
class FloatingCommentViewer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //ダークモード対応
        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_floating_comment_viewer)

        val liveId = intent.getStringExtra("liveId")

        //Fragment設置
        val trans = supportFragmentManager.beginTransaction()
        val commentFragment = CommentFragment()
        //LiveID詰める
        val bundle = Bundle()
        bundle.putString("liveId", liveId)
        commentFragment.arguments = bundle
        trans.replace(R.id.activity_floating_comment_viewer_linearlayout, commentFragment, liveId)
        trans.commit()
    }
}
