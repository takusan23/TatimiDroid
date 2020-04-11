package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.User
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_program_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat

class ProgramInfoFragment : Fragment() {

    var liveId = ""
    var usersession = ""
    lateinit var pref_setting: SharedPreferences

    // ユーザー、コミュID
    var userId = ""
    var communityId = ""

    // タグ変更に使うトークン
    var tagToken = ""

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

        val fragment =
            (activity as AppCompatActivity).supportFragmentManager.findFragmentByTag(liveId)
        val commentFragment = fragment as CommentFragment

        // 番組情報取得
        programInfoCoroutine()

        // ユーザーフォロー
        fragment_program_info_broadcaster_follow_button.setOnClickListener {
            requestFollow(userId) {
                Toast.makeText(context, "ユーザーをフォローしました。", Toast.LENGTH_SHORT).show()
            }
        }

        // コミュニティフォロー
        fragment_program_info_community_follow_button.setOnClickListener {
            requestCommunityFollow(communityId) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "コミュニティをフォローしました。\n$communityId", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        // 公式番組はユーザーフォローボタンない？
        if (commentFragment.isOfficial) {
            fragment_program_info_broadcaster_follow_button.isEnabled = false
        }

        // タグ編集
        fragment_program_info_tag_add_button.setOnClickListener {
            val nicoLiveTagBottomFragment = NicoLiveTagBottomFragment()
            val bundle = Bundle().apply {
                putString("liveId", liveId)
                putString("tagToken", tagToken)
            }
            nicoLiveTagBottomFragment.arguments = bundle
            nicoLiveTagBottomFragment.programFragment = this@ProgramInfoFragment
            nicoLiveTagBottomFragment.show(childFragmentManager, "bottom_tag")
        }

        // TS予約
        fragment_program_info_timeshift_button.setOnClickListener {
            registerTimeShift {
                println(it.body?.string())
                if (it.isSuccessful) {
                    activity?.runOnUiThread {
                        // 成功したらTS予約リストへ飛ばす
                        Snackbar.make(
                            fragment_program_info_timeshift_button,
                            R.string.timeshift_reservation_successful,
                            Snackbar.LENGTH_LONG
                        ).setAction(R.string.timeshift_list) {
                            val intent =
                                Intent(Intent.ACTION_VIEW, "https://live.nicovideo.jp/my".toUri())
                            startActivity(intent)
                        }.setAnchorView(commentFragment.getSnackbarAnchorView()).show()
                    }
                } else if (it.code == 500) {
                    // 予約済みの可能性。
                    // なお本家も多分一度登録APIを叩いて500エラーのとき登録済みって判断するっぽい？
                    Snackbar.make(
                        fragment_program_info_timeshift_button,
                        R.string.timeshift_reserved,
                        Snackbar.LENGTH_LONG
                    ).setAction(R.string.timeshift_delete_reservation_button) {
                        deleteTimeShift {
                            println(it.body?.string())
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    context,
                                    R.string.timeshift_delete_reservation_successful,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }.setAnchorView(commentFragment.getSnackbarAnchorView()).show()
                }
            }
        }

    }

    /** コルーチン */
    fun programInfoCoroutine() {
        fragment_program_info_tag_linearlayout.removeAllViews()
        GlobalScope.launch {
            val nicolive =
                NicoLiveHTML()
            val responseString = nicolive.getNicoLiveHTML(liveId, usersession).await()
            val jsonObject = nicolive.nicoLiveHTMLtoJSONObject(responseString.body?.string())
            // 番組情報取得
            val program = jsonObject.getJSONObject("program")
            val title = program.getString("title")
            val description = program.getString("description")
            val beginTime = program.getLong("beginTime")
            val endTime = program.getLong("endTime")
            // 放送者
            val supplier = program.getJSONObject("supplier")
            val name = supplier.getString("name")
            // 公式では使わない
            if (supplier.has("programProviderId")) {
                userId = supplier.getString("programProviderId")
                // ユーザー情報取得。フォロー中かどうか判断するため
                val user = User(context, userId)
                val userData = user.getUserCoroutine().await()
                // ユーザーフォロー中？
                if (userData?.isFollowing == true) {
                    activity?.runOnUiThread {
                        fragment_program_info_broadcaster_follow_button.isEnabled = false
                        fragment_program_info_broadcaster_follow_button.text =
                            getString(R.string.is_following)
                    }
                }
            }
            var level = 0
            // 公式番組対応版
            if (supplier.has("level")) {
                level = supplier.getInt("level")
            }
            // UnixTimeから変換
            val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            // フォーマット済み
            val formattedBeginTime = simpleDateFormat.format(beginTime * 1000)
            val formattedEndTime = simpleDateFormat.format(endTime * 1000)


            //UI
            activity?.runOnUiThread {
                fragment_program_info_broadcaster_name.text =
                    "${getString(R.string.broadcaster)} : $name"
                fragment_program_info_broadcaster_level.text =
                    "${getString(R.string.level)} : $level"
                fragment_program_info_time.text =
                    "${getString(R.string.program_start)} : $formattedBeginTime / ${getString(R.string.end_of_program)} : $formattedEndTime"
                fragment_program_info_title.text = title
                fragment_program_info_description.text =
                    HtmlCompat.fromHtml(description, FROM_HTML_MODE_COMPACT)
            }

            // コミュ
            val community = jsonObject.getJSONObject("socialGroup")
            val communityName = community.getString("name")
            var communityLevel = 0
            // 公式番組にはlevelない
            if (community.has("level")) {
                communityLevel = community.getInt("level")
            }
            activity?.runOnUiThread {
                fragment_program_info_community_name.text =
                    "${getString(R.string.community_name)} : $communityName"
                fragment_program_info_community_level.text =
                    "${getString(R.string.community_level)} : $communityLevel"
            }

            // コミュフォロー中？
            val isCommunityFollow =
                jsonObject.getJSONObject("socialGroup").getBoolean("isFollowed")
            if (isCommunityFollow) {
                activity?.runOnUiThread {
                    // 押せないように
                    fragment_program_info_community_follow_button.isEnabled = false
                    fragment_program_info_community_follow_button.text =
                        getString(R.string.followed)
                }
            }

            //たぐ
            val tag = jsonObject.getJSONObject("program").getJSONObject("tag")
            val isTagNotEditable = tag.getBoolean("isLocked") // タグ編集が可能か？
            val tagsList = tag.getJSONArray("list")
            if (tagsList.length() != 0) {
                activity?.runOnUiThread {
                    for (i in 0 until tagsList.length()) {
                        val tag = tagsList.getJSONObject(i)
                        val text = tag.getString("text")
                        val isNicopedia = tag.getBoolean("existsNicopediaArticle")
                        //ボタン作成
                        val button = MaterialButton(context!!)
                        button.text = text
                        button.isAllCaps = false
                        if (isNicopedia) {
                            val nicopediaUrl =
                                tag.getString("nicopediaArticlePageUrl")
                            button.setOnClickListener {
                                val intent = Intent(Intent.ACTION_VIEW, nicopediaUrl.toUri())
                                startActivity(intent)
                            }
                        } else {
                            button.isEnabled = false
                        }
                        fragment_program_info_tag_linearlayout.addView(button)
                    }
                }
            }
            // タグの登録に必要なトークンを取得
            tagToken = tag.getString("apiToken")
            // タグが変更できない場合はボタンをグレーアウトする
            if (isTagNotEditable) {
                fragment_program_info_tag_add_button.apply {
                    activity?.runOnUiThread {
                        isEnabled = false
                        text = getString(R.string.not_tag_editable)
                    }
                }
            }

            // タイムシフト予約済みか
            val userProgramReservation = jsonObject.getJSONObject("userProgramReservation")
            val isReserved = userProgramReservation.getBoolean("isReserved")

            // タイムシフト機能が使えない場合
            // JSONに programTimeshift と programTimeshiftWatch が存在しない場合はTS予約が無効にされている？
            // 存在すればTS予約が利用できる
            val canTSReserve =
                jsonObject.has("programTimeshift") && jsonObject.has("programTimeshiftWatch")
            activity?.runOnUiThread {
                fragment_program_info_timeshift_button.isEnabled = canTSReserve
                if (!canTSReserve) {
                    fragment_program_info_timeshift_button.text =
                        getString(R.string.disabled_ts_reservation)
                }
            }
        }
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
                    communityId = community.getString("id")
                    val community_name = community.getString("name")
                    val community_level = community.getString("communityLevel")
                    //配信者
                    val broadcaster = data.getJSONObject("broadcaster")
                    val name = broadcaster.getString("name")
                    userId = broadcaster.getString("id")
                    //UI
                    activity?.runOnUiThread {
                        fragment_program_info_broadcaster_name.text =
                            "${getString(R.string.broadcaster)} : $name"
                        fragment_program_info_time.text = startTimeFormat
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

    /**
     * ユーザーをフォローする関数。
     * @param userId ユーザーID
     * @param response 成功時呼ばれます。UIスレッドではない。
     * */
    fun requestFollow(userId: String, response: (Response) -> Unit) {
        val request = Request.Builder().apply {
            url("https://public.api.nicovideo.jp/v1/user/followees/niconico-users/${userId}.json")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$usersession")
            // これがないと 200 が帰ってこない
            header(
                "X-Request-With",
                "https://live2.nicovideo.jp/watch/$liveId?_topic=live_user_program_onairs"
            )
            post("{}".toRequestBody()) // 送る内容は｛｝ていいらしい。
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val status =
                        JSONObject(response.body?.string()).getJSONObject("meta").getInt("status")
                    if (status == 200) {
                        // 高階関数をよぶ
                        response(response)
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    /**
     * コミュニティをフォローする関数。
     * @param communityId コミュニティーID coから
     * @param response 成功したら呼ばれます。
     * */
    fun requestCommunityFollow(communityId: String, response: (Response) -> Unit) {
        val formData = FormBody.Builder().apply {
            add("mode", "commit")
            add("title", "フォローリクエスト")
            add("comment", "")
            add("notify", "")
        }.build()
        val request = Request.Builder().apply {
            url("https://com.nicovideo.jp/motion/${communityId}")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$usersession")
            header("Content-Type", "application/x-www-form-urlencoded")
            // Referer これないと200が帰ってくる。（ほしいのは302 Found）
            header("Referer", "https://com.nicovideo.jp/motion/$communityId")
            post(formData) // これ送って何にするの？
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // 302 Foundのとき成功？
                    response(response)
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    /**
     * タイムシフト登録する。 overwriteってなんだ？
     * @param レスポンスが帰ってくれば
     * */
    fun registerTimeShift(successful: (Response) -> Unit) {
        val postFormData = FormBody.Builder().apply {
            // 番組IDからlvを抜いた値を指定する
            add("vid", liveId.replace("lv", ""))
            add("overwrite", "0")
        }.build()
        val request = Request.Builder().apply {
            url("https://live.nicovideo.jp/api/timeshift.reservations")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$usersession")
            header("Content-Type", "application/x-www-form-urlencoded")
            header("Origin", "https://live2.nicovideo.jp") // これが必須の模様
            post(postFormData)
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                successful(response)
            }
        })
    }

    /**
     * タイムシフトを削除する
     * @param successful 成功したら呼ばれます。
     * */
    fun deleteTimeShift(successful: (Response) -> Unit) {
        val request = Request.Builder().apply {
            // 番組IDからlvを抜いた値を指定する
            url(
                "https://live.nicovideo.jp/api/timeshift.reservations?vid=${liveId.replace(
                    "lv",
                    ""
                )}"
            )
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$usersession")
            header("Content-Type", "application/x-www-form-urlencoded")
            header("Origin", "https://live2.nicovideo.jp") // これが必須の模様
            delete()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    successful(response)
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }
}