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
import androidx.preference.PreferenceManager
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

    // コメントの色を部屋の色にする設定が有効ならtrue
    var isCommentColorRoom = false

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

    var commentLines = arrayListOf<Long>()

    // コメントの配列
    val commentObjList = arrayListOf<CommentObject>()

    // 高さ：横の位置
    val commentLine = mutableMapOf<Float, CommentObject>()

    // 上付きコメントの配列
    val ueCommentList = arrayListOf<CommentObject>()

    // 高さ：追加時間（UnixTime）
    val ueCommentLine = mutableMapOf<Float, Long>()

    // 下付きコメントの配列
    val sitaCommentList = arrayListOf<CommentObject>()

    // 高さ：追加時間（UnixTime）
    val sitaCommentLine = mutableMapOf<Float, Long>()

    // Canvasの高さ。なぜかgetHeight()が0を返すので一工夫する必要がある。くっっっっっっそ
    var finalHeight = 10

    // ポップアップ再生時はtrue
    var isPopupView = false

    // コメントを流さないときはtrue
    var isPause = false

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

        //コメントの流れる速度
        val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        val speed = pref_setting.getString("setting_comment_speed", "5")?.toInt() ?: 5
        // コメントキャンバスの更新頻度
        val update = pref_setting.getString("setting_comment_canvas_timer", "10")?.toLong() ?: 10
        // コメントの色を部屋の色にする設定が有効ならtrue
        isCommentColorRoom = pref_setting.getBoolean("setting_command_room_color", false)

        Timer().schedule(update, update) {

            // コメント移動止めるやつ
            if (isPause) {
                return@schedule
            }

            // コメントを移動させる
            for (i in 0 until commentObjList.size) {
                val obj = commentObjList[i]
                if (obj.asciiArt) {
                    // AAの場合は速度を固定する
                    if ((obj.xPos + obj.commentMeasure) > 0) {
                        commentObjList[i].xPos -= speed
                    }
                } else {
                    if ((obj.xPos + obj.commentMeasure) > 0) {
                        commentObjList[i].xPos -= speed + (obj.comment.length / 4)
                    }
                }
            }

            val nowUnixTime = System.currentTimeMillis()
            // toList() を使って forEach すればエラー回避できます
            // 3秒経過したら配列から消す
            ueCommentList.toList().forEach {
                for (i in 0 until ueCommentList.size) {
                    if (nowUnixTime - it.unixTime > 3000) {
                        ueCommentList.remove(it)
                    }
                }
            }
            sitaCommentList.toList().forEach {
                for (i in 0 until sitaCommentList.size) {
                    if (nowUnixTime - it.unixTime > 3000) {
                        sitaCommentList.remove(it)
                    }
                }
            }

            // 再描画
            postInvalidate()
        }

        viewTreeObserver.addOnGlobalLayoutListener {
            finalHeight = height
            val lineCount = height / fontsize
            for (i in 0 until lineCount.toInt()) {
                commentLines.add(0L)
            }
        }

    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // コメントを描画する。
        for (i in 0 until commentObjList.size) {
            val obj = commentObjList[i]
            if ((obj.xPos + obj.commentMeasure) > 0) {
                canvas?.drawText(obj.comment, obj.xPos, obj.yPos, blackPaint)
                canvas?.drawText(obj.comment, obj.xPos, obj.yPos, getCommentTextPaint(obj.command))
            }
        }
        // 上付きコメントを描画する
        ueCommentList.toList().forEach {
            canvas?.drawText(it.comment, it.xPos, it.yPos, blackPaint)
            canvas?.drawText(it.comment, it.xPos, it.yPos, getCommentTextPaint(it.command))
        }
        // 下付きコメントを描画する
        sitaCommentList.toList().forEach {
            canvas?.drawText(it.comment, it.xPos, it.yPos, blackPaint)
            canvas?.drawText(it.comment, it.xPos, it.yPos, getCommentTextPaint(it.command))
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
        // コメントの色を部屋の色にする機能が有効になっている場合
        if (isCommentColorRoom) {
            paint.color = getRoomColor(command)
        }
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

    //コメビュの部屋の色。NCVに追従する
    fun getRoomColor(command: String): Int {
        if (command.contains("アリーナ")) {
            return Color.argb(255, 0, 153, 229)
        }
        if (command.contains("立ち見1")) {
            return Color.argb(255, 234, 90, 61)
        }
        if (command.contains("立ち見2")) {
            return Color.argb(255, 172, 209, 94)
        }
        if (command.contains("立ち見3")) {
            return Color.argb(255, 0, 217, 181)
        }
        if (command.contains("立ち見4")) {
            return Color.argb(255, 229, 191, 0)
        }
        if (command.contains("立ち見5")) {
            return Color.argb(255, 235, 103, 169)
        }
        if (command.contains("立ち見6")) {
            return Color.argb(255, 181, 89, 217)
        }
        if (command.contains("立ち見7")) {
            return Color.argb(255, 20, 109, 199)
        }
        if (command.contains("立ち見8")) {
            return Color.argb(255, 226, 64, 33)
        }
        if (command.contains("立ち見9")) {
            return Color.argb(255, 142, 193, 51)
        }
        if (command.contains("立ち見10")) {
            return Color.argb(255, 0, 189, 120)
        }
        return Color.parseColor("#ffffff")
    }


    /**
     * コメント投稿
     * @param asciiArt アスキーアートのときはtrueにすると速度が一定になります
     * */
    fun postComment(comment: String, commentJSONParse: CommentJSONParse, asciiArt: Boolean = false) {
        // ポップアップ再生時はフォントサイズを小さく
        if (isPopupView) {
            fontsize = (finalHeight / 10).toFloat()
            blackPaint.textSize = fontsize
        } else {
            fontsize = 20 * resources.displayMetrics.scaledDensity
            blackPaint.textSize = fontsize
        }
        // 生主/運営のコメントは無視する
        if (commentJSONParse.premium == "生主" || commentJSONParse.premium == "運営") {
            return
        }
        val measure = paint.measureText(comment)
        var xPos = width.toFloat()
        var yPos = fontsize
        val nowUnixTime = System.currentTimeMillis()
        val command = commentJSONParse.mail

        // 上でもなければ下でもないときは流す
        if (!command.contains("ue") && !command.contains("shita")) {
            // 流れるコメント
            for (i in 0 until commentLine.size) {
                val obj = commentLine.toList().get(i).second
                val space = width - obj.xPos
                // println(space)
                yPos = obj.yPos
                if (space < -1) {
                    // 画面外。負の値
                    // println("画面外です")
                    yPos += fontsize
                } else if (space < measure) {
                    // 空きスペースよりコメントの長さが大きいとき
                    // println("コメントのほうが長いです")
                    yPos += fontsize
                } else {
                    // 位置決定
                    // println("位置が決定しました")
                    break
                }
                if (yPos > finalHeight) {
                    // 画面外に行く場合はランダムで決定
                    if (finalHeight > 0 && fontsize.toInt() < finalHeight) {
                        // Canvasの高さが取得できているとき
                        yPos = Random.nextInt(fontsize.toInt(), finalHeight).toFloat()
                    } else {
                        // 取得できてないとき。ほんとに適当
                        yPos = Random.nextInt(1, 10) * fontsize
                    }
                    // println("らんだむ")
                }
            }
            val commentObj = CommentObject(
                comment,
                xPos,
                yPos,
                System.currentTimeMillis(),
                measure,
                command,
                asciiArt
            )
            commentObjList.add(commentObj)
            commentLine[yPos] = commentObj
        } else if (command.contains("ue")) {
            // 上付きコメント
            var yPos = fontsize
            // 位置決定
            for (i in 0 until ueCommentLine.size) {
                // 空きがあればそこに入れる
                val unix = ueCommentLine.toList().get(i).second
                if ((nowUnixTime - unix) > 3000) {
                    yPos = (i + 1) * fontsize
                    break
                } else {
                    yPos = (i + 1) * fontsize + fontsize
                }
            }
            val commentObj =
                CommentObject(
                    comment,
                    ((width.toFloat() - measure) / 2),
                    yPos,
                    System.currentTimeMillis(),
                    measure,
                    command,
                    asciiArt
                )
            ueCommentList.add(commentObj)
            ueCommentLine[yPos] = System.currentTimeMillis()
        } else if (command.contains("shita")) {
            // 下付きコメント
            var yPos = height - fontsize
            // 位置決定
            for (i in 0 until sitaCommentLine.size) {
                // 空きがあればそこに入れる
                val unix = sitaCommentLine.toList().get(i).second
                if ((nowUnixTime - unix) > 3000) {
                    yPos = height - ((i + 1) * fontsize)
                    break
                } else {
                    yPos = height - ((i + 1) * fontsize + fontsize)
                }
            }
            val commentObj =
                CommentObject(
                    comment,
                    ((width.toFloat() - measure) / 2),
                    yPos,
                    System.currentTimeMillis(),
                    measure,
                    command,
                    asciiArt
                )
            sitaCommentList.add(commentObj)
            sitaCommentLine[yPos] = System.currentTimeMillis()
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

        for (i in 0 until commentLines.size) {
            //println(posList)
            //UnixTimeで管理してるので。。
            val nowUnixTime = System.currentTimeMillis() / 1000
            val pos = commentLines[i]
            val tmp = nowUnixTime - pos
            posMinusList.add(tmp)
            if (!check) {
                if (pos < nowUnixTime) {
                    check = true
                    commentLines[i] = nowUnixTime
                    //commentPosition_1 = posList[0]
                    //commentPosition_2 = posList[1]
                    //commentPosition_3 = posList[2]
                    //commentPosition_4 = posList[3]
                    //commentPosition_5 = posList[4]
                    //commentPosition_6 = posList[5]
                    //commentPosition_7 = posList[6]
                    //commentPosition_8 = posList[7]
                    //commentPosition_9 = posList[8]
                    //commentPosition_10 = posList[9]
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

    data class CommentObject(
        val comment: String,
        var xPos: Float,
        var yPos: Float,
        var unixTime: Long,
        var commentMeasure: Float,
        var command: String,
        var asciiArt: Boolean = false
    )

}