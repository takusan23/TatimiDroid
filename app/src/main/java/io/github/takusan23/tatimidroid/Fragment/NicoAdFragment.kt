package io.github.takusan23.tatimidroid.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.GiftRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoLiveAPI.NicoAdAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_gift_layout.*
import kotlinx.android.synthetic.main.fragment_nicoad_layout.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class NicoAdFragment : Fragment() {
    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var giftRecyclerViewAdapter: GiftRecyclerViewAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager
    var liveId = ""
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_nicoad_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //番組ID取得
        liveId = arguments?.getString("liveId") ?: ""

        recyclerViewList = ArrayList()
        nicoad_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        nicoad_recyclerview.layoutManager = mLayoutManager as RecyclerView.LayoutManager?
        giftRecyclerViewAdapter = GiftRecyclerViewAdapter(recyclerViewList)
        nicoad_recyclerview.adapter = giftRecyclerViewAdapter
        recyclerViewLayoutManager = nicoad_recyclerview.layoutManager!!

        //貢献度ランキング
        getNicoAdRanking()

        // 情報取得
        getNicoAd()

        val darkModeSupport = DarkModeSupport(context!!)
        nicoad_tablayout.setBackgroundColor(darkModeSupport.getThemeColor())

        //TabLayout
        nicoad_tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    getString(R.string.nico_ad_history) -> {
                        getNicoAdHistory()
                    }
                    getString(R.string.nico_ad_ranking) -> {
                        getNicoAdRanking()
                    }
                }
            }

        })
    }

    private fun getNicoAd() {
        val nicoAdAPI = NicoAdAPI()
        nicoAdAPI.getNicoAdInfo(liveId, null) {
            if (it.isSuccessful) {
                val jsonObject = JSONObject(it.body?.string())
                val data = jsonObject.getJSONObject("data")
                val totalPoint = data.getInt("totalPoint")
                val activePoint = data.getInt("activePoint")
                activity?.runOnUiThread {
                    gift_total_point.text = "${getString(R.string.nicoad_total)}：${totalPoint}pt"
                    gift_active_point.text = "${getString(R.string.nicoad_active)}：${activePoint}pt"
                }
            }
        }
    }

    //貢献度ランキング
    fun getNicoAdRanking() {
        recyclerViewList.clear()
        val request = Request.Builder()
            .url("https://api.nicoad.nicovideo.jp/v1/contents/live/${liveId}/ranking/contribution?limit=100")
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
                    val jsonObject = JSONObject(response_string)
                    val rankingArray = jsonObject.getJSONObject("data").getJSONArray("ranking")
                    for (i in 0..(rankingArray.length() - 1)) {
                        val jsonObject = rankingArray.getJSONObject(i)
                        val advertiserName = jsonObject.getString("advertiserName")
                        val totalContribution = jsonObject.getString("totalContribution")
                        val rank = jsonObject.getString("rank")
                        //RecyclerView追加
                        val item = arrayListOf<String>()
                        item.add("")
                        item.add(rank)
                        item.add("${advertiserName} : ${totalContribution}")
                        recyclerViewList.add(item)
                    }
                    //更新
                    activity?.runOnUiThread {
                        giftRecyclerViewAdapter.notifyDataSetChanged()
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    //広告履歴
    fun getNicoAdHistory() {
        recyclerViewList.clear()
        val request = Request.Builder()
            .url("https://api.nicoad.nicovideo.jp/v2/contents/live/${liveId}/histories?limit=15")
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
                    val jsonObject = JSONObject(response_string)
                    val rankingArray = jsonObject.getJSONObject("data").getJSONArray("histories")
                    for (i in 0..(rankingArray.length() - 1)) {
                        val jsonObject = rankingArray.getJSONObject(i)
                        val advertiserName = jsonObject.getString("advertiserName")
                        val point = jsonObject.getString("adPoint")
                        //RecyclerView追加
                        val item = arrayListOf<String>()
                        item.add("")
                        item.add(advertiserName)
                        item.add(point)
                        recyclerViewList.add(item)
                    }
                    //更新
                    activity?.runOnUiThread {
                        giftRecyclerViewAdapter.notifyDataSetChanged()
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