package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.adapter_community_layout.*
import kotlinx.android.synthetic.main.fragment_program_info.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.text.SimpleDateFormat
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.temporal.WeekFields
import java.util.*
import kotlin.concurrent.thread

class ProgramInfoFragment : Fragment() {

    var liveId = ""
    var usersession = ""
    lateinit var pref_setting: SharedPreferences

    // ユーザー、コミュID
    var userId = ""
    var communityId = ""

    // タグ変更に使うトークン
    var tagToken = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_program_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //番組ID取得
        liveId = arguments?.getString("liveId") ?: ""
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        usersession = pref_setting.getString("user_session", "") ?: ""

        getProgramInfo()

        // タグ取得
        programInfoCoroutine()

        // ユーザーフォロー
        fragment_program_info_broadcaster_follow_button.setOnClickListener {
            requestFollow(userId) {
                Toast.makeText(context, "ユーザーをフォローしました。", Toast.LENGTH_SHORT).show()
            }
        }

        // コミュニティフォロー
        fragment_program_info_community_follow_button.setOnClickListener {
            requestCommunityFollow(communityId) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "コミュニティをフォローしました。\n$communityId", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        // タグ編集
        fragment_program_info_tag_add_button.setOnClickListener {
            val nicoLiveTagBottomFragment = NicoLiveTagBottomFragment()
            val bundle = Bundle().apply {
                putString("liveId", liveId)
                putString("tagToken", tagToken)
            }
            nicoLiveTagBottomFragment.arguments = bundle
            nicoLiveTagBottomFragment.programFragment = this@ProgramInfoFragment
            nicoLiveTagBottomFragment.show(childFragmentManager, "bottom_tag")
        }

        //getCommentWatchingCount()

    }

    /** コルーチン */
    fun programInfoCoroutine(){
        GlobalScope.launch {
            val responseString = getNicoLiveHTML().await()
            val html = Jsoup.parse(responseString)
            if (html.getElementById("embedded-data") != null) {
                val json = html.getElementById("embedded-data").attr("data-props")
                val jsonObject = JSONObject(json)
                // コミュフォロー中？
                val isCommunityFollow =
                    jsonObject.getJSONObject("socialGroup").getBoolean("isFollowed")
                if (isCommunityFollow) {
                    activity?.runOnUiThread {
                        // 押せないように
                        fragment_program_info_community_follow_button.isEnabled = false
                        fragment_program_info_community_follow_button.text = "フォロー済みです"
                    }
                }
                //たぐ
                val tag = jsonObject.getJSONObject("program").getJSONObject("tag")
                val tagsList = tag.getJSONArray("list")
                if (tagsList.length() != 0) {
                    activity?.runOnUiThread {
                        for (i in 0 until tagsList.length()) {
                            val tag = tagsList.getJSONObject(i)
                            val text = tag.getString("text")
                            val isNicopedia = tag.getBoolean("existsNicopediaArticle")
                            val nicopediaUrl =
                                "https://dic.nicovideo.jp/${tag.getString("nicopediaArticlePageUrlPath")}"
                            //ボタン作成
                            val button = MaterialButton(context!!)
                            button.text = text
                            if (isNicopedia) {
                                button.setOnClickListener {
                                    val intent = Intent(Intent.ACTION_VIEW, nicopediaUrl.toUri())
                                    startActivity(intent)
                                }
                            } else {
                                button.isEnabled = false
                            }
                            fragment_program_info_tag_linearlayout.addView(button)
                        }
                    }
                }
                // タグの登録に必要なトークンを取得
                tagToken = tag.getString("apiToken")
            }
        }
    }

    /*番組情報取得*/
    fun getProgramInfo() {
        val request = Request.Builder()
            .url("https://live2.nicovideo.jp/watch/${liveId}/programinfo")
            .header("Cookie", "user_session=${usersession}")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    //パース
                    val jsonObject = JSONObject(response.body?.string())
                    //番組情報。タグは取れない？
                    //↑ゲーム機版niconicoのAPIなら取れるっぽいけどいつ終わるかわからん。getplayerstatus使ってるやつが言うなって話ですが
                    val data = jsonObject.getJSONObject("data")
                    val title = data.getString("title")
                    val description = data.getString("description")
                    val startTime = data.getString("beginAt")
                    //UnixTime -> Calender
                    //放送開始時刻
                    val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                    val startTimeFormat = simpleDateFormat.format(startTime.toLong() * 1000)
                    //コミュ
                    val community = data.getJSONObject("socialGroup")
                    communityId = community.getString("id")
                    val community_name = community.getString("name")
                    val community_level = community.getString("communityLevel")
                    //配信者
                    val broadcaster = data.getJSONObject("broadcaster")
                    val name = broadcaster.getString("name")
                    userId = broadcaster.getString("id")
                    //UI
                    activity?.runOnUiThread {
                        fragment_program_info_broadcaster_name.text =
                            "${getString(R.string.broadcaster)} : $name"
                        fragment_program_info_start.text = startTimeFormat
                        fragment_program_info_title.text = title
                        fragment_program_info_description.text =
                            HtmlCompat.fromHtml(description, FROM_HTML_MODE_COMPACT)
                        fragment_program_info_community_name.text =
                            "${getString(R.string.community_name)} : $community_name"
                        fragment_program_info_community_level.text =
                            "${getString(R.string.community_level)} : $community_level"
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun getNicoLiveHTML(): Deferred<String> = GlobalScope.async {
        val url = "https://live2.nicovideo.jp/watch/$liveId"
        val request = Request.Builder()
            .get()
            .url(url)
            .addHeader("User-Agent", "TatimiDroid;@takusan_23")
            .addHeader("Cookie", "user_session=$usersession")
            .build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            return@async response.body?.string() ?: ""
        } else {
            return@async ""
        }
    }

    /**
     * ユーザーをフォローする関数。
     * @param userId ユーザーID
     * @param response 成功時呼ばれます。UIスレッドではない。
     * */
    fun requestFollow(userId: String, response: (Response) -> Unit) {
        val request = Request.Builder().apply {
            url("https://public.api.nicovideo.jp/v1/user/followees/niconico-users/${userId}.json")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$usersession")
            // これがないと 200 が帰ってこない
            header(
                "X-Request-With",
                "https://live2.nicovideo.jp/watch/$liveId?_topic=live_user_program_onairs"
            )
            post("{}".toRequestBody()) // 送る内容は｛｝ていいらしい。
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val status =
                        JSONObject(response.body?.string()).getJSONObject("meta").getInt("status")
                    if (status == 200) {
                        // 高階関数をよぶ
                        response(response)
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    /**
     * コミュニティをフォローする関数。
     * @param communityId コミュニティーID coから
     * @param response 成功したら呼ばれます。
     * */
    fun requestCommunityFollow(communityId: String, response: (Response) -> Unit) {
        val formData = FormBody.Builder().apply {
            add("mode", "commit")
            add("title", "フォローリクエスト")
            add("comment", "")
            add("notify", "")
        }.build()
        val request = Request.Builder().apply {
            url("https://com.nicovideo.jp/motion/${communityId}")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$usersession")
            header("Content-Type", "application/x-www-form-urlencoded")
            // Referer これないと200が帰ってくる。（ほしいのは302 Found）
            header("Referer", "https://com.nicovideo.jp/motion/$communityId")
            post(formData) // これ送って何にするの？
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // 302 Foundのとき成功？
                    response(response)
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

}