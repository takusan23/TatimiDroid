package io.github.takusan23.tatimidroid.NicoLiveAPI

import android.content.Context
import android.os.Looper
import android.widget.Toast
import io.github.takusan23.tatimidroid.R
import okhttp3.*
import java.io.IOException
import java.util.logging.Handler

/**
 * タイムシフト予約をするAPIまとめ
 * */
class NicoLiveTimeShiftAPI(var context: Context?, var user_session: String, var liveId: String) {

    /**
     * タイムシフト登録
     * @param successful レスポンスが帰ってくれば呼ばれます。成功かもしれないしそこはわからん。
     * */
    fun registerTimeShift(successful: (Response) -> Unit) {
        val postFormData = FormBody.Builder().apply {
            // 番組IDからlvを抜いた値を指定する
            add("vid", liveId.replace("lv", ""))
            add("overwrite", "0")
        }.build()
        val request = Request.Builder().apply {
            url("https://live.nicovideo.jp/api/timeshift.reservations")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$user_session")
            header("Content-Type", "application/x-www-form-urlencoded")
            header("Origin", "https://live2.nicovideo.jp") // これが必須の模様
            post(postFormData)
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(context?.getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                successful(response)
            }
        })
    }

    /**
     * タイムシフトを削除する
     * @param successful 成功したら呼ばれます。
     * */
    fun deleteTimeShift(successful: (Response) -> Unit) {
        val request = Request.Builder().apply {
            // 番組IDからlvを抜いた値を指定する
            url(
                "https://live.nicovideo.jp/api/timeshift.reservations?vid=${liveId.replace(
                    "lv",
                    ""
                )}"
            )
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$user_session")
            header("Content-Type", "application/x-www-form-urlencoded")
            header("Origin", "https://live2.nicovideo.jp") // これが必須の模様
            delete()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(context?.getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    successful(response)
                } else {
                    showToast("${context?.getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }


    private fun showToast(message: String?) {
        android.os.Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}