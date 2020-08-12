package io.github.takusan23.tatimidroid

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceManager
import kotlin.random.Random

/**
 * ニコ動のために作り直されたコメント描画。
 *
 * 今回はただ流すのではなく、動画の再生時間に合わせて動きます（多分。てかそうしたい）
 * ```kotlin
 * // コメント動かす
 * Timer().schedule(timerTask {
 *      runOnUiThread {
 *           re_comment_canvas.currentPosSec = exoPlayer.currentPosition / 1000
 *           re_comment_canvas.oldUpdateMs = re_comment_canvas.currentPosMillSec
 *           re_comment_canvas.currentPosMillSec = exoPlayer.currentPosition
 *           re_comment_canvas.invalidate()
 *      }
 * },30,30)
 * ```
 * */
class ReCommentCanvas(ctx: Context, attributeSet: AttributeSet?) : View(ctx, attributeSet) {

    /** 描画するコメント一覧。描画中コメントは[getDrawingCommentList]を参照 */
    private val commentList = arrayListOf<DrawCommentData>()

    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    /**
     * 現在の動画の再生時間。
     *
     * こっちは秒で入れてね。ExoPlayerなら[com.google.android.exoplayer2.SimpleExoPlayer.getCurrentPosition]を1000で割った値を入れればいいです。
     *
     * コメント流すので使うので新しい値を常に入れてくれ
     *
     * */
    var currentPosSec = 0L

    /**
     * 現在の動画の再生時間。
     *
     * こっちはミリ秒で入れてね。ExoPlayerなら[com.google.android.exoplayer2.SimpleExoPlayer.getCurrentPosition]の値をそのまま入れてください。
     *
     * コメント流すので使うので新しい値を常に入れてくれ
     * */
    var currentPosMillSec = 0L

