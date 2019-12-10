package io.github.takusan23.tatimidroid.NicoVideo

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoSelectAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_select.*
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException

class NicoVideoSelectFragment : Fragment() {

    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var nicoVideoSelectAdapter: NicoVideoSelectAdapter

    lateinit var pref_setting: SharedPreferences

    var usersession = ""

    lateinit var darkModeSupport: DarkModeSupport

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //タイトル
        (activity as AppCompatActivity).supportActionBar?.title =
            getString(R.string.video_comment_get)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        usersession = pref_setting.getString("user_session", "") ?: ""

        darkModeSupport = DarkModeSupport(context!!)

        //動画IDから
        setVideoID()

        //RecyclerView初期化
        initRecyclerView()

        //とりあえず視聴履歴
        callHistoryAPI()

        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            fragment_nicovideo_select_tab_layout.setBackgroundColor(Color.parseColor("#000000"))
        }

        fragment_nicovideo_select_tab_layout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    getString(R.string.post_video) -> {
                        //投稿動画
                        getPOSTVideoHTML()
                    }
                    getString(R.string.history) -> {
                        //履歴
                        callHistoryAPI()
                    }
                }
            }
        })

        fragment_nicovideo_select_swipe_to_reflesh.setOnRefreshListener {
            val pos = fragment_nicovideo_select_tab_layout.selectedTabPosition
            val text = fragment_nicovideo_select_tab_layout.getTabAt(pos)?.text
                ?: getString(R.string.follow_program)
            when (text) {
                getString(R.string.post_video) -> {
                    //投稿動画
                    getPOSTVideoHTML()
                }
                getString(R.string.history) -> {
                    //履歴
                    callHistoryAPI()
                }
            }
        }

    }

    private fun initRecyclerView() {
        fragment_nicovideo_select_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        fragment_nicovideo_select_recyclerview.layoutManager =
            mLayoutManager as RecyclerView.LayoutManager?
        nicoVideoSelectAdapter = NicoVideoSelectAdapter(recyclerViewList)
        fragment_nicovideo_select_recyclerview.adapter = nicoVideoSelectAdapter
    }

    private fun setVideoID() {
        fragment_nicovideo_select_video_id_button.setOnClickListener {
            //Activity移動
            val id = fragment_nicovideo_select_video_id_textinput.text.toString()
            val intent = Intent(context, NicoVideoActivity::class.java)
            intent.putExtra("id", id)
            startActivity(intent)
        }
    }

    //履歴取得。スマホWeb版見てたらそれっぽいAPI特定した
    fun callHistoryAPI() {
        recyclerViewList.clear()
        fragment_nicovideo_select_swipe_to_reflesh.isRefreshing = true
        //200件最大まで取得する
        val url = "https://nvapi.nicovideo.jp/v1/users/me/watch/history?pageSize=200"
        val request = Request.Builder()
            .url(url)
            .header("Cookie", "user_session=${usersession}")
            .header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
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
                    //成功
                    val string = response.body?.string()
                    val json = JSONObject(string)
                    val jsonArray = json.getJSONObject("data").getJSONArray("items")
                    for (i in 0 until jsonArray.length()) {
                        val video = jsonArray.getJSONObject(i)
                        val videoId = video.getString("watchId")
                        val title = video.getJSONObject("video").getString("title")
                        val lastWatch = video.getString("lastViewedAt")
                        val registeredAt = video.getJSONObject("video").getString("registeredAt")
                        val watchCount = video.getString("views")
                        val thumbnailUrl = video.getJSONObject("video").getJSONObject("thumbnail")
                            .getString("largeUrl")

                        val commentCount =
                            video.getJSONObject("video").getJSONObject("count").getString("comment")
                        val playCount =
                            video.getJSONObject("video").getJSONObject("count").getString("view")
                        val mylistCount =
                            video.getJSONObject("video").getJSONObject("count").getString("mylist")

                        val item = arrayListOf<String>().apply {
                            add("history")//りれきだよー
                            add(videoId)
                            add(title)
                            add(lastWatch)
                            add(registeredAt)
                            add(watchCount)
                            add(thumbnailUrl)
                            add(commentCount)
                            add(playCount)
                            add(mylistCount)
                        }

                        recyclerViewList.add(item)

                    }

                    activity?.runOnUiThread {
                        nicoVideoSelectAdapter.notifyDataSetChanged()
                        fragment_nicovideo_select_swipe_to_reflesh.isRefreshing = false
                    }

                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    /*
    * 投稿動画のAPIは見つからなかった。HTMLスクレイピングする
    * スクレイピング、HTMLが変わったら動かなくなるのでやりたくね～
    * */
    fun getPOSTVideoHTML(page: String = "") {
        //200件最大まで取得する
        var url = "https://www.nicovideo.jp/my/video"
        if (page.isNotEmpty()) {
            url = "https://www.nicovideo.jp$page"
        } else {
            //リストクリア
            recyclerViewList.clear()
            fragment_nicovideo_select_swipe_to_reflesh.isRefreshing = true
        }
        val request = Request.Builder()
            .url(url)
            .header("Cookie", "user_session=${usersession}")
            .header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
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
                    val document = Jsoup.parse(response.body?.string())
                    //動画のDiv要素を取り出す
                    val divList = document.getElementsByClass("outer VideoItem")
                    divList.forEach {
                        //一つずつ見ていく
                        var videoId = it.getElementsByClass("ct").first().attr("href")
                        videoId = videoId.replace("http://commons.nicovideo.jp/tree/", "")
                        val title = it.getElementsByTag("h5").first().getElementsByTag("a").text()
                        val postDate = it.getElementsByClass("posttime").first().text()
                        val thumbnailUrl = it.getElementsByTag("img").first().attr("src")
                        val commentCount = it.getElementsByClass("comment").first().text()
                        val playCount = it.getElementsByClass("play").first().text()
                        val mylistCount = it.getElementsByClass("mylist").first().text()

                        val item = arrayListOf<String>().apply {
                            add("post")//投稿だよー
                            add(videoId)
                            add(title)
                            add("")
                            add(postDate)
                            add("")
                            add(thumbnailUrl)
                            add(commentCount)
                            add(playCount)
                            add(mylistCount)
                        }

                        recyclerViewList.add(item)

                    }
                    //次のページへ移動
                    val videoListButtonDiv =
                        document.getElementsByClass("outer VideoListHeadMenuContainer")[0].getElementsByClass(
                            "pager"
                        )[0]
                    val nextButton = videoListButtonDiv.children().last()

                    //href取得。次のページへ
                    //最後かどうか判断しないと無限に取得しに行くので。最後は要素が「span」になる。最後の要素が「span」なら取得しない。
                    if (nextButton.tagName() != "span") {
                        val link = nextButton.attr("href")
                        getPOSTVideoHTML(link)
                    }

                    activity?.runOnUiThread {
                        nicoVideoSelectAdapter.notifyDataSetChanged()
                        fragment_nicovideo_select_swipe_to_reflesh.isRefreshing = false
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