package io.github.takusan23.tatimidroid.DevNicoVideo

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoMyListListFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoSearchFragment
import io.github.takusan23.tatimidroid.Tool.*
import io.github.takusan23.tatimidroid.Tool.IDRegex
import io.github.takusan23.tatimidroid.Tool.NICOVIDEO_ID_REGEX
import io.github.takusan23.tatimidroid.Tool.NICOVIDEO_MYLIST_ID_REGEX
import io.github.takusan23.tatimidroid.Tool.calcAnniversary
import io.github.takusan23.tatimidroid.Tool.isConnectionInternet
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.android.synthetic.main.fragment_nicovideo_info.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * 動画情報Fragment
 * */
class NicoVideoInfoFragment : Fragment() {

    lateinit var pref_setting: SharedPreferences

    var videoId = ""
    var usersession = ""

    var jsonObjectString = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        usersession = pref_setting.getString("user_session", "") ?: ""

        //動画ID受け取る（sm9とかsm157とか）
        videoId = arguments?.getString("id") ?: "sm157"
        // キャッシュ再生なら
        val isCache = arguments?.getBoolean("cache") ?: false

        if (jsonObjectString.isNotEmpty()) {
            parseJSONApplyUI(jsonObjectString)
        }


        fragment_nicovideo_info_description_textview.movementMethod = LinkMovementMethod.getInstance()

    }

    override fun onStart() {
        super.onStart()
        val fragment = fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment
        if (fragment.isInitJsonObject() && fragment.jsonObject.toString().isNotEmpty()) {
            parseJSONApplyUI(fragment.jsonObject.toString())
        }
    }

    /**
     * JSONをパースしてUIに反映させる
     * @param jsonString js-initial-watch-data.data-api-dataの値
     * */
    fun parseJSONApplyUI(jsonString: String) {

        // Fragment無いなら落とす
        if (!isAdded) return

        val json = JSONObject(jsonString)

        val threadObject = json.getJSONObject("thread")
        val commentCount = threadObject.getString("commentCount")

        //ユーザー情報。公式動画だと取れない。
        var nickname = ""
        var userId = ""
        var iconURL = ""
        if (!json.isNull("owner")) {
            val ownerObject = json.getJSONObject("owner")
            nickname = ownerObject.getString("nickname")
            userId = ownerObject.getString("id")
            iconURL = ownerObject.getString("iconURL")
        }
        //公式動画では代わりにチャンネル取る。
        if (!json.isNull("channel")) {
            val ownerObject = json.getJSONObject("channel")
            nickname = ownerObject.getString("name")
            userId = ownerObject.getString("globalId")
            //iconURL = ownerObject.getString("iconURL")
        }

        val videoObject = json.getJSONObject("video")

        val id = videoObject.getString("id")
        val title = videoObject.getString("title")
        val description = videoObject.getString("description")
        val postedDateTime = videoObject.getString("postedDateTime")

        val viewCount = videoObject.getString("viewCount")
        val mylistCount = videoObject.getString("mylistCount")

        //タグ
        val tagArray = json.getJSONArray("tags")

        activity?.runOnUiThread {
            // Fragmentがアタッチされているか確認する。
            if (!isDetached) {
                //UIスレッド
                fragment_nicovideo_info_title_textview.text = title
                setLinkText(HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT), fragment_nicovideo_info_description_textview)
                // 投稿日時、再生数 等
                fragment_nicovideo_info_upload_textview.text = "${getString(R.string.post_date)}：$postedDateTime"
                fragment_nicovideo_info_play_count_textview.text = "${getString(R.string.play_count)}：$viewCount"
                fragment_nicovideo_info_mylist_count_textview.text = "${getString(R.string.mylist)}：$mylistCount"
                fragment_nicovideo_info_comment_count_textview.text = "${getString(R.string.comment_count)}：$commentCount"
                fragment_nicovideo_info_owner_textview.text = nickname

                // 今日の日付から計算
                fragment_nicovideo_info_upload_day_count_textview.text = "今日の日付から ${getDayCount(postedDateTime)} 日前に投稿"
                // 一周年とか。
                val anniversary = calcAnniversary(toUnixTime(postedDateTime)) // AnniversaryDateクラス みて
                if (anniversary != -1) {
                    fragment_nicovideo_info_upload_anniversary_textview.apply {
                        visibility = View.VISIBLE
                        text = AnniversaryDate.makeAnniversaryMessage(anniversary) // お祝いメッセージ作成
                    }
                }

                //たぐ
                fragment_nicovideo_info_title_linearlayout.removeAllViews()
                for (i in 0 until tagArray.length()) {
                    val tag = tagArray.getJSONObject(i)
                    val name = tag.getString("name")
                    val isDictionaryExists = tag.getBoolean("isDictionaryExists") //大百科があるかどうか
                    val linearLayout = LinearLayout(context)
                    linearLayout.orientation = LinearLayout.HORIZONTAL
                    //ボタン
                    val button = Button(context)
                    //大きさとか
                    val linearLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    linearLayoutParams.weight = 1F
                    button.layoutParams = linearLayoutParams
                    button.text = name
                    button.isAllCaps = false
                    linearLayout.addView(button)
                    if (isDictionaryExists) {
                        val dictionaryButton = Button(context)
                        dictionaryButton.text = getString(R.string.dictionary)
                        linearLayout.addView(dictionaryButton)
                        //大百科ひらく
                        dictionaryButton.setOnClickListener {
                            openBrowser("https://dic.nicovideo.jp/a/$name")
                        }
                    }
                    fragment_nicovideo_info_title_linearlayout.addView(linearLayout)

                    // タグ検索FragmentをViewPagerに追加する
                    button.setOnClickListener {
                        // オフライン時は動かさない
                        if (isConnectionInternet(context)) {
                            // DevNicoVideoFragment
                            val fragment = parentFragmentManager.findFragmentByTag(id) as DevNicoVideoFragment
                            val searchFragment = DevNicoVideoSearchFragment().apply {
                                arguments = Bundle().apply {
                                    putString("search", name)
                                    putBoolean("search_hide", true)
                                }
                            }
                            // 追加位置
                            val addPos = fragment.viewPager.fragmentList.size
                            // ViewPager追加
                            fragment.viewPager.addFragment(searchFragment, "${getString(R.string.tag)}：$name")
                            // ViewPager移動
                            fragment.fragment_nicovideo_viewpager.currentItem = addPos
                        }
                        // 動画IDのとき。例：「後編→sm」とか
                        val id = IDRegex(name)
                        if (id != null) {
                            Snackbar.make(button, "${getString(R.string.find_video_id)} : $id", Snackbar.LENGTH_SHORT)
                                .apply {
                                    setAction(R.string.play) {
                                        val intent =
                                            Intent(context, NicoVideoActivity::class.java).apply {
                                                putExtra("id", id)
                                            }
                                        startActivity(intent)
                                    }
                                    show()
                                }
                        }
                    }
                }

                //ユーザーページ
                fragment_nicovideo_info_owner_textview.setOnClickListener {
                    if (!userId.contains("co")) {
                        openBrowser("https://www.nicovideo.jp/user/$userId")
                    } else {
                        //チャンネルの時、ch以外にもそれぞれアニメの名前を入れても通る。例：te-kyu2 / gochiusa など
                        openBrowser("https://ch.nicovideo.jp/$userId")
                    }
                }

            }
        }
    }


    /** 投稿日のフォーマットをUnixTimeへ変換する */
    private fun toUnixTime(postedDateTime: String) = SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(postedDateTime).time

    /**
     * 動画投稿日が何日前か数えるやつ。
     * @param upDateTime yyyy/MM/dd HH:mm:ssの形式で。
     *
     * */
    private fun getDayCount(upDateTime: String): String {
        // UnixTime（ミリ秒）へ変換
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        // 時、分とかは切り捨てる（多分いらない。）
        val calendar = Calendar.getInstance().apply {
            time = simpleDateFormat.parse(upDateTime)
            set(Calendar.HOUR, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        // 現在時刻から引く
        val calc = System.currentTimeMillis() - calendar.time.time
        // 計算で出す。多分もっといい方法ある。
        val second = calc / 1000    // ミリ秒から秒へ
        val minute = second / 60    // 秒から分へ
        val hour = minute / 60      // 分から時間へ
        val day = hour / 24         // 時間から日付へ
        return day.toString()
    }

    fun openBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)

    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * TextViewのリンク（mylist/数字）とか（sm157）とかを押したときブラウザ開くのではなくこのアプリ内で表示できるようにする。
     * */
    fun setLinkText(text: Spanned, textView: TextView) {
        // リンクを付ける。
        val span = Spannable.Factory.getInstance().newSpannable(text.toString())
        // 動画ID押せるように。ちなみに↓の変数は
        val mather = NICOVIDEO_ID_REGEX.toPattern().matcher(text)
        while (mather.find()) {
            // 動画ID取得
            val id = mather.group()
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 再生画面表示
                    val intent = Intent(context, NicoVideoActivity::class.java).apply {
                        putExtra("id", id)
                    }
                    activity?.finish()
                    startActivity(intent)
                }
            }, mather.start(), mather.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // マイリスト押せるように
        val mylistMatcher = NICOVIDEO_MYLIST_ID_REGEX.toPattern().matcher(text)
        while (mylistMatcher.find()) {
            val mylist = mylistMatcher.group()
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // マイリスト表示FragmentをViewPagerに追加する
                    val mylistFragment = DevNicoVideoMyListListFragment().apply {
                        arguments = Bundle().apply {
                            putString("mylist_id", mylist.replace("mylist/", ""))// IDだけくれ
                            putBoolean("is_other", true)
                        }
                    }
                    // DevNicoVideoFragment
                    (parentFragmentManager.findFragmentByTag(videoId) as DevNicoVideoFragment).apply {
                        // ViewPager追加
                        viewPager.addFragment(mylistFragment, "${getString(R.string.mylist)}：$mylist")
                        // ViewPager移動
                        fragment_nicovideo_viewpager.currentItem = viewPager.fragmentTabName.size
                    }
                }
            }, mylistMatcher.start(), mylistMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // URL押せるように
        val URL_REGEX = "https?://[\\w!?/\\+\\-_~=;\\.,*&@#\$%\\(\\)\\'\\[\\]]+"
        val urlMather = URL_REGEX.toPattern().matcher(text)
        while (urlMather.find()) {
            val url = urlMather.group()
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // ブラウザ
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    startActivity(intent)
                }
            }, urlMather.start(), urlMather.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // 再生時間へ移動。例：#25:25で25:25へ移動できる
        val SEEK_TIME_REGEX = "(#)([0-9][0-9]):([0-9][0-9])"
        val seekTimeMatcher = SEEK_TIME_REGEX.toPattern().matcher(text)
        while (seekTimeMatcher.find()) {
            val time = seekTimeMatcher.group().replace("#", "")
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 再生時間操作
                    (fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment).apply {
                        if (isInitExoPlayer()) {
                            // 分：秒　を ミリ秒へ
                            val minute = time.split(":")[0].toLong() * 60
                            val second = time.split(":")[1].toLong()
                            exoPlayer.seekTo(((minute + second) * 1000))
                        }
                    }
                }
            }, seekTimeMatcher.start(), seekTimeMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        textView.text = span
        textView.movementMethod = LinkMovementMethod.getInstance();
    }

}
