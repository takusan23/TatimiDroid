package io.github.takusan23.tatimidroid.NicoVideo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.takusan23.tatimidroid.R

class NicoVideoCommentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nico_video_comment)

        supportFragmentManager.beginTransaction()
            .replace(R.id.activity_comment_linearlayout, NicoVideoCommentFragment()).commit()

    }
}
