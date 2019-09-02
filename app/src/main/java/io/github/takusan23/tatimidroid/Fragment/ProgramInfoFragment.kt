package io.github.takusan23.tatimidroid.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.adapter_community_layout.*
import kotlinx.android.synthetic.main.fragment_program_info.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.time.temporal.WeekFields
import java.util.*

class ProgramInfoFragment : Fragment() {

    var liveId = ""
    var usersession = ""
    lateinit var pref_setting: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_program_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //番組ID取得
        liveId = activity?.intent?.getStringExtra("liveId") ?: ""
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        usersession = pref_setting.getString("user_session", "") ?: ""

        getProgramInfo()
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
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = startTime.toLong() * 1000L
                    val month = calendar.get(Calendar.MONTH)
                    val date = calendar.get(Calendar.DATE)
                    val week = calendar.get(Calendar.DAY_OF_WEEK)
                    val hour = calendar.get(Calendar.HOUR)
                    val minute = calendar.get(Calendar.MINUTE)
                    val weekList = listOf<String>("", "日", "月", "火", "水", "木", "金", "土")
                    val time = "${month + 1}/${date} ${weekList.get(week)} ${hour}:${minute}"
                    //コミュ
                    val community = data.getJSONObject("socialGroup")
                    val community_name = community.getString("name")
                    val community_level = community.getString("communityLevel")
                    //配信者
                    val broadcaster = data.getJSONObject("broadcaster")
                    val name = broadcaster.getString("name")
                    //UI
                    activity?.runOnUiThread {
                        fragment_program_info_broadcaster_name.text = "${getString(R.string.broadcaster)} : $name"
                        fragment_program_info_start.text = time
                        fragment_program_info_title.text = title
                        fragment_program_info_description.text = HtmlCompat.fromHtml(description, FROM_HTML_MODE_COMPACT)
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

}