    /** 黒枠 */
    private val blackPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 2.0f
        style = Paint.Style.STROKE
        textSize = 50f
        color = Color.parseColor("#000000")
        //文字サイズ計算。端末によって変わるので
        textSize = 20 * resources.displayMetrics.scaledDensity
    }

    /** 白色 */
    private val paint = Paint().apply {
        isAntiAlias = true
        textSize = 50f
        style = Paint.Style.FILL
        color = Color.WHITE
        //文字サイズ計算。端末によって変わるので
        textSize = 20 * resources.displayMetrics.scaledDensity
    }

    /** 当たり判定検証用にコメントに枠をつける */
    private val strokePaint = Paint().apply {
        strokeWidth = 5f
        color = Color.RED
        style = Paint.Style.STROKE
    }

    /** コメントの枠を表示する際はtrue。PC版とかで自分のコメントに枠が付くあれ。 */
    var watashiHaDeveloper = false

    /**
     * 更新前の位置。シークする際に使う。
     * 例：シーク前３秒、シーク後５秒で、コメントの位置を合わせるのにシーク前の時間が必要なため
     * */
    var oldUpdateMs = 0L

    /**
     * コメントの速度？
     * 2ぐらいが良いと思った（要検証）
     *
     * 30ms で 2 ぐらいが良い？
     *
     * */
    var commentMoveFix = 2

    /**
     * フォント変更
     * */
    var typeFace: Typeface? = null
        set(value) {
            paint.typeface = value
            blackPaint.typeface = value
            field = value
        }

    /**
     * なんかしらんけどViewの高さが取れないのでちょいまって
     * */
    var finalWidth = width
    var finalHeight = height

    init {
        viewTreeObserver.addOnGlobalLayoutListener {
            finalHeight = height
            finalWidth = width
        }
    }


    /**
     * あくまでもコメントを動かして描画するだけ。
     *
     * 当たり判定はコメント追加時に[postComment]で計算してたりする。
     *
     * というわけで[invalidate]を呼び、[currentPosMillSec]と[currentPosSec]の値はできる限り新しい値を入れておく必要があります。
     *
     *
     * */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // 「ぬるぽ」「ガッ」なんて知らんわ
        if (canvas == null) return

        /**
         * 描画すべきコメント
         * */
        val currentPosCommentList = getDrawingCommentList()

        /**
         * コメントずらす
         * */
        for (i in currentPosCommentList.indices) {
            val drawCommentData = currentPosCommentList[i]
            val minus = currentPosMillSec - drawCommentData.drawStartTimeMs
            val left = finalWidth - (minus / 2)
            drawCommentData.rect.left = left.toInt()
            drawCommentData.rect.right = (left + drawCommentData.measure).toInt()
            // 文字色
            setCommandPaint(drawCommentData.command, drawCommentData.fontSize)
            // 文字を流す
            canvas.drawText(drawCommentData.comment, drawCommentData.rect.left.toFloat(), drawCommentData.rect.bottom.toFloat(), paint)
            canvas.drawText(drawCommentData.comment, drawCommentData.rect.left.toFloat(), drawCommentData.rect.bottom.toFloat(), blackPaint)
            // 当たり判定検証ように枠をつける
            if (watashiHaDeveloper) {
                canvas.drawRect(drawCommentData.rect, strokePaint)
            }
        }


        // 上コメントを描画する。今から三秒前までに描画されるコメントを取得
        getDrawingUeCommentList().toList().forEach { drawCommentData ->
            // コメント描画すべきコメントがここに！
            // 文字色
            setCommandPaint(drawCommentData.command, drawCommentData.fontSize)
            // 上コメントを描画する
            canvas.drawText(drawCommentData.comment, drawCommentData.rect.left.toFloat(), drawCommentData.rect.bottom.toFloat(), paint)
            canvas.drawText(drawCommentData.comment, drawCommentData.rect.left.toFloat(), drawCommentData.rect.bottom.toFloat(), blackPaint)
            // 当たり判定検証ように枠をつける
            if (watashiHaDeveloper) {
                canvas.drawRect(drawCommentData.rect, strokePaint)
            }
        }

        // 下コメントを描画する。今から三秒前まで
        getDrawingShitaCommentList().toList().forEach { drawCommentData ->
            // コメント描画すべきコメントがここに！
            // 文字色
            setCommandPaint(drawCommentData.command, drawCommentData.fontSize)
            // 下コメントを描画する
            canvas.drawText(drawCommentData.comment, drawCommentData.rect.left.toFloat(), drawCommentData.rect.bottom.toFloat(), paint)
            canvas.drawText(drawCommentData.comment, drawCommentData.rect.left.toFloat(), drawCommentData.rect.bottom.toFloat(), blackPaint)
            // 当たり判定検証ように枠をつける
            if (watashiHaDeveloper) {
                canvas.drawRect(drawCommentData.rect, strokePaint)
            }
        }

    }

    /**
     * コメントを登録する。
     *
     * 生放送と違い動画は再生時間が必要
     *
     * @param comment コメント。うｐ主って死語？
     * @param drawStartTimeMs コメントを流す時間。vposをごにょごにょして
     * */
    fun postComment(comment: String, command: String, drawStartTimeMs: Long) {
        // コメントの長さを測る
        val measure = paint.measureText(comment)
        // フォントサイズ。コマンド等も考慮して
        val fontSize = getCommandFontSize(command)
        // 分岐
        when {
            checkUeComment(command) -> postCommentUe(comment, command, drawStartTimeMs, measure, fontSize)
            command.contains("shita") -> postCommentShita(comment, command, drawStartTimeMs, measure, fontSize)
            else -> postCommentNaka(comment, command, drawStartTimeMs, measure, fontSize)
        }
    }

    /**
     * 流れるコメントを登録する。
     *
     *         画面外に登録する
     *         ↓　こんな感じに
     * |        | www うぽつ
     * | 再生前 |   44444
     * |        |
     *
     * [onDraw]でずらしていく
     *
     * */
    private fun postCommentNaka(comment: String, command: String, drawStartTimeMs: Long, measure: Float, fontSize: Float) {
        // コメントを流す時間（秒）
        val drawStartTimeSec = drawStartTimeMs / 1000
        // コメントの位置。画面の端っこからミリ秒分離した先に設置
        val left = finalWidth + drawStartTimeMs / commentMoveFix
        val right = left + measure
        // 当たり判定計算。Rectで四角形の当たり判定の判定（？）ができる
        val addRect = Rect(left.toInt(), 0, right.toInt(), fontSize.toInt())
        // 当たっているか判定。全部のコメントから当たっていれば調整する。なお並び替えるとうまくいく模様
        for (drawCommentData in commentList.sortedBy { drawCommentData -> drawCommentData.rect.top }) {
            // Rect生成
            if (Rect.intersects(addRect, drawCommentData.rect)) {
                // 当たっているなら下のスペースへ
                addRect.top = drawCommentData.rect.bottom
                addRect.bottom = (addRect.top + fontSize).toInt()
            }
        }
        // なお画面外突入時はランダム
        if (addRect.top > finalHeight) {
            val randomValue = randomValue(fontSize)
            addRect.top = randomValue
            addRect.bottom = (addRect.top + fontSize).toInt()
        }
        // 配列に入れる
        val data = DrawCommentData(
            comment = comment,
            command = command,
            drawStartTimeMs = drawStartTimeMs,
            drawStartTimeSec = drawStartTimeSec,
            measure = measure,
            fontSize = fontSize,
            rect = addRect
        )
        commentList.add(data)
    }

    /**
     * 上コメントを追加する
     * */
    private fun postCommentUe(comment: String, command: String, drawStartTimeMs: Long, argMeasure: Float, argFontSize: Float) {
        // コメントの大きさ調整
        val calcCommentSize = calcCommentFontSize(argMeasure, argFontSize, comment)
        val measure = calcCommentSize.first
        val fontSize = calcCommentSize.second
        // コメントを流す時間（秒）
        val drawStartTimeSec = drawStartTimeMs / 1000
        // 当たり判定計算。Rectで四角形の当たり判定の判定（？）ができる
        val addRect = Rect(
            ((finalWidth - measure) / 2).toInt(),
            0,
            (((finalWidth - measure) / 2).toInt() + measure).toInt(),
            fontSize.toInt()
        ) // 真ん中にする
        // 現在描画中のコメント。
        val drawingUeCommentList = commentList.toList()
            .filter { drawCommentData -> drawCommentData.drawStartTimeSec in (drawStartTimeSec - 3)..drawStartTimeSec && drawCommentData.position == "ue" }
            .sortedBy { drawCommentData -> drawCommentData.rect.bottom }
        // 当たっているか判定
        for (drawCommentData in drawingUeCommentList) {
            // Rect生成
            if (Rect.intersects(addRect, drawCommentData.rect)) {
                // 当たっているなら下のスペースへ
                addRect.top = drawCommentData.rect.bottom
                addRect.bottom = (addRect.top + fontSize).toInt()
            }
        }
        // なお画面外突入時はランダム
        if (addRect.top > finalHeight) {
            val randomValue = randomValue(fontSize)
            addRect.top = randomValue
            addRect.bottom = (addRect.top + fontSize).toInt()
        }
        // 配列に入れる
        val data = DrawCommentData(
            comment = comment,
            command = command,
            drawStartTimeMs = drawStartTimeMs,
            drawStartTimeSec = drawStartTimeSec,
            measure = measure,
            fontSize = fontSize,
            rect = addRect,
            position = "ue"
        )
        commentList.add(data)
    }

    /**
     * 下付きコメントを登録する
     * */
    private fun postCommentShita(comment: String, command: String, drawStartTimeMs: Long, argMeasure: Float, argFontSize: Float) {
        // コメントの大きさ調整
        val calcCommentSize = calcCommentFontSize(argMeasure, argFontSize, comment)
        val measure = calcCommentSize.first
        val fontSize = calcCommentSize.second
        // コメントの位置
        val rectTop = finalHeight - fontSize
        // コメントを流す時間（秒）
        val drawStartTimeSec = drawStartTimeMs / 1000
        // 当たり判定計算。Rectで四角形の当たり判定の判定（？）ができる
        val addRect = Rect(
            ((finalWidth - measure) / 2).toInt(),
            rectTop.toInt(),
            (((finalWidth - measure) / 2).toInt() + measure).toInt(),
            finalHeight
        )
        // 現在描画中のコメント。なんか大きい順に並び替えると当たり判定改善される
        val drawingShitaCommentList = commentList.toList()
            .filter { drawCommentData -> drawCommentData.drawStartTimeSec in (drawStartTimeSec - 3)..(drawStartTimeSec) && drawCommentData.position == "shita" }
            .sortedByDescending { drawCommentData -> drawCommentData.rect.bottom }
        // 当たっているか判定
        for (drawCommentData in drawingShitaCommentList) {
            // Rect生成
            if (Rect.intersects(addRect, drawCommentData.rect)) {
                // 当たっているなら下のスペースへ
                addRect.bottom = drawCommentData.rect.top
                addRect.top = (addRect.bottom - fontSize).toInt()
            }
        }
        // なお画面外突入時はランダム
        if (addRect.top < 0) {
            val randomValue = randomValue(fontSize)
            addRect.bottom = (randomValue + fontSize).toInt()
            addRect.top = randomValue
        }
        // 配列に入れる
        val data = DrawCommentData(
            comment = comment,
            command = command,
            drawStartTimeMs = drawStartTimeMs,
            drawStartTimeSec = drawStartTimeSec,
            measure = measure,
            fontSize = fontSize,
            rect = addRect,
            position = "shita"
        )
        commentList.add(data)
    }

    /**
     * 現在流れているコメントを取得する。多分今の時間から5秒前のコメントまでは取れる。
     * */
    fun getDrawingCommentList(sec: Long = -1) = commentList.toList().filter {
        val current = if (sec != -1L) sec else currentPosSec
        val minus = current - it.drawStartTimeMs
        val left = finalWidth - (minus / 2)
        // ３秒のコメントを返す
        (it.drawStartTimeSec in ((current - 5)..current)) && it.position == "naka"
    }

    /**
     * 現在表示されてる上コメントを取得する。いまから三秒前までが対象
     * */
    fun getDrawingUeCommentList() = commentList.toList().filter {
        // いまから三秒前のコメントまででかつ上コメントの場合
        it.drawStartTimeSec in (currentPosSec - 3)..currentPosSec && it.position == "ue"
    }

    /**
     * 現在表示されてる下コメントを取得する。いまから三秒前までが対象
     * */
    fun getDrawingShitaCommentList() = commentList.toList().filter {
        // いまから三秒前のコメントまででかつ下コメントの場合
        it.drawStartTimeSec in (currentPosSec - 3)..currentPosSec && it.position == "shita"
    }

    /**
     * コマンドに合った文字の大きさを返す
     * @param command big/small など
     * @return フォントサイズ
     * */
    fun getCommandFontSize(command: String): Float {
        // コメント行を自由に設定する設定
        val isCustomCommentLine = prefSetting.getBoolean("setting_comment_canvas_custom_line_use", false)
        val customCommentLine = prefSetting.getString("setting_comment_canvas_custom_line_value", "10")?.toInt() ?: 20
        // 強制10行表示モード
        val is10LineMode = prefSetting.getBoolean("setting_comment_canvas_10_line", false)
        // フォントサイズ
        val defaultFontSize = when {
            is10LineMode -> (finalHeight / 10).toFloat() // 強制10行確保
            isCustomCommentLine -> (finalHeight / customCommentLine).toFloat() // 自由に行設定
            else -> 20 * resources.displayMetrics.scaledDensity // でふぉ
        }
        return when {
            command.contains("big") -> {
                (defaultFontSize * 1.3).toFloat()
            }
            command.contains("small") -> {
                (defaultFontSize * 0.8).toFloat()
            }
            else -> defaultFontSize
        }
    }

    /**
     * コメント配列を消す
     * それだけ
     * */
    fun clearList() {
        commentList.clear()
    }

    /**
     * 指定したフォントサイズのPaintを生成する関数
     * */
    private fun getBlackCommentTextPaint(fontSize: Float): Paint {
        val paint = Paint()
        paint.textSize = fontSize
        return paint
    }

    /** Heightが0のとき対策 */
    @Deprecated("使えない。F2を押して置き換えよう", ReplaceWith("finalHeight"))
    private fun getHeightOrDefault(default: Int = 893) = if (height == 0) {
        default
    } else {
        height
    }

    /** Widthが0のとき対策 */
    @Deprecated("使えない。F2を押して置き換えよう", ReplaceWith("finalWidth"))
    fun getWidthOrDefault(default: Int = 1190) = if (width == 0) {
        default
    } else {
        width
    }

    private fun randomValue(fontSize: Float): Int {
        return if (height > fontSize) {
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
        paint.alpha = (getAlphaFloat() * 225).toInt()
        blackPaint.alpha = (getAlphaFloat() * 225).toInt()
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
            measure = getBlackCommentTextPaint(commandFontSize).measureText(comment)
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
            command.replace("blue2", "").contains("ue") && command.replace("blue", "").contains("ue") -> true
            // まあ上こめ
            command.contains("ue") -> true
            // ちがう！！！
            else -> false
        }
    }

    /**
     * コメントの更新頻度を取得する関数。
     * */
    fun getUpdateMs(): Long {
        // コメントキャンバスの更新頻度
        // コメントの更新頻度をfpsで設定するかどうか
        val isSetSpeedFPS = prefSetting.getBoolean("setting_comment_canvas_speed_fps_enable", false)
        return if (isSetSpeedFPS) {
            // fpsで設定
            val fps = prefSetting.getString("setting_comment_canvas_speed_fps", "60")?.toIntOrNull() ?: 60
            // 1000で割る （例：1000/60=16....）
            (1000 / fps)
        } else {
            // ミリ秒で指定
            prefSetting.getString("setting_comment_canvas_timer", "10")?.toIntOrNull() ?: 10
        }.toLong()
    }

    /**
     * 透明度を返す。[Paint.setAlpha]に入れるときは255掛けないといけません
     *
     * 半透明にするぐらいなら消したほうが良くねというお気持ちもある
     * */
    fun getAlphaFloat() = prefSetting.getString("setting_comment_alpha", "1.0")?.toFloat() ?: 1.0F

}


/**
 * [ReCommentCanvas]で使うコメント情報が詰まったデータクラス。
 *
 * 生放送と違い動画は再生時間に合わせないといけないので引数が多い
 *
 * @param drawStartTimeSec コメントを流す時間。秒
 * @param drawStartTimeMs コメントを流す時間。ミリ秒
 * @param command コマンド。色の指定など
 * @param comment コメント本文
 * @param fontSize 文字の大きさ
 * @param measure コメントの長さ。
 * @param position naka か ue か　shita。めんどいのでコメントの位置は[command]ではなく[position]で取るのでちゃんとしてね
 * @param rect 当たり判定計算やコメントの位置で使う。ただコメントの当たり判定処理が終われば使わなくなるので、あとはコメントずらすのに使ってる
 * */
data class DrawCommentData(
    val comment: String,
    val command: String,
    val fontSize: Float,
    val rect: Rect,
    val measure: Float,
    val drawStartTimeSec: Long,
    val drawStartTimeMs: Long,
    val position: String = "naka"
)