package io.github.takusan23.tatimidroid

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.preference.PreferenceManager
import com.google.android.gms.common.FirstPartyScopes
import kotlinx.coroutines.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.random.Random

class CommentCanvasSurfaceView(context: Context?, attrs: AttributeSet?) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    //白色テキストPaint
    var paint: Paint

    //白色テキストの下に描画する黒色テキストPaint
    var blackPaint: Paint

    var fontsize = 40F

    var commentLines = arrayListOf<Long>()

    // コメントの配列
    val commentObjList = arrayListOf<CommentCanvas.CommentObject>()

    /**
     * 高さ：横の位置
     * 注意：コメントのフォントサイズ変更時はこの配列の中身をすべて消す必要があるよ。
     * コメントサイズ変更してもこの配列から最初の縦の位置（コメントフォントサイズ）を決定するので一行目のコメント描画で見切れる文字が発生する。
     * */
    val commentLine = mutableMapOf<Float, CommentCanvas.CommentObject>()

    // 上付きコメントの配列
    val ueCommentList = arrayListOf<CommentCanvas.CommentObject>()

    // 高さ：追加時間（UnixTime）
    val ueCommentLine = mutableMapOf<Float, CommentCanvas.CommentObject>()

    // 下付きコメントの配列
    val sitaCommentList = arrayListOf<CommentCanvas.CommentObject>()

    // 高さ：追加時間（UnixTime）
    val sitaCommentLine = mutableMapOf<Float, CommentCanvas.CommentObject>()

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

    var isRunning = false
    lateinit var coroutine: Job

    lateinit var thread: Thread

    var pref_setting: SharedPreferences

    // 更新頻度。とりあえず60fps。16msで更新する
    val FPS = 60L

    init {
        // SurfaceView初期化
        holder.addCallback(this)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
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

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        // コメントの行を最低10行確保するモード
        isTenLineSetting = pref_setting.getBoolean("setting_comment_canvas_10_line", false)
        // コメント行を自由に設定する設定
        isCustomCommentLine =
            pref_setting.getBoolean("setting_comment_canvas_custom_line_use", false)
        customCommentLine =
            pref_setting.getString("setting_comment_canvas_custom_line_value", "10")?.toInt() ?: 20
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        isRunning = true
        //コメントの流れる速度
        val speed = 20
        // val speed = pref_setting.getString("setting_comment_speed", "5")?.toInt() ?: 5
        // コメントキャンバスの更新頻度
        val update = pref_setting.getString("setting_comment_canvas_timer", "10")?.toLong() ?: 10

        viewTreeObserver.addOnGlobalLayoutListener {
            finalHeight = height
            val lineCount = height / fontsize
            for (i in 0 until lineCount.toInt()) {
                commentLines.add(0L)
            }
        }

        thread = thread {
            while (isRunning) {
                if (!isPause) {
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
                    val canvas = holder?.lockCanvas()
                    commentDraw(canvas)
                    holder?.unlockCanvasAndPost(canvas)
                }
                Thread.sleep(1000 / FPS)
            }
        }

        thread.start()
    }

    private fun commentDraw(canvas: Canvas?) {
        // Canvas消す
        canvas?.drawColor(0, PorterDuff.Mode.CLEAR)
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
                        canvas?.drawText(obj.comment, obj.xPos, obj.yPos, blackPaint)
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
        val paint = Paint()
        paint.isAntiAlias = true
        paint.textSize = fontSize
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor(getColor(command))
        return paint
    }

    // 枠取り文字の文字描画
    fun getBlackCommentTextPaint(fontSize: Float = this.fontsize): Paint {
        //黒色テキスト
        val blackPaint = Paint()
        blackPaint.isAntiAlias = true
        blackPaint.strokeWidth = 2.0f
        blackPaint.style = Paint.Style.STROKE
        blackPaint.textSize = fontSize
        blackPaint.color = Color.parseColor("#000000")
        return blackPaint
    }

    // 色
    // 大百科参照：https://dic.nicovideo.jp/a/%E3%82%B3%E3%83%A1%E3%83%B3%E3%83%88
    fun getColor(command: String): String {
        // プレ垢限定色。
        if (command.contains("white2")) {
            return "#CCCC99"
        }
        if (command.contains("red2")) {
            return "#CC0033"
        }
        if (command.contains("pink2")) {
            return "#FF33CC"
        }
        if (command.contains("orange2")) {
            return "#FF6600"
        }
        if (command.contains("yellow2")) {
            return "#999900"
        }
        if (command.contains("green2")) {
            return "#00CC66"
        }
        if (command.contains("cyan2")) {
            return "#00CCCC"
        }
        if (command.contains("blue2")) {
            return "#3399FF"
        }
        if (command.contains("purple2")) {
            return "#6633CC"
        }
        if (command.contains("black2")) {
            return "#666666"
        }
        // 一般でも使えるやつ
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

    /**
     * コメント投稿
     * @param asciiArt アスキーアートのときはtrueにすると速度が一定になります
     * */
    fun postComment(comment: String, commentJSONParse: CommentJSONParse, asciiArt: Boolean = false) {
        when {
            // コメント行をカスタマイズしてるとき
            isCustomCommentLine -> {
                fontsize = (finalHeight / customCommentLine).toFloat()
                blackPaint.textSize = fontsize
            }
            // ポップアップ再生 / 10行コメント確保ーモード時
            isPopupView || isTenLineSetting -> {
                fontsize = (finalHeight / 10).toFloat()
                blackPaint.textSize = fontsize
            }
            else -> {
                fontsize = 20 * resources.displayMetrics.scaledDensity
                blackPaint.textSize = fontsize
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
                CommentCanvas.CommentObject(comment, xPos, yPos, System.currentTimeMillis(), measure, command, asciiArt, fontsize)
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
                CommentCanvas.CommentObject(
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
            val commentObj = CommentCanvas.CommentObject(
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
            val commentObj = CommentCanvas.CommentObject(
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

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        isRunning = false
        coroutine.cancel()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

    }
}