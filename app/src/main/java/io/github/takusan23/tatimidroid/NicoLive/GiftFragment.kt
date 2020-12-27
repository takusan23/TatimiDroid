package io.github.takusan23.tatimidroid.NicoLive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.NicoLive.Adapter.GiftRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.databinding.FragmentNicoliveGiftBinding
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class GiftFragment : Fragment() {

    /** ギフト配列 */
    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()

    /** ギフト一覧Adapter */
    val giftRecyclerViewAdapter = GiftRecyclerViewAdapter(recyclerViewList)

    val viewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })
    val liveId by lazy { viewModel.nicoLiveHTML.liveId }

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicoliveGiftBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewBinding.fragmentNicoliveGiftRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = giftRecyclerViewAdapter
        }

        //ギフト履歴
        getGiftRanking()

        //TabLayout
        viewBinding.fragmentNicoliveGiftTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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