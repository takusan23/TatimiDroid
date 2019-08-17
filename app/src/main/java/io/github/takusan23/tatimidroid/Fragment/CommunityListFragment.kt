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
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import kotlinx.android.synthetic.main.fragment_commnunity_list_layout.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_commnunity_list_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        val darkModeSupport = DarkModeSupport(context!!)
        program_tablayout.backgroundTintList = ColorStateList.valueOf(darkModeSupport.getThemeColor())

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

        getFavouriteCommunity()
        program_tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text ?: "") {
                    getString(R.string.follow_program) -> {
                        GlobalScope.launch {
                            getFavouriteCommunity()
                        }
                        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.follow_program)
                    }
                    getString(R.string.nicorepo) -> {
                        GlobalScope.launch {
                            getNicorepo()
                        }
                        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.nicorepo)
                    }
                    getString(R.string.auto_admission) -> {
                        getAutoAdmissionList()
                        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.auto_admission)
                        //Service再起動
                        val intent = Intent(context, AutoAdmissionService::class.java)
                        context?.stopService(intent)
                        context?.startService(intent)
                    }
                }
            }
        })

    }


    //参加中コミュニティから放送中、予約枠を取得する。
    //APIが見つからなかったのでスマホ版Webページからスクレイピングすることにした。
    fun getFavouriteCommunity() {
        recyclerViewList.clear()
        val request = Request.Builder()
            .url("https://sp.live.nicovideo.jp/favorites")
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
                    val response_string = response.body?.string()
                    val document = Jsoup.parse(response_string)
                    val list =
                        document.getElementById("app").select("div.___page___lyKFH")
                            .select("div.___favorites___2coLw.___favorites-skin___2LMgt")
                            .select("div.___program-list-section___pxzaS.___program-list-section-base___2CsOR")
                            .select("ul.___program-card-list___38cBA.___program-card-list___3iNW_.___list___XtHoI.___program-card-list-skin___1Ihyl")
                    val li = list.select("li.___item___2Ygdh.___item___2UWvK.___item-skin___3B9Gi")

                    for (i in 0..(li.size - 1)) {
                        val title =
                            li.get(i).select("div").select("a").select("div.___program-detail___3Uk6B").select("h2")
                                .text()
                        val name =
                            li.get(i).select("div").select("a").select("div.___program-detail___3Uk6B").select("h3")
                                .select("span").text()
                        val live =
                            li.get(i).select("div").select("a").select("div.___state___2LXNP.___state-skin___nxppX")
                                .select("span.___status____1aVL.___status-skin___3saHk.___label-local___C6kvB").text()
                        val time =
                            li.get(i).select("div").select("a").select("div.___state___2LXNP.___state-skin___nxppX")
                                .select("span.___duration___12X8D.___duration-skin___2Ew_Z.___label-local___C6kvB")
                                .text()
                        val timeshift = li.get(i).select("div").select("a")
                            .select("div.___program-preview___3IGR8.___program-preview-base___2dy_9.___program-preview-skin___38pdd")
                            .select("div.___state___2LXNP.___state-skin___nxppX")
                            .select("time.___start-at___3HOBx.___start-at-skin___1a4B3.___label-local___C6kvB").text()
                        val liveId = li.get(i).select("div").select("a").attr("href").replace("/watch/", "")
                        val datetime = li.get(i).select("div").select("a")
                            .select("div.___program-preview___3IGR8.___program-preview-base___2dy_9.___program-preview-skin___38pdd")
                            .select("div.___state___2LXNP.___state-skin___nxppX")
                            .select("time.___start-at___3HOBx.___start-at-skin___1a4B3.___label-local___C6kvB")
                            .attr("datetime")
                        //RecyclerView追加
                        val item = arrayListOf<String>()
                        item.add("")
                        item.add(title)
                        item.add(name)
                        item.add(live)
                        item.add(time)
                        item.add(timeshift)
                        item.add(liveId)
                        item.add(datetime)
                        recyclerViewList.add(item)
                    }
                    //リスト更新
                    activity?.runOnUiThread {
                        communityRecyclerViewAdapter.notifyDataSetChanged()
                        community_recyclerview.adapter = communityRecyclerViewAdapter
                    }
                } else {
                    showToast(getString(R.string.error) + "\n" + response.code)
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
                            val program = nicorepoObject.getJSONObject("program")
                            val community = nicorepoObject.getJSONObject("community")
                            val title = program.getString("title")
                            val name = community.getString("name")
                            val live = parseTime(program.getString("beginAt"))
                            val time = getString(R.string.nicorepo)
                            val timeshift = parseTime(program.getString("beginAt"))
                            val liveId = program.getString("id")
                            //RecyclerView追加
                            val item = arrayListOf<String>()
                            item.add("")
                            item.add(title)
                            item.add(name)
                            item.add(live)
                            item.add(time)
                            item.add(timeshift)
                            item.add(liveId)
                            item.add("")
                            recyclerViewList.add(item)
                        }
                    }
                    //リスト更新
                    activity?.runOnUiThread {
                        communityRecyclerViewAdapter.notifyDataSetChanged()
                        community_recyclerview.adapter = communityRecyclerViewAdapter
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
        }
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


}