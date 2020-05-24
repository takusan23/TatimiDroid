package io.github.takusan23.tatimidroid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceManager
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToInt
import kotlin.random.Random

class CommentCanvas(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

/*
    //白色テキストPaint
    lateinit var paint: Paint

    //白色テキストの下に描画する黒色テキストPaint
    lateinit var blackPaint: Paint
    val textList = arrayListOf<String>()
*/

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

    var commentLines = arrayListOf<Long>()

    // コメントの配列
    val commentObjList = arrayListOf<CommentObject>()

    /**
     * 高さ：横の位置
     * 注意：コメントのフォントサイズ変更時はこの配列の中身をすべて消す必要があるよ。
     * コメントサイズ変更してもこの配列から最初の縦の位置（コメントフォントサイズ）を決定するので一行目のコメント描画で見切れる文字が発生する。
     * */
    val commentLine = mutableMapOf<Float, CommentObject>()

    // 上付きコメントの配列
    val ueCommentList = arrayListOf<CommentObject>()

    // 高さ：追加時間（UnixTime）
    val ueCommentLine = mutableMapOf<Float, CommentObject>()

    // 下付きコメントの配列
    val sitaCommentList = arrayListOf<CommentObject>()

    // 高さ：追加時間（UnixTime）
    val sitaCommentLine = mutableMapOf<Float, CommentObject>()

    // Canvasの高さ。なぜかgetHeight()が0を返すので一工夫する必要がある。くっっっっっっそ
    var finalHeight = 10

    // ポップアップ再生時はtrue
    var isPopupView = false

    // コメントを流さないときはtrue
    var isPause = false

    // 10行確保モード
    var isTenLineSetting = false

    // コメント行を自由に変更可能
    var isCustomCommentLine = false
    var customCommentLine = 10

    // 透明度の設定（重そう小並感）
    var commentAlpha = 1.0F

    init {
        //文字サイズ計算。端末によって変わるので
        fontsize = 20 * resources.displayMetrics.scaledDensity
/*
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
*/

        val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        //コメントの流れる速度
        val speed = pref_setting.getString("setting_comment_speed", "5")?.toInt() ?: 5
        // コメントキャンバスの更新頻度
        val update = pref_setting.getString("setting_comment_canvas_timer", "10")?.toLong() ?: 10
        // コメントの透明度
        commentAlpha = pref_setting.getString("setting_comment_alpha", "1.0")?.toFloat() ?: 1.0F
        // コメントの行を最低10行確保するモード
        isTenLineSetting = pref_setting.getBoolean("setting_comment_canvas_10_line", false)
        // コメントの色を部屋の色にする設定が有効ならtrue
        isCommentColorRoom = pref_setting.getBoolean("setting_command_room_color", false)
        // コメント行を自由に設定する設定
        isCustomCommentLine =
            pref_setting.getBoolean("setting_comment_canvas_custom_line_use", false)
        customCommentLine =
            pref_setting.getString("setting_comment_canvas_custom_line_value", "10")?.toInt() ?: 20

        Timer().schedule(update, update) {

            // コメント移動止めるやつ
            if (isPause) {
                return@schedule
            }

            // コメントを移動させる
            for (i in 0 until commentObjList.size) {
                if (commentObjList[i] != null) {
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
                when {
                    obj.command.contains("big") -> {
                        val fontSize = (fontsize * 1.3).toFloat()
                        canvas?.drawText(obj.comment, obj.xPos, obj.yPos, getBlackCommentTextPaint(fontSize))
                        canvas?.drawText(obj.comment, obj.xPos, obj.yPos, getCommentTextPaint(obj.command, fontSize))
                    }
                    obj.command.contains("small") -> {
                        val fontSize = (fontsize * 0.8).toFloat()
                        canvas?.drawText(obj.comment, obj.xPos, obj.yPos, getBlackCommentTextPaint(fontSize))
                        canvas?.drawText(obj.comment, obj.xPos, obj.yPos, getCommentTextPaint(obj.command, fontSize))
                    }
                    else -> {
                        canvas?.drawText(obj.comment, obj.xPos, obj.yPos, getBlackCommentTextPaint(fontsize))
                        canvas?.drawText(obj.comment, obj.xPos, obj.yPos, getCommentTextPaint(obj.command))
                    }
                }
            }
        }
        // 上付きコメントを描画する
        ueCommentList.toList().forEach {
            if (it?.command != null && it?.comment != null) {
                canvas?.drawText(it.comment, it.xPos, it.yPos, getBlackCommentTextPaint(it.fontSize))
                canvas?.drawText(it.comment, it.xPos, it.yPos, getCommentTextPaint(it.command, it.fontSize))
            }
        }
        // 下付きコメントを描画する
        sitaCommentList.toList().forEach {
            if (it?.command != null && it?.comment != null) {
                canvas?.drawText(it.comment, it.xPos, it.yPos, getBlackCommentTextPaint(it.fontSize))
                canvas?.drawText(it.comment, it.xPos, it.yPos, getCommentTextPaint(it.command, it.fontSize))
            }
        }
    }


    // 色の変更
    fun getCommentTextPaint(command: String, fontSize: Float = this.fontsize): Paint {
        //白色テキスト
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = fontSize
            style = Paint.Style.FILL
            color = Color.parseColor(getColor(command))
            alpha = (commentAlpha * 225).toInt() // 0 ~ 225 の範囲で指定するため 225かける
        }
        return paint
    }

    // 枠取り文字の文字描画
    fun getBlackCommentTextPaint(fontSize: Float = this.fontsize): Paint {
        //黒色テキスト
        val blackPaint = Paint().apply {
            isAntiAlias = true
            strokeWidth = 2.0f
            style = Paint.Style.STROKE
            textSize = fontSize
            color = Color.parseColor("#000000")
            alpha = (commentAlpha * 225).toInt() // 0 ~ 225 の範囲で指定するため 225かける
        }
        return blackPaint
    }

    // 色
    // 大百科参照：https://dic.nicovideo.jp/a/%E3%82%B3%E3%83%A1%E3%83%B3%E3%83%88
    fun getColor(command: String): String {
        return when {
            // プレ垢限定色。
            command.contains("white2") -> "#CCCC99"
            command.contains("red2") -> "#CC0033"
            command.contains("pink2") -> "#FF33CC"
            command.contains("orange2") -> "#FF6600"
            command.contains("yellow2") -> "#999900"
            command.contains("green2") -> "#00CC66"
            command.contains("cyan2") -> "#00CCCC"
            command.contains("blue2") -> "#3399FF"
            command.contains("purple2") -> "#6633CC"
            command.contains("black2") -> "#666666"
            // 一般でも使えるやつ
            command.contains("red") -> "#FF0000"
            command.contains("pink") -> "#FF8080"
            command.contains("orange") -> "#FFC000"
            command.contains("yellow") -> "#FFFF00"
            command.contains("green") -> "#00FF00"
            command.contains("cyan") -> "#00FFFF"
            command.contains("blue") -> "#0000FF"
            command.contains("purple") -> "#C000FF"
            command.contains("black") -> "#000000"
            // その他
            else -> "#ffffff"
        }
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
        when {
            // コメント行をカスタマイズしてるとき
            isCustomCommentLine -> {
                fontsize = (finalHeight / customCommentLine).toFloat()
                // blackPaint.textSize = fontsize
            }
            // ポップアップ再生 / 10行コメント確保ーモード時
            isPopupView || isTenLineSetting -> {
                fontsize = (finalHeight / 10).toFloat()
                // blackPaint.textSize = fontsize
            }
            else -> {
                fontsize = 20 * resources.displayMetrics.scaledDensity
                // blackPaint.textSize = fontsize
            }
        }
        // 生主/運営のコメントは無視する
        if (commentJSONParse.premium == "生主" || commentJSONParse.premium == "運営") {
            return
        }
        // コマンドで指定されたフォントサイズを
        // big->1.3倍
        // small->0.8倍
        var commandFontSize = when {
            commentJSONParse.mail.contains("big") -> {
                (fontsize * 1.3).toFloat()
            }
            commentJSONParse.mail.contains("small") -> {
                (fontsize * 0.8).toFloat()
            }
            else -> fontsize
        }
        // コマンドで指定されたサイズで作成したPaintでコメントの幅計算
        var measure =
            getCommentTextPaint(commentJSONParse.comment, commandFontSize).measureText(comment)
        var xPos = width.toFloat()
        var yPos = fontsize
        val nowUnixTime = System.currentTimeMillis()
        val command = commentJSONParse.mail

        val tmpCommand = command
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
                    yPos += commandFontSize
                } else if (space < measure) {
                    // 空きスペースよりコメントの長さが大きいとき
                    // println("コメントのほうが長いです")
                    yPos += commandFontSize
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
            val commentObj =
                CommentObject(comment, xPos, yPos, System.currentTimeMillis(), measure, command, asciiArt, fontsize)
            commentObjList.add(commentObj)
            commentLine[yPos] = commentObj
        } else if (command.contains("ue") && tmpCommand.replace("blue|blue([0-9])".toRegex(), "")
                .contains("ue")
        ) {
            var fontSize = when {
                command.contains("big") -> fontsize * 1.3
                command.contains("small") -> fontsize * 0.8
                else -> fontsize.toDouble()
            }
            // コメントが入り切らないとき
            if (measure > width) {
                fontSize = (width / comment.length).toDouble()
                measure = width.toFloat()
            }
            commandFontSize = fontSize.toFloat()
            var yPos = commandFontSize
            for (i in 0 until ueCommentLine.size) {
                // みていく
                val obj = ueCommentLine.toList()[i].second
                val unix = obj.unixTime
                if (nowUnixTime - unix > 3000) {
                    // スペースある？
                    if (obj.yPos >= commandFontSize) {
                        yPos = ueCommentLine.toList()[i].first
                        break
                    } else {
                        yPos = ueCommentLine.toList()[i].first + commandFontSize
                    }
                } else {
                    yPos = ueCommentLine.toList()[i].first + commandFontSize
                }
            }
            // 画面外にいったらランダムな位置に配置する
            if (yPos > finalHeight) {
                val random = Random.nextInt(1, (finalHeight / fontSize.roundToInt()))
                yPos = (random * fontSize).toFloat()
            }
            val commentObj =
                CommentObject(
                    comment,
                    ((width.toFloat() - measure) / 2),
                    yPos,
                    System.currentTimeMillis(),
                    measure,
                    command,
                    asciiArt,
                    fontSize.toFloat()
                )
            ueCommentList.add(commentObj)
            ueCommentLine[yPos] = commentObj
        } else if (command.contains("shita")) {
            // フォントサイズ
            var fontSize = when {
                command.contains("big") -> fontsize * 1.3
                command.contains("small") -> fontsize * 0.8
                else -> fontsize.toDouble()
            }
            // コメントが入り切らないとき
            if (measure > width) {
                fontSize = (width / comment.length).toDouble()
                measure = width.toFloat()
            }
            commandFontSize = fontSize.toFloat()
            var yPos = finalHeight.toFloat() - 10
            for (i in 0 until sitaCommentLine.size) {
                // みていく
                val obj = sitaCommentLine.toList()[i].second
                val unix = obj.unixTime
                if (nowUnixTime - unix > 3000) {
                    // スペースある？
                    if (sitaCommentLine.toList()[i].first - commandFontSize >= commandFontSize) {
                        yPos = sitaCommentLine.toList()[i].first
                        break
                    } else {
                        yPos = sitaCommentLine.toList()[i].first - commandFontSize
                    }
                } else {
                    yPos = sitaCommentLine.toList()[i].first - commandFontSize
                }
            }
            // マイナスにいったらランダムな位置に配置する
            if (yPos < fontSize) {
                val random = Random.nextInt(1, (finalHeight / fontSize.roundToInt()))
                yPos = (random * fontSize).toFloat()
            }
            val commentObj = CommentObject(
                comment,
                ((width.toFloat() - measure) / 2),
                yPos,
                System.currentTimeMillis(),
                measure,
                command,
                asciiArt,
                fontSize.toFloat()
            )
            sitaCommentList.add(commentObj)
            sitaCommentLine[yPos] = commentObj
        } else {
            // 流れるコメント
            for (i in 0 until commentLine.size) {
                val obj = commentLine.toList().get(i).second
                val space = width - obj.xPos
                // println(space)
                yPos = obj.yPos
                if (space < -1) {
                    // 画面外。負の値
                    // println("画面外です")
                    yPos += commandFontSize
                } else if (space < measure) {
                    // 空きスペースよりコメントの長さが大きいとき
                    // println("コメントのほうが長いです")
                    yPos += commandFontSize
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
                asciiArt,
                fontsize
            )
            commentObjList.add(commentObj)
            commentLine[yPos] = commentObj
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
        var asciiArt: Boolean = false,
        var fontSize: Float
    )

}