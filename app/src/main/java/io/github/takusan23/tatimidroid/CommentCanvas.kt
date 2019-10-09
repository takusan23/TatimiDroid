package io.github.takusan23.tatimidroid

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import kotlin.concurrent.schedule
import kotlin.random.Random

class CommentCanvas(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    //白色テキストPaint
    lateinit var paint: Paint
    //白色テキストの下に描画する黒色テキストPaint
    lateinit var blackPaint: Paint
    val textList = arrayListOf<String>()
    //座標？
    val xList = arrayListOf<Int>()
    val yList = arrayListOf<Int>()
    //色とか
    val commandList = arrayListOf<String>()

    //いまコメントが流れてる座標を保存する
    val commentFlowingXList = arrayListOf<Int>()
    val commentFlowingYList = arrayListOf<Int>()

    var fontsize = 40F

    //コメントのレールの配列を入れるための配列
    val commentPositionList = arrayListOf<ArrayList<Long>>()

    //レールの配列。1から10まで用意したけど使わないと思う。
    //コメントが入った時間をそれぞれのレーンに入れて管理する
    val commentPositionListOne = arrayListOf<Long>()
    val commentPositionListTwo = arrayListOf<Long>()
    val commentPositionListThree = arrayListOf<Long>()
    val commentPositionListFour = arrayListOf<Long>()
    val commentPositionListFive = arrayListOf<Long>()
    val commentPositionListSix = arrayListOf<Long>()
    val commentPositionListSeven = arrayListOf<Long>()
    val commentPositionListEight = arrayListOf<Long>()
    val commentPositionListNine = arrayListOf<Long>()
    val commentPositionListTen = arrayListOf<Long>()

    //コメントの描画改善
    //別に配列にする意味なくね？
    var commentPosition_1 = 0L
    var commentPosition_2 = 0L
    var commentPosition_3 = 0L
    var commentPosition_4 = 0L
    var commentPosition_5 = 0L
    var commentPosition_6 = 0L
    var commentPosition_7 = 0L
    var commentPosition_8 = 0L
    var commentPosition_9 = 0L
    var commentPosition_10 = 0L

    //フローティング表示
    var isFloatingView = false

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

        Timer().schedule(10, 10) {
            for (i in 0..(xList.size - 1)) {
                //文字数が多い場合はもっと早く流す
                val minus = 5 + (textList.get(i).length / 2)
                val x = xList.get(i) - minus
                if (x > -2000) {
                    xList.set(i, x)
                }
            }
            postInvalidate()
        }
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        commentFlowingXList.clear()
        commentFlowingYList.clear()
        for (i in 0..(xList.size - 1)) {
            val text = textList.get(i)
            val x = xList.get(i)
            val y = yList.get(i)
            val command = commandList.get(i)
            if (x > -1000) {
                commentFlowingXList.add(x)
                commentFlowingYList.add(y)
                canvas?.drawText(text, x.toFloat(), y.toFloat(), blackPaint)
                canvas?.drawText(text, x.toFloat(), y.toFloat(), getCommentTextPaint(command))
            }
        }
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

    /*
    * コメント投稿
    * */

    fun postComment(comment: String, command: String) {
        //フローティングモードのときは計算する
        if (isFloatingView) {
            fontsize = (height / 10).toFloat()
            paint.textSize = fontsize
            blackPaint.textSize = fontsize
        }
        if (this@CommentCanvas::paint.isInitialized) {
            val display = (context as AppCompatActivity).getWindowManager().getDefaultDisplay()
            val point = Point()
            display.getSize(point)

            //縦、横で開始位置を調整
            var weight = point.x
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //横。幅を割る2する
                weight = point.x / 2
            } else {
                //縦。そのまま
                weight = point.x
            }

            weight = this@CommentCanvas.width

            textList.add(comment)
            xList.add(weight)
            yList.add(getCommentPosition(comment))
            commandList.add(command)
        }
    }

    fun getCommentPosition(comment: String): Int {

        /*
        *
        * コメントの位置を取り出すやーつ
        *
        * コメントの流れた時間(UnixTime)を変数に入れておいて
        * 使いやすいように配列に入れて
        *
        * 時間と今のUnixTimeを比較して今のほうが大きかったら
        * 配列の位置のUnixTimeを置き換えます。
        *
        * あと配列→変数へ
        *
        * それと時間とUnixTimeを引いたときの値も配列に入れています。
        * その配列から0以上の時間があいていればその場所にコメントが流れます。
        *
        * */

        //配列に入れる。
        val posList = arrayListOf(
            commentPosition_1,
            commentPosition_2,
            commentPosition_3,
            commentPosition_4,
            commentPosition_5,
            commentPosition_6,
            commentPosition_7,
            commentPosition_8,
            commentPosition_9,
            commentPosition_10
        )

        var check = false

        var commentY = 100

        //コメント感覚。<--->
        //値が大きければどんどん下に表示される
        val timeSpace = 5000

        val posMinusList = arrayListOf<Long>()

        for (i in 0 until posList.size) {
            //println(posList)
            //UnixTimeで管理してるので。。
            val nowUnixTime = System.currentTimeMillis() / 1000
            val pos = posList[i]
            val tmp = nowUnixTime - pos
            posMinusList.add(tmp)
            if (!check) {
                if (pos < nowUnixTime) {
                    check = true
                    posList[i] = nowUnixTime
                    commentPosition_1 = posList[0]
                    commentPosition_2 = posList[1]
                    commentPosition_3 = posList[2]
                    commentPosition_4 = posList[3]
                    commentPosition_5 = posList[4]
                    commentPosition_6 = posList[5]
                    commentPosition_7 = posList[6]
                    commentPosition_8 = posList[7]
                    commentPosition_9 = posList[8]
                    commentPosition_10 = posList[9]
                }
            }
        }

        //コメントの位置を決定する
        var tmpFindZero = 10L
        var result = 0
        for (l in 0 until posMinusList.size) {
            val pos = posMinusList[l]
            if (pos > 0L) {
                if (tmpFindZero > pos) {
                    tmpFindZero = pos
                    result = l
                }
            } else {
                //少しでも被らないように？
                result = Random.nextInt(1, 10)
            }
        }
        commentY = returnNumberList(result)

/*
        for (index in 0 until commentPositionList.size) {

            val list = commentPositionList.get(index)

            if (!check) {
                if (list.size > 0) {
                    val tmp = list.get(list.size - 1)
                    val calc = System.currentTimeMillis() - tmp
                    if (calc < 5000) {
                        //今の時間と比較して1秒経過してれば2段目に入れる
                        if (calc > timeSpace) {
                            check = true
                            commentY = returnNumberList(index)
                            list.add(System.currentTimeMillis())
                        } else {
                            //ランダムで配置
                            //commentY = returnNumberList((1 until 10).random())
                        }
                    } else {
                        //一定期間（5秒？）コメントがないときは一段目に入れる
                        commentY = 100
                        commentPositionListOne.clear()
                        //commentPositionListOne.add(System.currentTimeMillis())


                        commentPosition_1 = 0
                        commentPosition_2 = 0
                        commentPosition_3 = 0
                        commentPosition_4 = 0
                        commentPosition_5 = 0
                        commentPosition_6 = 0
                        commentPosition_7 = 0
                        commentPosition_8 = 0
                        commentPosition_9 = 0
                        commentPosition_10 = 0

                        //一定期間（5秒）コメントがなかったら配列の中身もクリアに
                        //理由は経過時間の計算がおかしくなるからです。
                        commentPositionList.clear()
                        commentPositionListTwo.clear()
                        commentPositionListThree.clear()
                        commentPositionListFour.clear()
                        commentPositionListFive.clear()
                        commentPositionListSix.clear()
                        commentPositionListSeven.clear()
                        commentPositionListEight.clear()
                        commentPositionListNine.clear()
                        commentPositionListTen.clear()

                        commentPositionList.add(commentPositionListOne)
                        commentPositionList.add(commentPositionListTwo)
                        commentPositionList.add(commentPositionListThree)
                        commentPositionList.add(commentPositionListFour)
                        commentPositionList.add(commentPositionListFive)
                        commentPositionList.add(commentPositionListSix)
                        commentPositionList.add(commentPositionListSeven)
                        commentPositionList.add(commentPositionListEight)
                        commentPositionList.add(commentPositionListNine)
                        commentPositionList.add(commentPositionListTen)
                    }
                } else {
                    commentY = 100
                    list.add(System.currentTimeMillis())
                }
            }
        }
*/
        return commentY
    }


    fun returnNumberList(pos: Int): Int {
        var size = (fontsize).toInt()
        when (pos) {
            1 -> size = (fontsize).toInt()
            2 -> size = (fontsize * 2).toInt()
            3 -> size = (fontsize * 3).toInt()
            4 -> size = (fontsize * 4).toInt()
            5 -> size = (fontsize * 5).toInt()
            6 -> size = (fontsize * 6).toInt()
            7 -> size = (fontsize * 7).toInt()
            8 -> size = (fontsize * 8).toInt()
            9 -> size = (fontsize * 9).toInt()
            10 -> size = (fontsize * 10).toInt()
        }
        return size
    }

}