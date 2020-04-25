package io.github.takusan23.tatimidroid.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Adapter.TagRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveTagAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.bottom_fragment_tags.*
import kotlinx.android.synthetic.main.fragment_gift_layout.*
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class NicoLiveTagBottomFragment : BottomSheetDialogFragment() {

    lateinit var programFragment: ProgramInfoFragment

    // RecyclerView
    var recyclerViewList = arrayListOf<NicoLiveTagAPI.NicoLiveTagItemData>()
    lateinit var tagRecyclerViewAdapter: TagRecyclerViewAdapter

    lateinit var pref_setting: SharedPreferences

    var user_session = ""
    var liveId = ""
    var tagToken = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        user_session = pref_setting.getString("user_session", "") ?: ""

        // LiveID
        liveId = arguments?.getString("liveId", "") ?: ""
        // タグ編集に使うトークン
        tagToken = arguments?.getString("tagToken", "") ?: ""

        bottom_fragment_tag_recyclerview.apply {
            setHasFixedSize(true)
            val mLayoutManager = LinearLayoutManager(context)
            layoutManager = mLayoutManager
            tagRecyclerViewAdapter = TagRecyclerViewAdapter(recyclerViewList)
            adapter = tagRecyclerViewAdapter
        }

        tagRecyclerViewAdapter.apply {
            this.tagToken = this@NicoLiveTagBottomFragment.tagToken
            this.user_session = this@NicoLiveTagBottomFragment.user_session
            this.liveId = this@NicoLiveTagBottomFragment.liveId
            this.bottomFragment = this@NicoLiveTagBottomFragment
        }

        bottom_fragment_tag_add_button.setOnClickListener {
            // タグ追加
            GlobalScope.launch {
                val nicoLiveTagAPI = NicoLiveTagAPI()
                val tokenText = bottom_fragment_tag_edittext.text.toString()
                // タグ追加API叩く
                val response =
                    nicoLiveTagAPI.addTag(liveId, user_session, tagToken, tokenText).await()
                if (!response.isSuccessful) {
                    return@launch
                }
                programFragment.apply {
                    showToast("${getString(R.string.added)}：$tokenText")
                    // 番組情報Fragmentのタグも取得（更新）
                    coroutineGetTag()
                    tagCoroutine()
                }
            }
        }

        tagCoroutine()

    }

    // タグAPI叩く。メインスレッド指定
    fun tagCoroutine() {
        recyclerViewList.clear()
        GlobalScope.launch(Dispatchers.Main) {
            val nicoLiveTagAPI = NicoLiveTagAPI()
            val response = nicoLiveTagAPI.getTags(liveId, user_session).await()
            if (!response.isSuccessful) {
                return@launch
            }
            val jsonString = response.body?.string()
            // RecyclerViewへ
            val list = nicoLiveTagAPI.parseTags(jsonString)
            list.forEach {
                recyclerViewList.add(it)
            }
            activity?.runOnUiThread {
                tagRecyclerViewAdapter.notifyDataSetChanged()
                // タグ件数
                parseTagSize(jsonString)
            }
        }
    }

    fun parseTagSize(json: String?) {
        val jsonObject = JSONObject(json)
        val data = jsonObject.getJSONObject("data")
        val userTagCount = data.getInt("userTagCount")
        val userTagMax = data.getInt("userTagMax")
        bottom_fragment_tag_textview.text = "タグ件数：$userTagCount/$userTagMax"
    }

}