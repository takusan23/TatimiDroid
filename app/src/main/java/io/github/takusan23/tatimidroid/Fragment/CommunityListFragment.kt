package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.database.sqlite.SQLiteDatabase
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import kotlinx.android.synthetic.main.fragment_commnunity_list_layout.*
import kotlinx.android.synthetic.main.fragment_community_list_layout.*
import kotlinx.android.synthetic.main.fragment_community_list_layout.community_recyclerview
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumesAll
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class CommunityListFragment : Fragment() {
    var user_session = ""
    lateinit var pref_setting: SharedPreferences
    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var communityRecyclerViewAdapter: CommunityRecyclerViewAdapter
    lateinit var autoAdmissionAdapter: AutoAdmissionAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager

    lateinit var autoAdmissionSQLiteSQLite: AutoAdmissionSQLiteSQLite
    lateinit var sqLiteDatabase: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_commnunity_list_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        val darkModeSupport = DarkModeSupport(context!!)
        program_tablayout.backgroundTintList =
            ColorStateList.valueOf(darkModeSupport.getThemeColor())

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.follow_program)

        recyclerViewList = ArrayList()
        community_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        community_recyclerview.layoutManager = mLayoutManager as RecyclerView.LayoutManager?
        communityRecyclerViewAdapter = CommunityRecyclerViewAdapter(recyclerViewList)
        autoAdmissionAdapter = AutoAdmissionAdapter(recyclerViewList)
        community_recyclerview.adapter = communityRecyclerViewAdapter
        recyclerViewLayoutManager = community_recyclerview.layoutManager!!

        user_session = pref_setting.getString("user_session", "") ?: ""

        (activity as AppCompatActivity).supportActionBar?.title =
            getString(R.string.follow_program)

        //参加中のコミュニティ読み込み
        getFavouriteCommunity()

        community_swipe.isRefreshing = true

        program_tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                //クリア
                recyclerViewList.clear()
                community_recyclerview.adapter?.notifyDataSetChanged()
                setNicoLoad(tab?.text.toString() ?: getString(R.string.follow_program))
            }
        })

        community_swipe.setOnRefreshListener {
            val pos = program_tablayout.selectedTabPosition
            val text = program_tablayout.getTabAt(pos)?.text ?: getString(R.string.follow_program)
            setNicoLoad(text.toString())
        }


    }

    fun setNicoLoad(text: String) {
        //くるくる
        community_swipe.isRefreshing = true
        when (text) {
            getString(R.string.follow_program) -> {
                getFavouriteCommunity()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.follow_program)
            }
            getString(R.string.nicorepo) -> {
                getNicorepo()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.nicorepo)
            }
            getString(R.string.auto_admission) -> {
                getAutoAdmissionList()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.auto_admission)
                //Service再起動
                val intent = Intent(context, AutoAdmissionService::class.java)
                context?.stopService(intent)
                context?.startService(intent)
            }
            getString(R.string.osusume) -> {
                getRecommend()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.osusume)
            }
            getString(R.string.ranking) -> {
                getRanking()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.ranking)
            }
        }
    }

    fun setTitle(title: String) {
        activity?.runOnUiThread {
            (activity as AppCompatActivity).supportActionBar?.title = title
        }
    }

    //ランキング取得
    fun getRanking() {
        recyclerViewList.clear()
        val request = Request.Builder().apply {
            url("https://sp.live.nicovideo.jp/ranking")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$user_session")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                recyclerViewList.clear()
                if (response.isSuccessful) {
                    val document = Jsoup.parse(response.body?.string())
                    //JSONっぽいのがあるので取り出す
                    val json = document.getElementsByTag("script").get(5)
                    var json_string = URLDecoder.decode(json.html(), "utf-8")
                    // いらない部分消す
                    json_string = json_string.replace("window.__initial_state__ = \"", "")
                    json_string =
                        json_string.replace(
                            "window.__public_path__ = \"https://nicolive.cdn.nimg.jp/relive/sp/\";",
                            ""
                        )
                    json_string =
                        json_string.replace("\";", "")
                    val jsonObject = JSONObject(json_string)
                    //JSON解析
                    val programs =
                        jsonObject.getJSONObject("pageContents").getJSONObject("ranking")
                            .getJSONObject("rankingPrograms")
                            .getJSONArray("rankingPrograms")

                    //for
                    for (i in 0 until programs.length()) {
                        val jsonObject = programs.getJSONObject(i)
                        val programId = jsonObject.getString("id")
                        val title = jsonObject.getString("title")
                        val beginAt = jsonObject.getString("beginAt")
                        val communityName = jsonObject.getString("socialGroupName")
                        val liveNow = jsonObject.getString("liveCycle") //放送中か？
                        val rank = jsonObject.getString("rank")
                        //RecyclerView追加
                        val item = arrayListOf<String>()
                        item.add("")
                        item.add(title)
                        item.add(communityName)
                        item.add(title)
                        item.add(beginAt)
                        item.add(beginAt)
                        item.add(programId)
                        item.add(beginAt)
                        item.add(liveNow)
                        recyclerViewList.add(item)
                    }
                    //リスト更新
                    activity?.runOnUiThread {
                        communityRecyclerViewAdapter.notifyDataSetChanged()
                        community_recyclerview.adapter = communityRecyclerViewAdapter
                        community_swipe.isRefreshing = false
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    //おすすめの番組
    fun getRecommend() {
        recyclerViewList.clear()
        val request = Request.Builder().apply {
            url("https://live.nicovideo.jp/?header")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$user_session")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                recyclerViewList.clear()
                if (response.isSuccessful) {
                    val document = Jsoup.parse(response.body?.string())
                    //JSONっぽいのがあるので取り出す
                    val json = document.getElementById("embedded-data")
                        .getElementsByAttribute("data-props")
                    val json_string = json.attr("data-props")
                    val jsonObject = JSONObject(json_string)
                    //JSON解析
                    val programs =
                        jsonObject.getJSONObject("view")
                            .getJSONObject("recommendedProgramListState")
                            .getJSONArray("programList")
                    //for
                    for (i in 0 until programs.length()) {
                        val jsonObject = programs.getJSONObject(i)
                        val programId = jsonObject.getString("id")
                        val title = jsonObject.getString("title")
                        val beginAt = jsonObject.getString("beginAt")
                        val communityName =
                            jsonObject.getJSONObject("socialGroup").getString("name")
                        val liveNow = jsonObject.getString("liveCycle") //放送中か？
                        //RecyclerView追加
                        val item = arrayListOf<String>()
                        item.add("")
                        item.add(title)
                        item.add(communityName)
                        item.add(title)
                        item.add(beginAt)
                        item.add(beginAt)
                        item.add(programId)
                        item.add(beginAt)
                        item.add("Begun")
                        recyclerViewList.add(item)
                    }
                    //リスト更新
                    activity?.runOnUiThread {
                        communityRecyclerViewAdapter.notifyDataSetChanged()
                        community_recyclerview.adapter = communityRecyclerViewAdapter
                        community_swipe.isRefreshing = false
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }


    // 参加中コミュニティから放送中、予約枠を取得する。
// 今まではスマホサイトにアクセスしてJSON取ってたけど動かなくなった。
// のでPC版にアクセスしてJSONを取得する（PC版にもJSON存在した。）
    fun getFavouriteCommunity() {
        recyclerViewList.clear()
        val request = Request.Builder().apply {
            url("https://live.nicovideo.jp/?header")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$user_session")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                recyclerViewList.clear()
                if (response.isSuccessful) {
                    // ログイン出来てないとき（ユーザーセッション切れた）
                    val niconicoId = response.headers["x-niconico-id"]
                    if (niconicoId != null) {
                        // ログイン済み
                        // JSONのあるscriptタグ
                        val document = Jsoup.parse(response.body?.string())
                        val script = document.getElementById("embedded-data")
                        val jsonString = script.attr("data-props")
                        val jsonObject = JSONObject(jsonString)
                        //JSON解析
                        val programs =
                            jsonObject.getJSONObject("view")
                                .getJSONObject("favoriteProgramListState")
                                .getJSONArray("programList")
                        //for
                        for (i in 0 until programs.length()) {
                            val jsonObject = programs.getJSONObject(i)
                            val programId = jsonObject.getString("id")
                            val title = jsonObject.getString("title")
                            val beginAt = jsonObject.getString("beginAt")
                            val communityName =
                                jsonObject.getJSONObject("socialGroup").getString("name")
                            val liveNow = jsonObject.getString("liveCycle") //放送中か？
                            //RecyclerView追加
                            val item = arrayListOf<String>()
                            item.add("")
                            item.add(title)
                            item.add(communityName)
                            item.add(title)
                            item.add(beginAt)
                            item.add(beginAt)
                            item.add(programId)
                            item.add(beginAt)
                            item.add(liveNow)
                            recyclerViewList.add(item)
                        }
                        //リスト更新
                        activity?.runOnUiThread {
                            communityRecyclerViewAdapter.notifyDataSetChanged()
                            community_recyclerview.adapter = communityRecyclerViewAdapter
                            community_swipe.isRefreshing = false
                        }
                    } else {
                        // ログイン切れた
                        NicoLogin.login(context) {
                            // 成功時（失敗時はToastに表示する）
                            activity?.runOnUiThread {
                                Snackbar.make(
                                    community_recyclerview,
                                    R.string.re_login_successful,
                                    Snackbar.LENGTH_SHORT
                                ).setAction(R.string.reload) {
                                    getFavouriteCommunity()
                                }.show()
                            }
                        }
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    //ニコレポ取得
    fun getNicorepo() {
        recyclerViewList.clear()
        val request = Request.Builder()
            .url("https://www.nicovideo.jp/api/nicorepo/timeline/my/all?client_app=pc_myrepo")
            .header("Cookie", "user_session=${user_session}")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonObject = JSONObject(response.body?.string())
                    val data = jsonObject.getJSONArray("data")
                    for (i in 0 until data.length()) {
                        val nicorepoObject = data.getJSONObject(i)
                        //番組開始だけ取得
                        if (nicorepoObject.has("program")) {
                            if (nicorepoObject.has("community")) {
                                val program = nicorepoObject.getJSONObject("program")
                                val community = nicorepoObject.getJSONObject("community")
                                val title = program.getString("title")
                                val name = community.getString("name")
                                val live = parseTime(program.getString("beginAt"))
                                val time = getString(R.string.nicorepo)
                                val timeshift = parseTime(program.getString("beginAt"))
                                val liveId = program.getString("id")

                                //変換
                                val simpleDateFormat =
                                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SS.sssX")
                                val date_calender =
                                    simpleDateFormat.parse(program.getString("beginAt"))
                                val calender = Calendar.getInstance(TimeZone.getDefault())
                                calender.time = date_calender

                                //RecyclerView追加
                                val item = arrayListOf<String>()
                                item.add("")
                                item.add(title)
                                item.add(name)
                                item.add(calender.time.time.toString())
                                item.add(calender.time.time.toString())
                                item.add(timeshift)
                                item.add(liveId)
                                item.add("")
                                item.add("Begun")
                                recyclerViewList.add(item)
                            }
                        }
                    }
                    //リスト更新
                    activity?.runOnUiThread {
                        communityRecyclerViewAdapter.notifyDataSetChanged()
                        community_recyclerview.adapter = communityRecyclerViewAdapter
                        community_swipe.isRefreshing = false
                    }
                } else {
                    showToast(getString(R.string.error) + "\n" + response.code)
                }
            }
        })
    }

    fun parseTime(startTime: String): String {
        //SimpleDataFormat
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SS.sssX")
        val date_calender = simpleDateFormat.parse(startTime)
        val calender = Calendar.getInstance(TimeZone.getDefault())
        calender.time = date_calender

        val month = calender.get(Calendar.MONTH)
        val date = calender.get(Calendar.DATE)
        val hour = calender.get(Calendar.HOUR_OF_DAY)
        val minute = calender.get(Calendar.MINUTE)

        val time = "${month + 1}/${date} ${hour}:${minute}"
        return time
    }

    //予約枠自動入場機能
    fun getAutoAdmissionList() {
        recyclerViewList.clear()

        //初期化したか
        if (!this@CommunityListFragment::autoAdmissionSQLiteSQLite.isInitialized) {
            autoAdmissionSQLiteSQLite =
                AutoAdmissionSQLiteSQLite(context!!)
            sqLiteDatabase = autoAdmissionSQLiteSQLite.writableDatabase
            autoAdmissionSQLiteSQLite.setWriteAheadLoggingEnabled(false)
        }

        //SQLite読み出し
        val cursor = sqLiteDatabase.query(
            "auto_admission",
            arrayOf("name", "liveid", "start", "app"),
            null, null, null, null, null
        )
        cursor.moveToFirst()
        for (i in 0 until cursor.count) {

            val programName = cursor.getString(0)
            val liveId = cursor.getString(1)
            val start = cursor.getString(2)
            val app = cursor.getString(3)

            //未来の番組だけ読み込む（終わってるのは読み込まない）
            if (Calendar.getInstance().timeInMillis < start.toLong()) {
                //RecyclerView追加
                val item = arrayListOf<String>()
                item.add("")
                item.add(programName)
                item.add(liveId)
                item.add(start)
                item.add(app)
                recyclerViewList.add(item)
            }
            cursor.moveToNext()
        }
        cursor.close()

        //リスト更新
        activity?.runOnUiThread {
            autoAdmissionAdapter.notifyDataSetChanged()
            community_recyclerview.adapter = autoAdmissionAdapter
            community_swipe.isRefreshing = false
        }
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


}