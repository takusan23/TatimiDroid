package io.github.takusan23.tatimidroid

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceManager
import java.util.*
import java.util.regex.Pattern
import kotlin.concurrent.schedule
import kotlin.random.Random


/**
 * コメントを流すView。
 * バージョン３
 * */
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

    /** 定期実行 */
    private val timer = Timer()

    // コメントの配列
    var commentObjList = arrayListOf<CommentObject>()

    // 上付きコメントの配列
    val ueCommentList = arrayListOf<CommentObject>()

    // 下付きコメントの配列
    val sitaCommentList = arrayListOf<CommentObject>()

    // Canvasの高さ。なぜかgetHeight()が0を返すので一工夫する必要がある。くっっっっっっそ
    var finalHeight = 10

    // ポップアップ再生時はtrue
    var isPopupView = false

    /** コメントを流さないときはtrue */
    var isPause = false

    // 10行確保モード
    var isTenLineSetting = false

    // コメント行を自由に変更可能
    var isCustomCommentLine = false
    var customCommentLine = 10

    // 透明度の設定（重そう小並感）
    var commentAlpha = 1.0F

    /** デバッグ用。当たり判定を可視化？ */
    var isShowDrawTextRect = false

    /** てｓｔ */
    lateinit var typeface: Typeface

    /** アスキーアート（コメントアート・職人）のために使う。最後に追加しあ高さが入る */
    private var oldHeight = 0

    init {
        //文字サイズ計算。端末によって変わるので
        fontsize = 20 * resources.displayMetrics.scaledDensity
        val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        // コメントの更新頻度をfpsで設定するかどうか
        val enableCommentSpeedFPS = pref_setting.getBoolean("setting_comment_canvas_speed_fps_enable", false)
        // コメントの流れる速度
        val speed = pref_setting.getString("setting_comment_speed", "3")?.toInt() ?: 3
        // コメントキャンバスの更新頻度
        val update = if (enableCommentSpeedFPS) {
            // fpsで設定
            val fps = pref_setting.getString("setting_comment_canvas_speed_fps", "60")?.toIntOrNull() ?: 60
            // 1000で割る （例：1000/60=16....）
            (1000 / fps)
        } else {
            // ミリ秒で指定
            pref_setting.getString("setting_comment_canvas_timer", "10")?.toIntOrNull() ?: 10
        }.toLong()
        // コメントの透明度
        commentAlpha = pref_setting.getString("setting_comment_alpha", "1.0")?.toFloat() ?: 1.0F
        // コメントの行を最低10行確保するモード
        isTenLineSetting = pref_setting.getBoolean("setting_comment_canvas_10_line", false)
        // コメントの色を部屋の色にする設定が有効ならtrue
        isCommentColorRoom = pref_setting.getBoolean("setting_command_room_color", false)
        // コメント行を自由に設定する設定
        isCustomCommentLine = pref_setting.getBoolean("setting_comment_canvas_custom_line_use", false)
        customCommentLine = pref_setting.getString("setting_comment_canvas_custom_line_value", "10")?.toInt() ?: 20
        // 開発者用項目
        isShowDrawTextRect = pref_setting.getBoolean("dev_setting_comment_canvas_text_rect", false) ?: false
        // 定期実行
        timer.schedule(update, update) {

            // コメント移動止めるやつ
            if (isPause) {
                return@schedule
            }

            // コメントを移動させる
            commentObjList.toList().forEach { obj ->
                if (obj.asciiArt) {
                    // AAの場合は速度を固定する
                    obj.xPos -= speed
                    obj.rect?.left = obj.rect?.left?.minus(speed)
                    obj.rect?.right = obj.rect?.right?.minus(speed)
                } else {
                    obj.xPos -= speed + (obj.comment.length / 8)
                    obj.rect?.left = obj.rect?.left?.minus(speed + (obj.comment.length / 8))
                    obj.rect?.right = obj.rect?.right?.minus(speed + (obj.comment.length / 8))
                }
            }

            // 画面の端っこまで行ってないコメントを選別する。画面外は消す。filter{}はよく落ちるので辞めた
            commentObjList.toList().forEach {
                // 暗黒放送だと100前後くらいになる。
                if (it.rect?.right ?: -10 < 0) {
                    commentObjList.remove(it)
                }
            }
            // commentObjList = commentObjList.filter { commentObject -> commentObject.rect?.right ?: 0 > 0 } as ArrayList<CommentObject>

            val nowUnixTime = System.currentTimeMillis()
            // toList() を使って forEach すればエラー回避できます
            // 3秒経過したら配列から消す
            ueCommentList.toList().forEach {
                if (it != null) {
                    if (nowUnixTime - it.unixTime > 3000) {
                        ueCommentList.remove(it)
                    }
                }
            }
            sitaCommentList.toList().forEach {
                if (it != null) {
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
        }

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // コメントを描画する。
        commentObjList.toList().forEach { obj ->
            if (obj == null) return
            if ((obj.rect?.right ?: 0) > 0) {
                when {
                    obj.command.contains("big") -> {
                        drawComment(canvas, obj, obj.fontSize)
                    }
                    obj.command.contains("small") -> {
                        drawComment(canvas, obj, obj.fontSize)
                    }
                    else -> {
                        drawComment(canvas, obj, obj.fontSize)
                    }
                }
            }
        }
        // 上付きコメントを描画する
        ueCommentList.toList().forEach {
            if (it == null) return // なんかnullの時がある？
            drawComment(canvas, it, it.fontSize)
        }
        // 下付きコメントを描画する
        sitaCommentList.toList().reversed().forEach {
            if (it == null) return // なんかnullの時がある？
            // 上付きコメントを反転させるなどして下付きコメントを実現してるのでちょっとややこしい（なんか当たり判定がうまく行かなくて上付きを使いまわしている）。
            // あと10引いてるのはなんか埋まるから。
            canvas?.drawText(it.comment, it.xPos, (height - 10) - it.yPos, getBlackCommentTextPaint(it.fontSize))
            canvas?.drawText(it.comment, it.xPos, (height - 10) - it.yPos, getCommentTextPaint(it.command, it.fontSize))
        }
    }

    /**
     * コメント描画。
     * Canvas#drawText()を隠しただけ。
     * @param canvas きゃんばす。
     * @param commentObject CommentObject。コメント描画に使う
     * @param fontSize 文字サイズ。big/smallのときはこの関数では扱わないので呼ぶ前に引数に入れてね。
     * */
    private fun drawComment(canvas: Canvas?, obj: CommentObject?, fontSize: Float = obj?.fontSize ?: fontsize) {
        // まあnullになることはない。けど一応
        if (obj?.command != null && obj?.comment != null) {
            // わく
            canvas?.drawText(obj.comment, obj.xPos, obj.yPos, getBlackCommentTextPaint(fontSize))
            // もじ
            canvas?.drawText(obj.comment, obj.xPos, obj.yPos, getCommentTextPaint(obj.command, fontSize))
            // 私が検証で使う。基本使わん
            if (isShowDrawTextRect || obj.yourpost) {
                val checkRect = Rect(obj.xPos.toInt() - 5, (obj.yPos - obj.fontSize).toInt() + 5, (obj.xPos + obj.commentMeasure).toInt() + 5, (obj.yPos).toInt() + 10)
                obj.rect?.apply {
                    val strokePaint = Paint()
                    strokePaint.strokeWidth = 1f
                    strokePaint.color = Color.YELLOW
                    strokePaint.style = Paint.Style.STROKE
                    canvas?.drawRect(checkRect, strokePaint)
                }
            }
        }
    }


    /** 色の変更 */
    fun getCommentTextPaint(command: String, fontSize: Float = this.fontsize): Paint {
        //白色テキスト
        val paint = Paint().apply {
            if (this@CommentCanvas::typeface.isInitialized) {
                typeface = this@CommentCanvas.typeface
            }
            isAntiAlias = true
            textSize = fontSize
            style = Paint.Style.FILL
            color = Color.parseColor(getColor(command))
            alpha = (commentAlpha * 225).toInt() // 0 ~ 225 の範囲で指定するため 225かける
        }
        return paint
    }

    /** 枠取り文字のPaint */
    fun getBlackCommentTextPaint(fontSize: Float = this.fontsize): Paint {
        //黒色テキスト
        val blackPaint = Paint().apply {
            if (this@CommentCanvas::typeface.isInitialized) {
                typeface = this@CommentCanvas.typeface
            }
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
        val colorCodeRegex = Pattern.compile("^#(?:[0-9a-fA-F]{3}){1,2}\$")
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
            // カラーコード？職人は色指定にカラーコード使ってるっぽい
            command.contains("#") -> {
                // 分割して。なんか正規表現で抜き出そうと思ったんだけどできなかったわ
                val colorCode = command.split(" ").find { s -> colorCodeRegex.matcher(s).find() }
                return colorCode ?: "#ffffff" // なければ白
            }
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
     * 複数行コメントを描画する。
     * @param commentStringList コメントを改行コードで区切った時にできる配列[String.split]
     * @param commentJSONParse コメントJSON。
     * */
    fun postCommentAsciiArt(commentStringList: List<String>, commentJSONParse: CommentJSONParse) {
        // 高さ初期化
        oldHeight = 0
        // 入れる
        commentStringList.forEach { comment ->
            postComment(comment, commentJSONParse, true, commentStringList.size)
        }
    }

    /**
     * コメント投稿
     * @param asciiArt アスキーアートのときはtrueにすると速度が一定になり、画面外になった場合もできる限り再現します。
     * もしかして：別スレッドにすれば軽くなる？
     * @param asciiArtLines [asciiArt]がtrueのときは画面に収まるように文字サイズを調整します。そのためにコメントアートが何行になるかの値が必要なので指定してください。
     * */
    fun postComment(comment: String, commentJSONParse: CommentJSONParse, asciiArt: Boolean = false, asciiArtLines: Int = -1) {
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
        // アスキーアートが画面に収まるように。ただし特に何もしなくても画面内に収まる場合は無視。改行多くて入らない場合のみ
        val isAsciiArtUseHeightMax = asciiArt && finalHeight < asciiArtLines * fontsize
        if (isAsciiArtUseHeightMax) {
            fontsize = (finalHeight / asciiArtLines).toFloat()
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
        var measure = getCommentTextPaint(commentJSONParse.comment, commandFontSize).measureText(comment)
        val command = commentJSONParse.mail
        when {
            checkUeComment(command) -> {
                // 上コメ
                // なんだけどコメントがコメントキャンバスを超えるときの対応をしないといけない。
                if (width < measure) {
                    // 超えるとき。私の時代はもう携帯代青天井とかは無いですね。
                    // 一文字のフォントサイズ計算。収めるにはどれだけ縮めれば良いのか
                    commandFontSize = (width.toFloat() / comment.length)
                    // コメントの幅再取得
                    measure = getBlackCommentTextPaint(commandFontSize).measureText(comment)
                } else {
                    // 超えない。10年前から携帯で動画見れた気がするけど結局10年経ってもあんまり外で動画見る人いない気がする
                }
                // 開始位置（横）、高さ、どこまで引き伸ばすか（横）、どこまで引き伸ばすか（高さ）
                // 開始地点は真ん中になるように幅を文字の長さ分引いてそっから割る
                // 高さはこれから図るのでまだ0f
                // 幅は第一引数+コメントの長さ
                // 最後はフォントサイズ分下に伸ばしておk
                val addRect =
                    Rect((((width - measure) / 2).toInt()), 0, (((width - measure) / 2) + measure).toInt(), commandFontSize.toInt())
                val tmpList = ueCommentList.toList()
                for (i in 0 until tmpList.size) {
                    // みていく
                    val obj = tmpList[i]
                    val rect = obj?.rect ?: return
                    if (Rect.intersects(rect, addRect)) {
                        // かぶったー
                        addRect.top += (obj.rect!!.bottom - obj.rect!!.top)
                        addRect.bottom += (obj.rect!!.bottom - obj.rect!!.top)
                    }
                }
                // 画面外はランダム
                if (addRect.top > height) {
                    val range = height / commandFontSize
                    addRect.top = (Random.nextInt(1, range.toInt()) * commandFontSize).toInt()
                    addRect.bottom = (addRect.top + commandFontSize).toInt()
                }
                val commentObj = CommentObject(
                    comment,
                    addRect.left.toFloat(),
                    addRect.bottom.toFloat(),
                    System.currentTimeMillis(),
                    measure,
                    command,
                    asciiArt,
                    addRect,
                    commandFontSize,
                    commentJSONParse.yourPost
                )
                ueCommentList.add(commentObj)
            }
            command.contains("shita") -> {
                // 下こめ
                // なんだけどコメントがコメントキャンバスを超えるときの対応をしないといけない。
                if (width < measure) {
                    // 超えるとき。私の時代はもう携帯代青天井とかは無いですね。
                    // 一文字のフォントサイズ計算。収めるにはどれだけ縮めれば良いのか
                    commandFontSize = (width.toFloat() / comment.length)
                    // コメントの幅再取得
                    measure = getBlackCommentTextPaint(commandFontSize).measureText(comment)
                } else {
                    // 超えない。10年前から携帯で動画見れた気がするけど結局10年経ってもあんまり外で動画見る人いない気がする
                }
                // 開始位置（横）、高さ、どこまで引き伸ばすか（横）、どこまで引き伸ばすか（高さ）
                // 開始地点は真ん中になるように幅を文字の長さ分引いてそっから割る
                // 高さはこれから図るのでまだ0f
                // 幅は第一引数+コメントの長さ
                // 最後はフォントサイズ分下に伸ばしておk
                val addRect =
                    Rect((((width - measure) / 2).toInt()), 0, (((width - measure) / 2) + measure).toInt(), (commandFontSize).toInt())
                val tmpList = sitaCommentList.toList()
                for (i in 0 until tmpList.size) {
                    // みていく
                    val obj = tmpList[i]
                    val rect = obj?.rect ?: return
                    if (Rect.intersects(rect, addRect)) {
                        // かぶったー
                        addRect.top += (obj.rect!!.bottom - obj.rect!!.top)
                        addRect.bottom += (obj.rect!!.bottom - obj.rect!!.top)
                    }
                }
                // 画面外はランダム
                if (addRect.bottom > height) {
                    val range = (height / commandFontSize)
                    addRect.top = (Random.nextInt(1, range.toInt()) * commandFontSize).toInt()
                    addRect.bottom = (addRect.top + commandFontSize).toInt()
                    // println("$comment ${addRect.top} $range")
                }
                val commentObj = CommentObject(
                    comment,
                    addRect.left.toFloat(),
                    addRect.top.toFloat(),
                    System.currentTimeMillis(),
                    measure,
                    command,
                    asciiArt,
                    addRect,
                    commandFontSize,
                    commentJSONParse.yourPost
                )
                sitaCommentList.add(commentObj)
            }
            else -> {
                // sortedByで検証
                val tmpList = commentObjList.toList().sortedBy { commentObject ->
                    return@sortedBy if (commentObject != null) commentObject.yPos else 0f
                }
                // 流れるコメント
                // 開始位置（横）、高さ、どこまで引き伸ばすか（横）、どこまで引き伸ばすか（高さ）
                // 開始地点は画面の端
                // 高さはこれから図るのでまだ0f
                // 幅はコメントの大きさ
                // 最後はフォントサイズ分下に伸ばしておk
                val addRect = Rect(width, 0, (width + measure).toInt(), commandFontSize.toInt())
                // コメントアートのときは当たり判定なし
                if (!isAsciiArtUseHeightMax) {
                    for (i in 0 until tmpList.size) {
                        val obj = tmpList[i]
                        // nullの時がある
                        if (obj != null) {
                            // Rectで当たり判定計算？
                            //  val rect = obj.rect ?: return
                            val rect = Rect(obj.xPos.toInt(), (obj.yPos - fontsize).toInt(), (obj.xPos + obj.commentMeasure).toInt(), obj.yPos.toInt())
                            if (
                                Rect.intersects(rect, addRect)
                            ) {
                                // あたっているので下へ
                                addRect.top = obj.yPos.toInt()
                                addRect.bottom = (addRect.top + commandFontSize).toInt()
                            }
                            // なお画面外の場合はランダム。
                            if (addRect.bottom > height) {
                                // heightが0の時は適当に10にする
                                val until = if (height > 0) height else 10
                                val randomStart = Random.nextInt(1, until)
                                addRect.top = randomStart
                                addRect.bottom = (addRect.top + commandFontSize).toInt()
                            }
                        }
                    }
                } else {
                    addRect.top = oldHeight
                    addRect.bottom = (addRect.top + commandFontSize).toInt()
                    oldHeight = addRect.bottom
                }

                val commentObj = CommentObject(
                    comment,
                    addRect.left.toFloat(),
                    addRect.bottom.toFloat(),
                    System.currentTimeMillis(),
                    measure,
                    command,
                    asciiArt,
                    addRect,
                    commandFontSize,
                    commentJSONParse.yourPost
                )
                commentObjList.add(commentObj)
            }
        }
    }

    /**
     * 上コメントかどうかを検証する
     * 部分一致で「ue」で上か判定するともれなく「blue」「guest」が引っかかるので
     * */
    private fun checkUeComment(command: String): Boolean {
        return when {
            // blueでなおblueの文字を消してもueが残る場合は上コメント
            command.replace(Regex("blue2|blue|guest"), "").contains("ue") -> true
            // ちがう！！！
            else -> false
        }
    }

    /**
     * Viewが外されたとき（Activity/FragmentのonDestroy()みたいな）
     * */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        timer.cancel()
    }

    fun IsInBox(x1: Int, y1: Int, width1: Int, height1: Int, x2: Int, y2: Int, width2: Int, height2: Int): Boolean {
        val right1 = x1 + width1
        val right2 = x2 + width2
        val bottom1 = y1 + height1
        val bottom2 = y2 + height2

        // Check if top-left point is in box
        if (x2 >= x1 && x2 <= right1 && y2 >= y2 && y2 <= bottom1) return true
        // Check if bottom-right point is in box
        return if (right2 >= x1 && right2 <= right1 && bottom2 >= y2 && bottom2 <= bottom1) true else false
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

    /**
     * コメント描画の際に使うデータクラス。
     * */
    data class CommentObject(
        val comment: String,
        var xPos: Float,
        var yPos: Float,
        var unixTime: Long,
        var commentMeasure: Float,
        var command: String,
        var asciiArt: Boolean = false,
        var rect: Rect? = null,
        var fontSize: Float,
        var yourpost: Boolean
    )

}