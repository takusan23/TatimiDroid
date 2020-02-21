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
import io.github.takusan23.tatimidroid.CommunityRecyclerViewAdapter
import io.github.takusan23.tatimidroid.GiftRecyclerViewAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_commnunity_list_layout.*
import kotlinx.android.synthetic.main.fragment_gift_layout.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class GiftFragment : Fragment() {
    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var giftRecyclerViewAdapter: GiftRecyclerViewAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager
    var liveId = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_gift_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //番組ID取得
        liveId = arguments?.getString("liveId") ?: ""

        recyclerViewList = ArrayList()
        gift_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        gift_recyclerview.layoutManager = mLayoutManager as RecyclerView.LayoutManager?
        giftRecyclerViewAdapter = GiftRecyclerViewAdapter(recyclerViewList)
        gift_recyclerview.adapter = giftRecyclerViewAdapter
        recyclerViewLayoutManager = gift_recyclerview.layoutManager!!

        //ギフト履歴
        getGiftRanking()

        //TabLayout
        gift_tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    getString(R.string.gift_history) -> {
                        //ギフト履歴
                        getGiftHistory()
                    }
                    getString(R.string.gift_ranking) -> {
                        //ギフトランキング
                        getGiftRanking()
                    }
                }
            }
        })

    }

    //ギフトランキング取得
    fun getGiftRanking() {
        recyclerViewList.clear()
        val request = Request.Builder()
            .url("https://api.nicoad.nicovideo.jp/v1/contents/nage_agv/${liveId}/ranking/contribution?limit=100")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    //他の端末でJSONエラー出るので例外処理。私の環境だと再現できねえ？
                    try {
                        val response_string = response.body?.string()
                        // println(response_string)
                        val jsonObject = JSONObject(response_string)
                        val rankingArray = jsonObject.getJSONObject("data").getJSONArray("ranking")
                        for (i in 0 until rankingArray.length()) {
                            val jsonObject = rankingArray.getJSONObject(i)
                            // val userId = jsonObject.getString("userId")
                            val advertiserName = jsonObject.getString("advertiserName")
                            val totalContribution = jsonObject.getString("totalContribution")
                            val rank = jsonObject.getString("rank")
                            //RecyclerView追加
                            val item = arrayListOf<String>()
                            item.add("")
                            item.add("$rank : $advertiserName")
                            item.add(totalContribution)
                            recyclerViewList.add(item)
                        }
                        //更新
                        activity?.runOnUiThread {
                            giftRecyclerViewAdapter.notifyDataSetChanged()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    //ギフト履歴取得
    fun getGiftHistory() {
        recyclerViewList.clear()
        val request = Request.Builder()
            .url("https://api.nicoad.nicovideo.jp/v2/contents/nage_agv/${liveId}/histories?limit=100")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                val response_string = response.body?.string()
                if (response.isSuccessful) {
                    val jsonObject = JSONObject(response_string)
                    val rankingArray = jsonObject.getJSONObject("data").getJSONArray("histories")
                    for (i in 0..(rankingArray.length() - 1)) {
                        val jsonObject = rankingArray.getJSONObject(i)
                        val advertiserName = jsonObject.getString("advertiserName")
                        val itemName = jsonObject.getJSONObject("item").getString("name")
                        //RecyclerView追加
                        val item = arrayListOf<String>()
                        item.add("")
                        item.add(advertiserName)
                        item.add(itemName)
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