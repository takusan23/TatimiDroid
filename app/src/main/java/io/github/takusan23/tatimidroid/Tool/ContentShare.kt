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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.view.drawToBitmap
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
 * @param activity Activity Result API (保存先を選んでもらう) で使う
 * @param programId 番組 か 動画 ID
 * @param programName 名前
 * */
class ContentShare(val activity: AppCompatActivity, val programId: String, val programName: String) {

    /**
     * 写真付きで共有をする。内部でコルーチンを使ってるから非同期です
     * @param commentCanvas コメントView。ReCommentCanvasでもCommentCanvasでもどうぞ
     * @param playerView ExoPlayerにセットしてるSurfaceView
     * @param message 共有時になにか追加したい場合は入れてね
     * */
    fun shareContentAttachPicture(playerView: SurfaceView, commentCanvas: View, message: String = "") {
        activity.lifecycleScope.launch {
            // ExoPlayerのViewをキャプチャーする
            val playerViewBitmap = capturePlayerView(playerView) ?: return@launch
            // コメントもキャプチャする
            val commentCanvasBitmap = captureCommentView(commentCanvas)
            // Bitmapを重ねる
            //動画とコメントのBitmapを重ねて。。重ねて
            val canvas = Canvas(playerViewBitmap)
            canvas.drawBitmap(commentCanvasBitmap, 0F, 0F, null)
            // 保存する。Activity Result APIを使って
            val callback = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {
                    // 成功時
                    try {
                        //保存する
                        val outputStream = activity.contentResolver.openOutputStream(result.data?.data!!)
                        playerViewBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        //共有画面出す
                        shareContent(result.data?.data!!, message)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
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
     * */
    fun shareContent(uri: Uri? = null, message: String = "") {
        val builder = ShareCompat.IntentBuilder.from(activity)
        builder.setChooserTitle(programName)
        builder.setText("$programName\n#$programId\nhttps://nico.ms/$programId\n$message")
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
                                Toast.makeText(activity, activity.getString(R.string.bitmap_error), Toast.LENGTH_SHORT).show()
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