package io.github.takusan23.tatimidroid.DevNicoVideo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import io.github.takusan23.tatimidroid.CommentJSONParse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * コメントの盛り上がりを可視化できるやつ作る。
 * これで きしめん とか はやぶさ とか ごちうさ とか ダイナモ感覚 とか メニメニマニマニ とかのコメントの分布見たらおもろいと思った
 * */
class DanmakuView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // 色：X座標
    private var danmakuList = arrayListOf<Pair<Int, Int>>()

    // Viewの幅。getWidth()が0返すので
    var viewWidth = 0

    // Canvasに書く幅
    private var danmakuWidth = 0F

    // 書き込む時に使う
    private val paint = Paint()

    /**
     * コメントの盛り上がりを描画する。
     * @param videoDuration 再生時間。秒で
     * @param commentList CommentJSONParseの配列。
     * @param finalWidth ここでgetWidth()しても0帰ってくるから、addOnGlobalLayoutListener{}で取得して。
     * */
    fun init(videoDuration: Long, commentList: ArrayList<CommentJSONParse>, finalWidth: Int) {
        // 0なら動かない。ついでに非表示
        if (commentList.isEmpty()) {
            isVisible = false
            return
        }
        // 幅決定
        viewWidth = finalWidth
        // 秒
        val space = 10 // 何秒間隔にするか。
        val minute = videoDuration / space // spaceの値から何個分生成するか（1分を10秒ごとなら6個分だね。）
        // なん分割するか
        danmakuWidth = (viewWidth.toFloat() / minute.toFloat()).roundToInt().toFloat() // Floatだと最後まで描画される（整数に丸めると最後微妙に足りない）
        // 配列操作は重いので非同期処理。コルーチンくんなんか軽いって聞いたのでぽんぽん使ってるけど良いんか？
        GlobalScope.launch {
            val percentList = arrayListOf<Int>()
            // 配列から取り出して10秒ごとのパーセントを求める
            for (i in 0..minute) {
                // 10秒ごとで取り出す
                val secondList = commentList.filter { commentJSONParse ->
                    ((commentJSONParse.vpos.toInt() / 100) / space) == i.toInt()
                }
                // ぱーせんと
                val percent = ((secondList.size.toFloat() / commentList.size.toFloat()) * 100).roundToInt()
                percentList.add(percent)
            }
            // 最大値を取る（最大値を高さの限界にする）
            val maxValue = percentList.max() ?: 10
            // 描画用配列に保存
            percentList.forEach {
                // 描画する高さ
                val viewHeight = (height.toFloat() / maxValue).roundToInt()
                val drawHeight = it * viewHeight
                danmakuList.add(Pair(Color.GRAY, drawHeight))
            }
            withContext(Dispatchers.Main) {
                invalidate()
            }
        }
    }

    // Canvasに書き込む
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // 空なら落とす
        if (danmakuList.size == 0) {
            return
        }
        /**
         * ↓第1、第2引数
         *  □
         *   ↑第3、第4引数
         * */
        var left = 0F
        var right = danmakuWidth
        for (i in 0 until danmakuList.size) {
            val top = height - danmakuList[i].second.toFloat()
            val bottom = height.toFloat()
            paint.color = danmakuList[i].first
            canvas?.drawRect(left, top, right, bottom, paint)
            // 書いたら次進める
            left += danmakuWidth
            right += danmakuWidth
        }
    }

/*
    // 画面回転時に値を引き継ぐ
    override fun onSaveInstanceState(): Parcelable? {
        super.onSaveInstanceState()
        return Bundle().apply {
            putSerializable("list", danmakuList)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        danmakuList = (state as Bundle).getSerializable("list") as ArrayList<Pair<Int, Int>>
    }
*/

}