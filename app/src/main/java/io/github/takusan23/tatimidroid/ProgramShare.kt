package io.github.takusan23.tatimidroid

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.DisplayMetrics
import android.view.PixelCopy
import android.view.Surface
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.graphics.drawable.toDrawable
import java.lang.Exception
import java.net.URI
import java.security.cert.Extension
import java.util.*

class ProgramShare(
    val activity: AppCompatActivity,
    val view: View,
    val programName: String,
    val programId: String
) {
    // onActivityResult でリクエストする値。
    companion object {
        const val mediaProtectionCode = 810
        const val requestCode = 514
    }

    var commentBitmap: Bitmap? = null
    var videoBitmap: Bitmap? = null

    var saveBitmap: Bitmap? = null
    var saveUri: Uri? = null

    /**
     * 画像付きで共有するとき
     * */
    fun shareAttacgImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //動画部分のBitmap
            getBitmapOreoVideo()
        } else {
            getBitmapNougat()
        }
    }

    //コメント部分のBitmapを生成
    fun getBitmapOreoComment() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val locationOfViewInWindow = IntArray(2)
            view.getLocationInWindow(locationOfViewInWindow)
            try {
                PixelCopy.request(
                    activity.window,
                    Rect(
                        locationOfViewInWindow[0],
                        locationOfViewInWindow[1],
                        locationOfViewInWindow[0] + view.width,
                        locationOfViewInWindow[1] + view.height
                    ),
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            //nullチェック
                            commentBitmap = bitmap

                            //動画とコメントのBitmapを重ねて。。重ねて
                            val canvas = Canvas(videoBitmap!!)
                            canvas.drawBitmap(commentBitmap!!, 0F, 0F, null)
                            //完成Bitmap
                            saveBitmap = videoBitmap

                            //Uri生成
                            saveStorageAccessFramework()

                        } else {
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.bitmap_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    Handler()
                )
            } catch (e: IllegalArgumentException) {
                // PixelCopy may throw IllegalArgumentException, make sure to handle it
                e.printStackTrace()
            }
        }
    }


    //動画部分のBitmapを生成
    fun getBitmapOreoVideo() {
        var returnBitmap: Bitmap? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val locationOfViewInWindow = IntArray(2)
            view.getLocationInWindow(locationOfViewInWindow)
            try {
                PixelCopy.request(
                    (view as SurfaceView),
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            //nullチェック
                            videoBitmap = bitmap
                            //コメント部分のBitmap取得
                            getBitmapOreoComment()
                        } else {
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.bitmap_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    Handler()
                )
            } catch (e: IllegalArgumentException) {
                // PixelCopy may throw IllegalArgumentException, make sure to handle it
                e.printStackTrace()
            }
        }
    }

    //レガシーよう
    fun getBitmapNougat() {
        //PixelColor API がOreo以降じゃないと利用できないため
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true)
        val bitmap = view.getDrawingCache(true).copy(Bitmap.Config.RGB_565, false)
        //完成Bitmap
        saveBitmap = videoBitmap
        saveStorageAccessFramework()
    }

    //Storage Access Frameworkに画像保存する
    //ここで保存するのではなくUriを生成してonActivityResult()で保存するらしい。
    fun saveStorageAccessFramework() {
        //時間
        val date = Calendar.getInstance()
        val hour = date.get(Calendar.HOUR_OF_DAY)
        val minute = date.get(Calendar.MINUTE)
        val second = date.get(Calendar.SECOND)
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_TITLE, "$programId-$hour:$minute:$second.png")
        activity.startActivityForResult(intent, requestCode)
    }

    //保存
    fun saveActivityResult(uri: Uri) {
        try {
            //保存する
            val outputStream = activity.contentResolver.openOutputStream(uri)
            saveBitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            saveUri = uri
            //共有画面出す
            showShareScreenAttachScreenShot()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //画像共有共有画面出す
    private fun showShareScreenAttachScreenShot() {
        if (saveBitmap != null && saveUri != null) {
            val builder = ShareCompat.IntentBuilder.from(activity)
            builder.setChooserTitle(programName)
            builder.setText(programId)
            builder.setStream(saveUri)
            builder.setType("text/jpeg")
            builder.startChooser()
        }
    }

    //共有画面
    fun showShareScreen() {
        val builder = ShareCompat.IntentBuilder.from(activity)
        builder.setChooserTitle(programName)
        builder.setText(programId)
        builder.setStream(saveUri)
        builder.setType("text/plain")
        builder.startChooser()
    }
}