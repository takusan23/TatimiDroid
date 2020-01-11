package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
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
import java.time.temporal.WeekFields
import java.util.*

class ProgramInfoFragment : Fragment() {

    var liveId = ""
    var usersession = ""
    lateinit var pref_setting: SharedPreferences

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

        GlobalScope.launch {
            val list = getNicoLiveHTML().await()
            if (list.isNotEmpty()) {
                activity?.runOnUiThread {
                    val jsonArray = JSONArray(list)
                    for (i in 0 until jsonArray.length()) {
                        val tag = jsonArray.getJSONObject(i)
                        val text = tag.getString("text")
                        val isNicopedia = tag.getBoolean("existsNicopediaArticle")
                        val nicopediaUrl =
                            "https://dic.nicovideo.jp/${tag.getString("nicopediaArticlePageUrlPath")}"
                        //ボタン作成
                        val button = MaterialButton(context!!)
                        button.text = text
                        if(isNicopedia){
                            button.setOnClickListener {
                                val intent = Intent(Intent.ACTION_VIEW,nicopediaUrl.toUri())
                                startActivity(intent)
                            }
                        }else{
                            button.isEnabled = false
                        }
                        fragment_program_info_tag_linearlayout.addView(button)
                    }
                }
            }
        }

        //getCommentWatchingCount()

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
                    val community_name = community.getString("name")
                    val community_level = community.getString("communityLevel")
                    //配信者
                    val broadcaster = data.getJSONObject("broadcaster")
                    val name = broadcaster.getString("name")
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
            val html = Jsoup.parse(response.body?.string())
            if (html.getElementById("embedded-data") != null) {
                val json = html.getElementById("embedded-data").attr("data-props")
                val jsonObject = JSONObject(json)
                //たぐ
                val tags =
                    jsonObject.getJSONObject("program").getJSONObject("tag").getJSONArray("list")
                return@async tags.toString()
            } else {
                return@async ""
            }
        } else {
            return@async ""
        }
    }

}