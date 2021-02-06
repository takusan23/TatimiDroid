package io.github.takusan23.tatimidroid

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * ニコ動のために作り直されたコメント描画。
 *
 * シークバーいじることができる。
 * */
class ReCommentCanvas(ctx: Context, attributeSet: AttributeSet?) : View(ctx, attributeSet) {

    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    /** 鯖からもらったコメント一覧 */
    var rawCommentList = arrayListOf<CommentJSONParse>()

    /** 流す or 流れた コメント一覧 */
    val drawNakaCommentList = arrayListOf<ReDrawCommentData>()

    /** 上のコメント一覧 */
    val drawUeCommentList = arrayListOf<ReDrawCommentData>()

    /** 下のコメント一覧 */
    val drawShitaCommentList = arrayListOf<ReDrawCommentData>()

    /** アスキーアート（コメントアート・職人）のために使う。最後に追加しあ高さが入る */
    private var oldHeight = 0

    /** 黒枠 */
    private val blackPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 2.0f
        style = Paint.Style.STROKE
        textSize = 20 * resources.displayMetrics.scaledDensity
        color = Color.parseColor("#000000")
    }

    /** 白色 */
    private val paint = Paint().apply {
        isAntiAlias = true
        textSize = 20 * resources.displayMetrics.scaledDensity
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    /** 当たり判定検証用にコメントに枠をつける */
    private val strokePaint = Paint().apply {
        strokeWidth = 5f
        color = Color.RED
        style = Paint.Style.STROKE
    }

    /** コメントの枠を表示する際はtrue。PC版とかで自分のコメントに枠が付くあれ。 */
    val watashiHaDeveloper by lazy { prefSetting.getBoolean("dev_setting_comment_canvas_text_rect", false) }

    /**
     * フォント変更
     * */
    var typeFace: Typeface? = null
        set(value) {
            paint.typeface = value
            blackPaint.typeface = value
            field = value
        }

    /** コメントを動かすTimer */
    private var commentDrawTimer = Timer()

    /** コメントを登録するTimer */
    private var commentAddTimer = Timer()

    /** 再生時間を入れてください。定期的に */
    var currentPos = 0L

    /** 再生中かどうか。再生状態が変わったらこちらも更新してください。 */
    var isPlaying = false

    /**
     * この２つはコメントが重複しないようにするためのもの
     * */
    private val drewedList = arrayListOf<Long>()
    private var tmpPosition = 0L

    /**
     * コメントの移動する大きさ。
     *
     * でふぉは-5です。
     * */
    private val commentMoveMinus by lazy { prefSetting.getString("setting_comment_speed", "3")?.toInt() ?: 3 }

    /**
     * 16ミリ秒ごと[commentMoveMinus]分動かす。
     *
     * 1秒60回(60fps)になる(1000ms/60fps=16)
     * */
    private val commentUpdateMs by lazy { getCommentCanvasUpdateMs() }

    /** コメントの透明度 */
    val commentAlpha = prefSetting.getString("setting_comment_alpha", "1.0")?.toFloat() ?: 1.0F

    /**
     * 高さ、幅決定版。getWidthとかが0を返すので対策
     * */
    private var finalWidth = width
    private var finalHeight = height

    /** コルーチン */
    private val coroutineJob = Job()


    init {
        // コメントを動かす
        commentDrawTimer.schedule(commentUpdateMs, commentUpdateMs) {
            GlobalScope.launch(coroutineJob + Dispatchers.Main) {
                if (isPlaying) {
                    // 画面外のコメントは描画しない
                    for (reDrawCommentData in drawNakaCommentList.toList().filter { reDrawCommentData -> reDrawCommentData.rect.right > -reDrawCommentData.measure }) {
                        if (reDrawCommentData != null) {
                            // 文字が長いときは早くする。アスキーアートのときは速度一定
                            val speed = if (reDrawCommentData.asciiArt) commentMoveMinus else commentMoveMinus + (reDrawCommentData.comment.length / 8)
                            reDrawCommentData.rect.left -= speed
                            reDrawCommentData.rect.right -= speed
                            // なお画面外は消す
                            if (reDrawCommentData.rect.right < 0) {
                                drawNakaCommentList.remove(reDrawCommentData)
                            }
                        }
                    }
                    // 上コメ、下コメは３秒間表示させる
                    drawUeCommentList.toList().forEach { reDrawCommentData ->
                        // マイナスの値は許されない。これしないとループ再生時に固定コメントが残る
                        val calc = currentPos - reDrawCommentData.videoPos
                        if (calc > 3000 || calc < -1) {
                            drawUeCommentList.remove(reDrawCommentData)
                        }
                    }
                    drawShitaCommentList.toList().forEach { reDrawCommentData ->
                        // マイナスの値は許されない。これしないとループ再生時に固定コメントが残る
                        val calc = currentPos - reDrawCommentData.videoPos
                        if (calc > 3000 || calc < -1) {
                            drawShitaCommentList.remove(reDrawCommentData)
                        }
                    }
                    // 再描画
                    invalidate()
                }
            }
        }
        // コメントを追加する
        GlobalScope.launch(coroutineJob + Dispatchers.Main) {
            // delay+whileで定期実行が書ける
            while (true) {
                val currentVPos = currentPos / 100L
                val currentPositionSec = currentPos / 1000
                if (tmpPosition != currentPositionSec) {
                    drewedList.clear()
                    tmpPosition = currentPositionSec
                }
                val drawList = withContext(Dispatchers.IO) {
                    rawCommentList.filter { commentJSONParse ->
                        (commentJSONParse.vpos.toLong() / 10L) == (currentVPos)
                    }
                }
                drawList.forEach {
                    // 追加可能か（livedl等TSのコメントはコメントIDが無い？のでvposで代替する）
                    // なんかしらんけど負荷がかかりすぎるとここで ConcurrentModificationException 吐くので Array#toList() を使う
                    val isAddable = drewedList.toList().none { id -> if (it.commentNo.isEmpty()) it.vpos.toLong() == id else it.commentNo.toLong() == id } // 条件に合わなければtrue
                    if (isAddable) {
                        // コメントIDない場合はvposで代替する
                        drewedList.add(if (it.commentNo.isEmpty()) it.vpos.toLong() else it.commentNo.toLong())
                        // コメント登録。
                        drawComment(it, currentPos)
                    }
                }
                delay(100)
            }
        }


        // Viewの大きさ決定版
        viewTreeObserver.addOnGlobalLayoutListener {
            finalHeight = height
            finalWidth = width
        }
    }

    fun clearCommentList() {
        rawCommentList.clear()
        drawNakaCommentList.clear()
        drawShitaCommentList.clear()
        drawUeCommentList.clear()
    }

    /** Viewがおわった時 */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        commentDrawTimer.cancel()
        commentAddTimer.cancel()
        coroutineJob.cancelChildren()
    }

    /**
     * コメントシークした時に描画できるように
     * */
    fun seekComment() {
        GlobalScope.launch(coroutineJob + Dispatchers.Main) {
            drawNakaCommentList.clear()
            drawUeCommentList.clear()
            drawShitaCommentList.clear()
            // １秒でどれだけ移動させるか。1000ミリ秒に何回呼ばれるかを求めて後は移動分掛ける
            val moveValue = (1000 / commentUpdateMs).toInt() * commentMoveMinus
            // 画面のサイズ分取り出す？
            repeat((finalWidth.toFloat() / moveValue).roundToInt()) { sec ->
                val currentPos = currentPos
                val currentPosSec = currentPos / 1000
                val drawList = withContext(Dispatchers.IO) {
                    rawCommentList.filter { commentJSONParse ->
                        (commentJSONParse.vpos.toLong() / 100L) == (currentPosSec - sec)
                    }
                }
                drawList.forEach {
                    // 追加可能か（livedl等TSのコメントはコメントIDが無い？のでvposで代替する）
                    // なんかしらんけど負荷がかかりすぎるとここで ConcurrentModificationException 吐くので Array#toList() を使う
                    val isAddable = drewedList.toList().none { id -> if (it.commentNo.isEmpty()) it.vpos.toLong() == id else it.commentNo.toLong() == id } // 条件に合わなければtrue
                    if (isAddable) {
                        // コメントIDない場合はvposで代替する
                        drewedList.add(if (it.commentNo.isEmpty()) it.vpos.toLong() else it.commentNo.toLong())
                        // コメントが長いときは早く流す
                        val speed = commentMoveMinus + (it.comment.length / 8)
                        // コメント登録。
                        drawComment(it, currentPos + (sec * 1000), (sec * moveValue) + speed)
                    }
                }
            }
            invalidate()
        }
    }

    /** [invalidate]呼ぶとここに来る */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // 「ぬるぽ」「ガッ」なんて知らんわ
        if (canvas == null) return

        // 中コメ
        for (reDrawCommentData in drawNakaCommentList.toList()) {
            if (reDrawCommentData != null) {
                setCommandPaint(reDrawCommentData.command, reDrawCommentData.fontSize)
                canvas.drawText(reDrawCommentData.comment, reDrawCommentData.rect.left.toFloat(), reDrawCommentData.rect.bottom.toFloat(), blackPaint)
                canvas.drawText(reDrawCommentData.comment, reDrawCommentData.rect.left.toFloat(), reDrawCommentData.rect.bottom.toFloat(), paint)
                // 当たり判定検証ように枠をつける
                if (watashiHaDeveloper) {
                    canvas.drawRect(reDrawCommentData.rect, strokePaint)
                }
            }
        }

        // 上コメ
        for (reDrawCommentData in drawUeCommentList.toList()) {
            if (reDrawCommentData != null) {
                setCommandPaint(reDrawCommentData.command, reDrawCommentData.fontSize)
                canvas.drawText(reDrawCommentData.comment, reDrawCommentData.rect.left.toFloat(), reDrawCommentData.rect.bottom.toFloat(), blackPaint)
                canvas.drawText(reDrawCommentData.comment, reDrawCommentData.rect.left.toFloat(), reDrawCommentData.rect.bottom.toFloat(), paint)
                // 当たり判定検証ように枠をつける
                if (watashiHaDeveloper) {
                    canvas.drawRect(reDrawCommentData.rect, strokePaint)
                }
            }
        }

        // 下コメ
        for (reDrawCommentData in drawShitaCommentList.toList()) {
            if (reDrawCommentData != null) {
                setCommandPaint(reDrawCommentData.command, reDrawCommentData.fontSize)
                canvas.drawText(reDrawCommentData.comment, reDrawCommentData.rect.left.toFloat(), reDrawCommentData.rect.bottom.toFloat(), blackPaint)
                canvas.drawText(reDrawCommentData.comment, reDrawCommentData.rect.left.toFloat(), reDrawCommentData.rect.bottom.toFloat(), paint)
                // 当たり判定検証ように枠をつける
                if (watashiHaDeveloper) {
                    canvas.drawRect(reDrawCommentData.rect, strokePaint)
                }
            }
        }
    }

    /**
     * コメントを登録する
     * */
    fun drawComment(commentJSONParse: CommentJSONParse, videoPos: Long, addPos: Int = 0) {
        when {
            // うえ
            checkUeComment(commentJSONParse.mail) -> drawUeComment(commentJSONParse, videoPos)
            // した
            commentJSONParse.mail.contains("shita") -> drawShitaComment(commentJSONParse, videoPos)
            // 複数行の中コメ
            commentJSONParse.comment.contains("\n") -> drawAsciiArtNakaComment(commentJSONParse.comment.split("\n"), commentJSONParse, videoPos, addPos)
            // 通常
            else -> drawNakaComment(commentJSONParse, videoPos, addPos)
        }
    }

    /**
     * 複数行コメントな中コメントを描画します。
     * @param commentJSONList コメントを改行コードで分割した配列。[String.split]で\n区切りで分割してね
     * */
    private fun drawAsciiArtNakaComment(commentList: List<String>, commentJSON: CommentJSONParse, videoPos: Long, addPos: Int = 0) {
        // たかさ初期化
        oldHeight = 0
        commentList.forEach { comment ->
            commentJSON.comment = comment
            // アスキーアートで登録
            drawNakaComment(commentJSON, videoPos, addPos, true, commentList.size)
        }
    }

    /**
     * 中コメントを登録する
     * @param asciiArt アスキーアート（複数行コメント）の場合はtrue
     * @param asciiArtLines コメントアートが何行になるのか
     * */
    private fun drawNakaComment(commentJSON: CommentJSONParse, videoPos: Long, addPos: Int = 0, asciiArt: Boolean = false, asciiArtLines: Int = -1) {
        val comment = commentJSON.comment
        val command = commentJSON.mail
        // フォントサイズ。コマンド等も考慮して
        var fontSize = getCommandFontSize(command).toInt()
        val measure = getBlackCommentTextPaint(fontSize).measureText(comment)
        val lect = finalWidth - addPos
        // アスキーアートが画面に収まるように。ただし特に何もしなくても画面内に収まる場合は無視。改行多くて入らない場合のみ
        val isAsciiArtUseHeightMax = asciiArt && finalHeight < asciiArtLines * fontSize
        if (isAsciiArtUseHeightMax) {
            fontSize = finalHeight / asciiArtLines
        }
        // 当たり判定計算
        val addRect = Rect(lect, 0, (lect + measure).toInt(), fontSize)
        if (isAsciiArtUseHeightMax) {
            // 画面外に収まらないコメントの場合
            // コメントアート、アスキーアート
            addRect.top = oldHeight
            addRect.bottom = addRect.top + fontSize
            oldHeight = addRect.bottom
        } else {
            // 全パターん
            val tmpList = drawNakaCommentList.toList().sortedBy { reDrawCommentData -> reDrawCommentData.rect.top }
            for (reDrawCommentData in tmpList) {
                if (Rect.intersects(reDrawCommentData.rect, addRect)) {
                    // あたっているので下へ
                    addRect.top = reDrawCommentData.rect.bottom
                    addRect.bottom = (addRect.top + fontSize)
                }
            }
            // なお画面外突入時はランダム
            if (addRect.bottom > finalHeight) {
                val randomValue = randomValue(fontSize.toFloat())
                addRect.top = randomValue
                addRect.bottom = addRect.top + fontSize
            }
        }
        // 配列に入れる
        val data = ReDrawCommentData(
            comment = comment,
            command = command,
            videoPos = videoPos,
            rect = addRect,
            pos = "naka",
            fontSize = fontSize.toFloat(),
            measure = measure,
            asciiArt = asciiArt
        )
        drawNakaCommentList.add(data)
    }

    /**
     * 上コメントを登録する
     * */
    private fun drawUeComment(commentJSON: CommentJSONParse, videoPos: Long) {
        val comment = commentJSON.comment
        val command = commentJSON.mail
        // コメントの大きさ調整
        val calcCommentSize = calcCommentFontSize(getBlackCommentTextPaint(getCommandFontSize(command).toInt()).measureText(comment), getCommandFontSize(command), comment)
        val measure = calcCommentSize.first
        // フォントサイズ。コマンド等も考慮して
        val fontSize = calcCommentSize.second.toInt()
        val lect = ((finalWidth - measure) / 2).toInt()
        // 当たり判定計算
        val addRect = Rect(lect, 0, (lect + measure).toInt(), fontSize)
        // 全パターん
        val tmpList = drawUeCommentList.toList().sortedBy { reDrawCommentData -> reDrawCommentData.rect.top }
        for (reDrawCommentData in tmpList) {
            if (Rect.intersects(reDrawCommentData.rect, addRect)) {
                // あたっているので下へ
                addRect.top = reDrawCommentData.rect.bottom
                addRect.bottom = (addRect.top + reDrawCommentData.fontSize).roundToInt()
            }
        }
        // なお画面外突入時はランダム
        if (addRect.top > finalHeight) {
            val randomValue = randomValue(fontSize.toFloat())
            addRect.top = randomValue
            addRect.bottom = addRect.top + fontSize
        }
        // 配列に入れる
        val data = ReDrawCommentData(
            comment = comment,
            command = command,
            videoPos = videoPos,
            rect = addRect,
            pos = "ue",
            measure = measure,
            fontSize = fontSize.toFloat()
        )
        drawUeCommentList.add(data)
    }

    /**
     * 下コメントを登録する
     * */
    private fun drawShitaComment(commentJSON: CommentJSONParse, videoPos: Long) {
        val comment = commentJSON.comment
        val command = commentJSON.mail
        // コメントの大きさ調整
        val calcCommentSize = calcCommentFontSize(getBlackCommentTextPaint(getCommandFontSize(command).toInt()).measureText(comment), getCommandFontSize(command), comment)
        val measure = calcCommentSize.first
        // フォントサイズ。コマンド等も考慮して
        val fontSize = calcCommentSize.second.toInt()
        val lect = ((finalWidth - measure) / 2).toInt()
        // 当たり判定計算
        val addRect = Rect(lect, finalHeight - fontSize, (lect + measure).toInt(), finalHeight)
        // 全パターん
        val tmpList = drawShitaCommentList.toList().sortedBy { reDrawCommentData -> reDrawCommentData.videoPos }
        for (reDrawCommentData in tmpList) {
            if (Rect.intersects(reDrawCommentData.rect, addRect)) {
                // あたっているので下へ
                addRect.top = (reDrawCommentData.rect.top - reDrawCommentData.fontSize).roundToInt()
                addRect.bottom = (reDrawCommentData.rect.bottom - reDrawCommentData.fontSize).roundToInt()
            }
        }
        // なお画面外突入時はランダム
        if (addRect.top < 0) {
            val randomValue = randomValue(fontSize.toFloat())
            addRect.bottom = randomValue + fontSize
            addRect.top = randomValue
        }
        // 配列に入れる
        val data = ReDrawCommentData(
            comment = comment,
            command = command,
            videoPos = videoPos,
            rect = addRect,
            pos = "shita",
            measure = measure,
            fontSize = fontSize.toFloat()
        )
        drawShitaCommentList.add(data)
    }

    /**
     * コマンドに合った文字の大きさを返す
     * @param command big/small など
     * @return フォントサイズ
     * */
    fun getCommandFontSize(command: String): Float {
        // でふぉ
        val defaultFontSize = 20 * resources.displayMetrics.scaledDensity

        // コメント行を自由に設定する設定
        val isCustomCommentLine = prefSetting.getBoolean("setting_comment_canvas_custom_line_use", false)
        val customCommentLine = prefSetting.getString("setting_comment_canvas_custom_line_value", "10")?.toIntOrNull() ?: 20

        // CommentCanvasが小さくても最低限確保する行
        val isMinLineSetting = prefSetting.getBoolean("setting_comment_canvas_min_line", true)
        val minLineValue = prefSetting.getString("setting_comment_canvas_min_line_value", "10")?.toIntOrNull() ?: 10

        // 現在最大何行書けるか
        val currentCommentLine = finalHeight / defaultFontSize

        // 強制10行表示モード
        val is10LineMode = prefSetting.getBoolean("setting_comment_canvas_10_line", false)
        // フォントサイズ
        val fontSize = when {
            is10LineMode -> (finalHeight / 10).toFloat() // 強制10行確保
            isCustomCommentLine -> (finalHeight / customCommentLine).toFloat() // 自由に行設定
            isMinLineSetting && currentCommentLine < minLineValue -> (finalHeight / minLineValue).toFloat() // 最低限確保する設定。最低限確保する行を下回る必要がある
            else -> defaultFontSize // でふぉ
        }
        return when {
            command.contains("big") -> {
                (fontSize * 1.3).toFloat()
            }
            command.contains("small") -> {
                (fontSize * 0.8).toFloat()
            }
            else -> fontSize
        }
    }

    /**
     * 指定したフォントサイズのPaintを生成する関数
     * */
    private fun getBlackCommentTextPaint(fontSize: Int): Paint {
        val paint = Paint()
        paint.textSize = fontSize.toFloat()
        return paint
    }

    /** 高さをランダムで決める */
    private fun randomValue(fontSize: Float): Int {
        return if (finalHeight > fontSize) {
            Random.nextInt(1, (height - fontSize).toInt())
        } else {
            Random.nextInt(1, finalHeight)
        }
    }

    /**
     * コマンドの色に合わせてPaintを切り替える
     * @param command コマンド。
     * */
    private fun setCommandPaint(command: String, fontSize: Float) {
        paint.textSize = fontSize
        blackPaint.textSize = fontSize
        paint.color = Color.parseColor(getColor(command))
        paint.alpha = (commentAlpha * 225).toInt() // 0 ~ 225 の範囲で指定するため 225かける
        blackPaint.alpha = (commentAlpha * 225).toInt() // 0 ~ 225 の範囲で指定するため 225かける
    }

    /**
     * 色
     * 大百科参照：https://dic.nicovideo.jp/a/%E3%82%B3%E3%83%A1%E3%83%B3%E3%83%88
     * */
    private fun getColor(command: String): String {
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
            // command.contains("#") -> {
            //     return command.split(" ").find { s -> s.contains("#") } ?: "#FFFFFF"
            //     // return ("#.{6}?").toRegex().find(command)?.value ?: "#FFFFFF"
            // }
            // その他
            else -> "#ffffff"
        }
    }


    /**
     * コメントが入り切らないときに大きさを調整して画面内に収める関数
     * @param argMeasure 元のコメントの長さ
     * @param comment コメント
     * @param fontSize フォントサイズ
     * @return firstがコメントの長さ、secondがコメントのフォントサイズ
     * */
    private fun calcCommentFontSize(argMeasure: Float, fontSize: Float, comment: String): Pair<Float, Float> {
        // コメントがコメントキャンバスを超えるときの対応をしないといけない。
        var measure = argMeasure
        var commandFontSize = fontSize
        if (finalWidth < argMeasure) {
            // 超えるとき。私の時代はもう携帯代青天井とかは無いですね。
            // 一文字のフォントサイズ計算。収めるにはどれだけ縮めれば良いのか
            commandFontSize = (finalWidth.toFloat() / comment.length)
            // コメントの幅再取得
            measure = getBlackCommentTextPaint(commandFontSize.toInt()).measureText(comment)
        } else {
            // 超えない。10年前から携帯で動画見れた気がするけど結局10年経ってもあんまり外で動画見る人いない気がする
        }
        return Pair(measure, commandFontSize)
    }

    /**
     * 上コメントかどうかを検証する
     * 部分一致で「ue」で上か判定するともれなく「blue」が引っかかるので
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
     * コメントキャンバスの更新頻度を返す
     * */
    private fun getCommentCanvasUpdateMs(): Long {
        // コメントの更新頻度をfpsで設定するかどうか
        val enableCommentSpeedFPS = prefSetting.getBoolean("setting_comment_canvas_speed_fps_enable", false)
        // コメントキャンバスの更新頻度
        return if (enableCommentSpeedFPS) {
            // fpsで設定
            val fps = prefSetting.getString("setting_comment_canvas_speed_fps", "60")?.toIntOrNull() ?: 60
            // 1000で割る （例：1000/60=16....）
            (1000 / fps)
        } else {
            // ミリ秒で指定
            prefSetting.getString("setting_comment_canvas_timer", "10")?.toIntOrNull() ?: 10
        }.toLong()
    }

}

/**
 * [io.github.takusan23.tatimidroid.ReCommentCanvas]で使うデータクラス。
 * @param command コマンド
 * @param comment コメント本文
 * @param pos ue naka shita のどれか
 * @param rect 当たり判定やコメント移動で使う
 * @param videoPos 再生位置。動画の時間
 * */
class ReDrawCommentData(
    val comment: String,
    val command: String,
    val videoPos: Long,
    val rect: Rect,
    val pos: String,
    val fontSize: Float,
    val measure: Float,
    val asciiArt: Boolean = false,
)