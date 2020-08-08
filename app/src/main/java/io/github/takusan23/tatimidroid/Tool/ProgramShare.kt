package io.github.takusan23.tatimidroid.Tool

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoFragment
import io.github.takusan23.tatimidroid.NicoLive.CommentFragment
import io.github.takusan23.tatimidroid.R
import java.text.SimpleDateFormat

/**
 * 共有画面を出す
 * */
class ProgramShare(val activity: AppCompatActivity, val view: View, val programName: String, val programId: String) {
    // onActivityResult でリクエストする値。
    companion object {
        const val mediaProtectionCode = 810
        const val requestCode = 514
    }

    var commentBitmap: Bitmap? = null
    var videoBitmap: Bitmap? = null

    var saveBitmap: Bitmap? = null
    var saveUri: Uri? = null

    /** 画像つきで共有の時に一時的に持っておく */
    private var message = ""

    /**
     * 画像付きで共有するとき
     * @param message タイトル、ID、URL以外に文字列を入れたい場合は指定してください。
     * */
    fun shareAttachImage(message: String = "") {
        this.message = message
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //動画部分のBitmap
            getBitmapOreoVideo()
        } else {
            getBitmapNougat()
        }
    }

    //コメント部分のBitmapを生成
    private fun getBitmapOreoComment() {
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
    private fun getBitmapOreoVideo() {
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
        //完成Bitmap
        saveBitmap = videoBitmap
        saveStorageAccessFramework()
    }

    //Storage Access Frameworkに画像保存する
    //ここで保存するのではなくUriを生成してonActivityResult()で保存するらしい。
    fun saveStorageAccessFramework() {
        //時間
        val simpleDateFormat = SimpleDateFormat("HH:mm:ss")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_TITLE, "$programId-${simpleDateFormat.format(System.currentTimeMillis())}.png")
        //Fragmentにする
        val fragment = if (programId.contains("lv")) {
            activity.supportFragmentManager.findFragmentByTag(programId) as CommentFragment
        } else {
            activity.supportFragmentManager.findFragmentByTag(programId) as NicoVideoFragment
        }
        fragment.startActivityForResult(intent, requestCode)
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

    /**
     * 画像つき共有画面出す。
     * */
    private fun showShareScreenAttachScreenShot() {
        if (saveBitmap != null && saveUri != null) {
            val builder = ShareCompat.IntentBuilder.from(activity)
            builder.setChooserTitle(programName)
            builder.setText("$programName\n#$programId\nhttps://nico.ms/$programId\n$message")
            builder.setStream(saveUri)
            builder.setType("text/jpeg")
            builder.startChooser()
        }
    }

    /**
     * 共有画面出す。
     * @param message タイトル、ID、URL以外に文字列を入れたい場合は指定してください。
     * */
    fun showShareScreen(message: String = "") {
        val builder = ShareCompat.IntentBuilder.from(activity)
        builder.setChooserTitle(programName)
        builder.setText("$programName\n#$programId\nhttps://nico.ms/$programId\n$message")
        builder.setStream(saveUri)
        builder.setType("text/plain")
        builder.startChooser()
    }
}