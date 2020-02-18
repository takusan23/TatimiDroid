package io.github.takusan23.commentcanvas

import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.method.ReplacementTransformationMethod
import android.util.ArrayMap
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.CommentJSONParse
import org.w3c.dom.Comment
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.random.Random


class CommentCanvas(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    //白色テキストPaint
    var paint: Paint
    //白色テキストの下に描画する黒色テキストPaint
    var blackPaint: Paint

    var fontsize = 100f

    // var lineCount = 10f

    // コメントの配列
    val commentObjList = arrayListOf<CommentObject>()

    // コメントのライン。行？。keyは高さ、valueはコメントの位置。決して空きスペースではない。
    /*
    *  |----ディスプレイ----|
    *  |  wwwww             | ←これがKey。高さ。
    *         ↑個々の位置がValue。
    *
    * */
    val lineList = mutableMapOf<Float, Long>() // コメントの高さ、追加時間(UnixTime)

    init {
        //文字サイズ計算。端末によって変わるので
        fontsize = 20 * resources.displayMetrics.scaledDensity
        //白色テキスト
        paint = Paint()
        paint.isAntiAlias = true
        paint.textSize = fontsize
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#ffffff")

        //黒色テキスト
        blackPaint = Paint()
        blackPaint.isAntiAlias = true
        blackPaint.strokeWidth = 2.0f
        blackPaint.style = Paint.Style.STROKE
        blackPaint.textSize = fontsize
        blackPaint.color = Color.parseColor("#000000")

        //lineCount = height / fontsize

        //コメントの流れる速度
        val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        val speed = pref_setting.getString("setting_comment_speed", "5")?.toInt() ?: 5


        // コメント移動
        Timer().schedule(timerTask {

            // コメント移動。
            for (i in 0 until commentObjList.size) {
                commentObjList[i].xPos -= speed
            }

/*
            // コメントの移動に合わせて各行のコメントの位置も移動させる。
            for (i in 0 until lineList.size) {
                val line = lineList.toList()[i].first
                val space = lineList.toList()[i].second
                if (space > 0f) {
                    lineList[line] = space - 5f
                }
            }
*/

            // 再描画
            postInvalidate()
        }, 10, 10)

        // getHeightが0を返すので一手間加える
        viewTreeObserver.addOnGlobalLayoutListener {
            // Canvasの縦の長さとフォントサイズで割り算して何行書けるか計算して予めMapに入れておく
            // 初期値は0である。
            /*
             *  |----ディスプレイ----|
             *  |                    |
             *   ↑初期は0にする。ここ。すべての行を0にする。
             * */
            val initUnixTime = System.currentTimeMillis()
            val heightLineSize = (height / fontsize).toInt()
            for (i in 1..heightLineSize) {
                lineList[fontsize * i] = initUnixTime
            }
        }

    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        // コメントを描画する。
        commentObjList.forEach {
            canvas?.drawText(it.comment, it.xPos, it.yPos, blackPaint)
            canvas?.drawText(it.comment, it.xPos, it.yPos, getCommentTextPaint(it.commentJSONParse.mail))
        }
    }

    var oldComment = ""

    var theBottomYPos = fontsize

    /**
     * 最重要関数。
     * */
    fun postComment(comment: String, commentJSONParse: CommentJSONParse) {
        // 縦。ここがすごく大変だった。何段目（行）に入れるか比較してみていくため。
        var yPos = fontsize
        // よこ。paint.measureText()で描画するときの文字の幅が取得可能である。
        val xPos = width.toFloat()
        // これから書くコメントの大きさ。上記の方法で描画するときの幅を取得している。
        val commentLength = blackPaint.measureText(comment)

        var check = false

        val nowUnixTime = System.currentTimeMillis()

        var returnPos = 0

        var oldTextLength = 0

        // ここから何段目に入れるかどうかを決める。
        // 行が指定されていない場合はtrue
        // 行が入ってるMapをfor文で見ていく。
        for (i in 0 until lineList.size) {
            // 高さ。何行目に入れるかはここで
            val line = lineList.toList()[i].first

            val lineUnix = lineList.toList()[i].second + comment.length * 100 // 何故か掛け算するとうまくいく。

            // println("たかさ：$line / 前のUnixTime：$lineUnix / 現在のUnixTime：$nowUnixTime / 比較：${lineUnix < nowUnixTime} / ひきざん：${nowUnixTime - lineUnix}")

            if (!check) {
                if (lineUnix < nowUnixTime) {
                    // 次の行へ
                    check = true
                    returnPos = i + 1
                    yPos = returnPos * fontsize
                    if (theBottomYPos < yPos) {
                        theBottomYPos = yPos
                    }
                } else {
                    // すべての段で計算結果がマイナスになる場合＝＝重なるとき。
                    var allCalc = false
                    val calc = nowUnixTime - lineUnix
                    for (x in 0 until lineList.size) {
                        val lineUnix = lineList.toList()[x].second + comment.length * 100
                        allCalc = (nowUnixTime - lineUnix == calc)
                    }
                    if (allCalc) {
                        // すべて同じだったとき
                        Toast.makeText(context, "すべて同じ", Toast.LENGTH_SHORT).show()
                        // 最大値を利用する
                        //yPos = theBottomYPos + fontsize
                        //theBottomYPos = yPos
                        // 仕方ないのでランダムで
                        yPos = (Random.nextInt(1, lineList.size) * fontsize)
                    }
                }
            }
        }

        lineList[yPos] = nowUnixTime

        //println("いち：$yPos / Xpos：$xPos")
        //println("一番下：$theBottomYPos")


        // 内容を更新する。なおここで入れたコメントの位置は上の方のforEachで引いていく。
        /*
        * |----ディスプレイ----|
        * |              wwwwww|　ここの行（段）のコメントの位置を
        *                     ↑コメントの位置を更新する。
        * */
        //lineList[yPos] = System.currentTimeMillis() / 1000L

        //println(lineList.toList())

        // コメント追加。
        val commentObject =
            CommentObject(comment, xPos, yPos, System.currentTimeMillis(),commentJSONParse)
        commentObjList.add(commentObject)
    }

    //色の変更
    fun getCommentTextPaint(command: String): Paint {
        //白色テキスト
        val paint = Paint()
        paint.isAntiAlias = true
        paint.textSize = fontsize
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor(getColor(command))

        return paint
    }

    //色
    fun getColor(command: String): String {
        if (command.contains("red")) {
            return "#FF0000"
        }
        if (command.contains("pink")) {
            return "#FF8080"
        }
        if (command.contains("orange")) {
            return "#FFC000"
        }
        if (command.contains("yellow")) {
            return "#FFFF00"
        }
        if (command.contains("green")) {
            return "#00FF00"
        }
        if (command.contains("cyan")) {
            return "#00FFFF"
        }
        if (command.contains("blue")) {
            return "#0000FF"
        }
        if (command.contains("purple")) {
            return "#C000FF"
        }
        if (command.contains("black")) {
            return "#000000"
        }
        return "#FFFFFF"
    }

}

data class CommentObject(val comment: String, var xPos: Float, var yPos: Float, var unixTime: Long,var commentJSONParse: CommentJSONParse)

data class CommentDrawObject(var space: Float, var comment: String)