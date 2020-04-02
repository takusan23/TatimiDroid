package io.github.takusan23.tatimidroid.NicoVideo

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_info.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException

class NicoVideoInfoFragment : Fragment() {

    lateinit var pref_setting: SharedPreferences

    var id = ""
    var usersession = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        //動画ID受け取る（sm9とかsm157とか）
        id = arguments?.getString("id") ?: "sm157"

        usersession = pref_setting.getString("user_session", "") ?: ""

        getNicoVideoWebPage()

        fragment_nicovideo_info_description_textview.movementMethod =
            LinkMovementMethod.getInstance();

    }


    /*
    *
    * https://nmsg.nicovideo.jp/api.json/ を叩く時に必要な情報をスクレイピングして取得
    * これで動くと思ってたんだけど「dmcInfo」がない動画が存在するんだけどどういう事？。
    *     */
    fun getNicoVideoWebPage() {
        //番組ID
        //視聴モード（ユーザーセッション付き）
        val request = Request.Builder()
            .url("https://www.nicovideo.jp/watch/${id}")
            .header("Cookie", "user_session=${usersession}")
            .header("User-Agent", "TatimiDroid;@takusan_23")
            .get()
            .build()

        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("${getString(R.string.error)}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val response_string = response.body?.string()
                    //HTML解析
                    val html = Jsoup.parse(response_string)
                    //謎のJSON取得
                    //この部分長すぎてChromeだとうまくコピーできないんだけど、Edgeだと完璧にコピーできたぞ！
                    if (html.getElementById("js-initial-watch-data") != null) {
                        val data_api_data = html.getElementById("js-initial-watch-data")
                        val json = JSONObject(data_api_data.attr("data-api-data"))

                        val threadObject = json.getJSONObject("thread")
                        val commentCount = threadObject.getString("commentCount")

                        //ユーザー情報。公式動画だと取れない。
                        var nickname = ""
                        var userId = ""
                        var iconURL = ""
                        if (!json.isNull("owner")) {
                            val ownerObject = json.getJSONObject("owner")
                            nickname = ownerObject.getString("nickname")
                            userId = ownerObject.getString("id")
                            iconURL = ownerObject.getString("iconURL")
                        }
                        //公式動画では代わりにチャンネル取る。
                        if (!json.isNull("channel")) {
                            val ownerObject = json.getJSONObject("channel")
                            nickname = ownerObject.getString("name")
                            userId = ownerObject.getString("globalId")
                            //iconURL = ownerObject.getString("iconURL")
                        }

                        val videoObject = json.getJSONObject("video")

                        val id = videoObject.getString("id")
                        val title = videoObject.getString("title")
                        val description = videoObject.getString("description")
                        val postedDateTime = videoObject.getString("postedDateTime")

                        val viewCount = videoObject.getString("viewCount")
                        val mylistCount = videoObject.getString("mylistCount")

                        //タグ
                        val tagArray = json.getJSONArray("tags")

                        activity?.runOnUiThread {
                            //UIスレッド
                            fragment_nicovideo_info_title_textview.text = title
                            fragment_nicovideo_info_description_textview.text =
                                HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT)
                            fragment_nicovideo_info_upload_textview.text =
                                "${getString(R.string.post_date)}：$postedDateTime"


                            fragment_nicovideo_info_play_count_textview.text =
                                "${getString(R.string.play_count)}：$viewCount"

                            fragment_nicovideo_info_mylist_count_textview.text =
                                "${getString(R.string.mylist)}：$mylistCount"

                            fragment_nicovideo_info_comment_count_textview.text =
                                "${getString(R.string.comment_count)}：$commentCount"

                            fragment_nicovideo_info_owner_textview.text = nickname

                            //たぐ
                            for (i in 0 until tagArray.length()) {
                                val tag = tagArray.getJSONObject(i)
                                val name = tag.getString("name")
                                val isDictionaryExists =
                                    tag.getBoolean("isDictionaryExists") //大百科があるかどうか


                                val linearLayout = LinearLayout(context)
                                linearLayout.orientation = LinearLayout.HORIZONTAL

                                //ボタン
                                val button = Button(context)
                                //大きさとか
                                val linearLayoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                linearLayoutParams.weight = 1F

                                button.layoutParams = linearLayoutParams
                                button.text = name
                                linearLayout.addView(button)

                                if (isDictionaryExists) {
                                    val dictionaryButton = Button(context)
                                    dictionaryButton.text = getString(R.string.dictionary)
                                    linearLayout.addView(dictionaryButton)
                                    //大百科ひらく
                                    dictionaryButton.setOnClickListener {
                                        openBrowser("https://dic.nicovideo.jp/a/$name")
                                    }
                                }

                                fragment_nicovideo_info_title_linearlayout.addView(linearLayout)
                            }


                            //ブラウザで再生。このアプリで再生できるようにするかは考え中。
                            fragment_nicovideo_info_open_browser.setOnClickListener {
                                openBrowser("https://www.nicovideo.jp/watch/$id")
                            }

                            //ユーザーページ
                            fragment_nicovideo_info_owner_textview.setOnClickListener {
                                if (userId.contains("co")) {
                                    openBrowser("https://www.nicovideo.jp/user/$userId")
                                } else {
                                    //チャンネルの時、ch以外にもそれぞれアニメの名前を入れても通る。例：te-kyu2 / gochiusa など
                                    openBrowser("https://ch.nicovideo.jp/$userId")
                                }
                            }

                        }

                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    fun openBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)

    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}