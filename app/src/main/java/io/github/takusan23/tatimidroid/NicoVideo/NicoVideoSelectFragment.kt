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
import kotlinx.android.synthetic.main.fragment_commnunity_list_layout.view.*
import kotlinx.android.synthetic.main.fragment_nicovideo_select.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

class NicoVideoSelectFragment : Fragment() {

    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var nicoVideoSelectAdapter: NicoVideoSelectAdapter

    lateinit var pref_setting: SharedPreferences

    var usersession = ""
    var nicoAPIToken = ""

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
            fragment_nicovideo_select_tab_layout.setBackgroundColor(darkModeSupport.getThemeColor())
            fragment_nicovideo_select_mylist_tab_layout.setBackgroundColor(darkModeSupport.getThemeColor())
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
                    getString(R.string.mylist) -> {
                        //マイリスト
                        GlobalScope.launch {
                            val token = getNicoAPIToken().await()
                            nicoAPIToken = token
                            //とりあえずマイリストを最初に読み込んでおく。
                            postMylist(token, "", true)
                            postMylistList(token)
                        }
                    }
                }
            }
        })

        //マイリストのTabLayoutのコールバック
        fragment_nicovideo_select_mylist_tab_layout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    //とりあえずマイリスト
                    getString(R.string.toriaezu_mylist) -> {
                        postMylist(nicoAPIToken, "", true)
                    }
                    //それ以外（普通のマイリストなど
                    else -> {
                        val id = tab?.tag as String
                        postMylist(nicoAPIToken, id)
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
                getString(R.string.mylist) -> {
                    val tab = fragment_nicovideo_select_mylist_tab_layout.getTabAt(
                        fragment_nicovideo_select_mylist_tab_layout.selectedTabPosition
                    )
                    when (tab?.text) {
                        //とりあえずマイリスト
                        getString(R.string.toriaezu_mylist) -> {
                            postMylist(nicoAPIToken, "", true)
                        }
                        //それ以外（普通のマイリストなど
                        else -> {
                            val id = tab?.tag as String
                            postMylist(nicoAPIToken, id)
                        }
                    }
                }
            }
        }

    }

    //マイリスト一覧を取得
    private fun postMylistList(token: String) {

        //表示する
        activity?.runOnUiThread {
            fragment_nicovideo_select_mylist_tab_layout.visibility = View.VISIBLE

            //タブ初期化
            fragment_nicovideo_select_mylist_tab_layout.removeAllTabs()
            //IDをtagに詰めとく。
            val tabItem = fragment_nicovideo_select_mylist_tab_layout.newTab()
            tabItem.apply {
                text = getString(R.string.toriaezu_mylist)
            }
            fragment_nicovideo_select_mylist_tab_layout.addTab(tabItem)

        }

        val post = FormBody.Builder()
            .add("token", token)
            .build()
        val url = "https://www.nicovideo.jp/api/mylistgroup/list"
        val request = Request.Builder()
            .header("Cookie", "user_session=${usersession}")
            .header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
            .header("User-Agent", "TatimiDroid;@takusan_23")
            .url(url)
            .post(post)
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("${getString(R.string.error)}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonObject = JSONObject(response.body?.string())
                    activity?.runOnUiThread {
                        val mylistItems = jsonObject.getJSONArray("mylistgroup")
                        for (i in 0 until mylistItems.length()) {
                            val mylist = mylistItems.getJSONObject(i)
                            val name = mylist.getString("name")
                            val id = mylist.getString("id")

                            //IDをtagに詰めとく。
                            val tabItem = fragment_nicovideo_select_mylist_tab_layout.newTab()
                            tabItem.apply {
                                text = name
                                tag = id
                            }
                            fragment_nicovideo_select_mylist_tab_layout.addTab(tabItem)

                        }
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    /**
     * マイリストを取得する（POSTすれば返ってくる。
     * @param toriaezumylist とりあえずマイリストを取得するときはtrueで
     * @param mylistId https://www.nicovideo.jp/my/mylist/#/ここの数字　とりあえずマイリスト以外では指定しないといけません。
     * */
    private fun postMylist(token: String, mylistId: String = "", toriaezumylist: Boolean = false) {

        recyclerViewList.clear()
        activity?.runOnUiThread { fragment_nicovideo_select_swipe_to_reflesh.isRefreshing = true }

        val post = FormBody.Builder().apply {
            add("token", token)
            //とりあえずマイリスト以外ではIDを入れる、
            if (!toriaezumylist) {
                add("group_id", mylistId)
            }
        }.build()

        //とりあえずマイリストと普通のマイリスト。
        val url = if (toriaezumylist) {
            "https://www.nicovideo.jp/api/deflist/list"
        } else {
            "https://www.nicovideo.jp/api/mylist/list"
        }

        val request = Request.Builder()
            .header("Cookie", "user_session=${usersession}")
            .header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
            .header("User-Agent", "TatimiDroid;@takusan_23")
            .url(url)
            .post(post)
            .build()

        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("${getString(R.string.error)}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonObject = JSONObject(response.body?.string())
                    val mylistItems = jsonObject.getJSONArray("mylistitem")
                    val tmpList = arrayListOf<ArrayList<String>>()
                    for (i in 0 until mylistItems.length()) {

                        val videoObject = mylistItems.getJSONObject(i)
                        val videoInfo = videoObject.getJSONObject("item_data")

                        val videoId = videoInfo.getString("video_id")
                        val title = videoInfo.getString("title")
                        val lastWatch = ""
                        val registeredAt = videoInfo.getLong("first_retrieve").toString()
                        val watchCount = videoInfo.getString("view_counter")
                        val thumbnailUrl = videoInfo.getString("thumbnail_url")
                        val commentCount = videoInfo.getString("num_res")
                        val playCount = videoInfo.getString("view_counter")
                        val mylistCount = videoInfo.getString("mylist_counter")

                        val createTime = videoObject.getLong("create_time").toString()

                        val item = arrayListOf<String>().apply {
                            add("mylist")//マイリストだよ
                            add(videoId)
                            add(title)
                            add(createTime)
                            add(registeredAt)
                            add(watchCount)
                            add(thumbnailUrl)
                            add(commentCount)
                            add(playCount)
                            add(mylistCount)
                        }

                        tmpList.add(item)
                    }

                    //登録順に並び替える？
                    tmpList.toList()
                        .sortedByDescending { arrayList -> arrayList[3].toString().toLong() }
                        .forEach {
                            recyclerViewList.add(it)
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
        fragment_nicovideo_select_mylist_tab_layout.visibility = View.GONE

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

        recyclerViewList.clear()
        fragment_nicovideo_select_swipe_to_reflesh.isRefreshing = true
        fragment_nicovideo_select_mylist_tab_layout.visibility = View.GONE

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

    /*
    * マイリストのHTMLからNicoAPI.Tokenをとってみよう
    * */
    fun getNicoAPIToken(): Deferred<String> = GlobalScope.async {
        //200件最大まで取得する
        val url = "https://www.nicovideo.jp/my/mylist"
        val request = Request.Builder()
            .url(url)
            .header("Cookie", "user_session=${usersession}")
            .header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
            .header("User-Agent", "TatimiDroid;@takusan_23")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        //同期実行
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {

            //正規表現で取り出す。
            val html = Jsoup.parse(response.body?.string())
            val regex = "NicoAPI.token = \"(.+?)\";"
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(html.html())
            if (matcher.find()) {
                val token = matcher.group(1)
                //println("トークン　$token")
                return@async token
            }

        } else {
            showToast("${getString(R.string.error)}\n${response.code}")
        }
        return@async ""
    }
}