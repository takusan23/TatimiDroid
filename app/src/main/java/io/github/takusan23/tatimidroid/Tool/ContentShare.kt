package io.github.takusan23.tatimidroid.Tool

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ShareCompat
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 共有画面を出す。Activity Result APIを使った。
 *
 * あと、ニコ動はURLのパラメーター「from」を使うことで再生時間を指定できる
 *
 * 例：`https://nico.ms/sm157?from=30` // きしめんを30秒から再生
 *
 * インスタンス化する場合は書く場所に注意してください。(ライフサイクル的に見てonCreateの前でインスタンス化しないとだめ？)
 *
 * @param fragment Activity Result API を利用するために必要
 * */
class ContentShare(private val fragment: Fragment) {

    /** 保存する画像のBitmap */
    private var playerViewBitmap: Bitmap? = null

    /** 共有時につける文字列 */
    private var message: String? = null

    /** 番組ID / 動画ID */
    private var contentId: String? = null

    /** タイトル */
    private var contentName: String? = null

    /** 再生時間（秒） */
    private var fromTimeSecond: Int? = null

    // 保存する。Activity Result APIを使って
    private val callback = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {
            // 成功時
            try {
                //保存する
                val outputStream = fragment.context?.contentResolver?.openOutputStream(result.data?.data!!)
                playerViewBitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                //共有画面出す
                shareContent(contentId, contentName, fromTimeSecond, result.data?.data!!, message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 写真付きで共有をする。内部でコルーチンを使ってるから非同期です
     * @param commentCanvas コメントView。ReCommentCanvasでもCommentCanvasでもどうぞ
     * @param playerView ExoPlayerにセットしてるSurfaceView
     * @param message 共有時になにか追加したい場合は入れてね
     * @param fromTimeSecond 再生開始時間（指定したければ）。生放送ではnull、動画なら秒
     * @param programId 番組 か 動画 ID
     * @param programName 名前
     * */
    fun shareContentAttachPicture(playerView: SurfaceView, commentCanvas: View, programId: String?, programName: String?, fromTimeSecond: Int?, message: String? = "") {
        this.message = message
        this.contentId = programId
        this.contentName = programName
        this.fromTimeSecond = fromTimeSecond
        fragment.lifecycleScope.launch {
            // ExoPlayerのViewをキャプチャーする
            playerViewBitmap = capturePlayerView(playerView) ?: return@launch
            // コメントもキャプチャする
            val commentCanvasBitmap = captureCommentView(commentCanvas)
            // Bitmapを重ねる
            //動画とコメントのBitmapを重ねて。。重ねて
            val canvas = Canvas(playerViewBitmap!!)
            canvas.drawBitmap(commentCanvasBitmap, 0F, 0F, null)
            // SAFを開いて保存先を選んでもらう
            val simpleDateFormat = SimpleDateFormat("HH:mm:ss")
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_TITLE, "$programId-${simpleDateFormat.format(System.currentTimeMillis())}.png")
            callback.launch(intent)
        }
    }

    /**
     * 共有画面出す。写真なし
     * @param uri 画像を付ける場合はUriを入れてください。
     * @param message タイトル、ID、URL以外に文字列を入れたい場合は指定してください。
     * @param programId 番組 か 動画 ID
     * @param programName 名前
     * @param fromTimeSecond 再生開始時間（指定したければ）。生放送ではnull、動画なら秒
     * */
    fun shareContent(programId: String?, programName: String?, fromTimeSecond: Int?, uri: Uri? = null, message: String? = "") {
        this.message = message
        this.contentId = programId
        this.contentName = programName
        this.fromTimeSecond = fromTimeSecond
        val builder = ShareCompat.IntentBuilder(fragment.requireActivity())
        builder.setChooserTitle(programName)
        // 時間指定パラメーター付き？
        val url = if (fromTimeSecond != null) {
            "https://nico.ms/$programId?from=${fromTimeSecond}"
        } else {
            "https://nico.ms/$programId"
        }
        builder.setText("$programName\n#$programId\n$url\n$message")
        if (uri != null) {
            builder.setStream(uri)
            builder.setType("text/jpeg")
        } else {
            builder.setType("text/plain")
        }
        builder.startChooser()
    }

    /** 再生画面（動画とか生放送とか）をキャプチャーする。なおコールバックな関数を使ったためコルーチンです */
    private suspend fun capturePlayerView(playerView: SurfaceView) = withContext(Dispatchers.Main) {
        // コールバック形式な関数をコルーチン化できる有能
        suspendCoroutine<Bitmap?> { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val bitmap = Bitmap.createBitmap(playerView.width, playerView.height, Bitmap.Config.ARGB_8888)
                val locationOfViewInWindow = IntArray(2)
                playerView.getLocationInWindow(locationOfViewInWindow)
                try {
                    PixelCopy.request(
                        playerView, bitmap, { copyResult ->
                            if (copyResult == PixelCopy.SUCCESS) {
                                result.resume(bitmap)
                            } else {
                                Toast.makeText(fragment.requireContext(), fragment.requireContext().getString(R.string.bitmap_error), Toast.LENGTH_SHORT).show()
                                result.resume(null)
                            }
                        },
                        Handler(Looper.getMainLooper())
                    )
                } catch (e: IllegalArgumentException) {
                    // PixelCopy may throw IllegalArgumentException, make sure to handle it
                    e.printStackTrace()
                }
            } else {
                // Android 7以下のユーザー。
                playerView.isDrawingCacheEnabled = true
                result.resume(playerView.drawingCache)
                playerView.isDrawingCacheEnabled = false
            }
        }
    }

    /** コメントのViewをキャプチャする */
    private fun captureCommentView(commentCanvas: View): Bitmap {
        return commentCanvas.drawToBitmap()
    }
}