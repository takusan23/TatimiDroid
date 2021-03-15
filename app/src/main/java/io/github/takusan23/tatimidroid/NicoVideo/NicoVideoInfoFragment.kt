package io.github.takusan23.tatimidroid.NicoVideo

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoLikeAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoLikeBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoLikeThanksMessageBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoMyListListFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoSearchFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoSeriesFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.*
import io.github.takusan23.tatimidroid.databinding.FragmentNicovideoInfoBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * 動画情報Fragment
 * */
class NicoVideoInfoFragment : Fragment() {

    val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    val userSession by lazy { prefSetting.getString("user_session", "") ?: "" }
    val videoId by lazy { arguments?.getString("id")!! }

    // NicoVideoFragmentのViewModelを取得する
    val viewModel: NicoVideoViewModel by viewModels({ requireParentFragment() })

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoInfoBinding.inflate(layoutInflater) }

    /** NicoVideoFragmentを取得する */
    private fun requireDevNicoVideoFragment(): NicoVideoFragment {
        return requireParentFragment() as NicoVideoFragment
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 動画説明欄
        viewModel.nicoVideoJSON.observe(viewLifecycleOwner) { json ->
            parseJSONApplyUI(json.toString())
        }

        viewBinding.fragmentNicovideoInfoDescriptionTextview.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * JSONをパースしてUIに反映させる
     * @param jsonString js-initial-watch-data.data-api-dataの値
     * */
    private fun parseJSONApplyUI(jsonString: String) {

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
            if (isAdded) {
                //UIスレッド
                viewBinding.fragmentNicovideoInfoTitleTextview.text = title
                setLinkText(HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT), viewBinding.fragmentNicovideoInfoDescriptionTextview)
                // 投稿日時、再生数 等
                viewBinding.fragmentNicovideoInfoUploadTextview.text = "${getString(R.string.post_date)}：$postedDateTime"
                viewBinding.fragmentNicovideoInfoPlayCountTextview.text = "${getString(R.string.play_count)}：$viewCount"
                viewBinding.fragmentNicovideoInfoMylistCountTextview.text = "${getString(R.string.mylist)}：$mylistCount"
                viewBinding.fragmentNicovideoInfoCommentCountTextview.text = "${getString(R.string.comment_count)}：$commentCount"
                viewBinding.fragmentNicovideoInfoOwnerTextview.text = nickname

                // 今日の日付から計算
                viewBinding.fragmentNicovideoInfoUploadDayCountTextview.text = "今日の日付から ${getDayCount(postedDateTime)} 日前に投稿"
                // 一周年とか。
                val anniversary = calcAnniversary(toUnixTime(postedDateTime)) // AnniversaryDateクラス みて
                when {
                    anniversary == 0 -> {
                        viewBinding.fragmentNicovideoInfoUploadTextview.setTextColor(Color.RED)
                    }
                    anniversary != -1 -> {
                        viewBinding.fragmentNicovideoInfoUploadAnniversaryTextview.apply {
                            isVisible = true
                            text = AnniversaryDate.makeAnniversaryMessage(anniversary) // お祝いメッセージ作成
                        }
                    }
                }

                // 投稿者情報がない場合は消す
                if (nickname.isEmpty()) {
                    viewBinding.fragmentNicovideoInfoOwnerTextview.isVisible = false
                } else {
                    viewBinding.fragmentNicovideoInfoOwnerCardview.setOnClickListener {
                        if (!userId.contains("co") && !userId.contains("ch")) {
                            // ユーザー情報Fragmentへ飛ばす
                            val accountFragment = NicoAccountFragment().apply {
                                arguments = Bundle().apply {
                                    putString("userId", userId)
                                }
                            }
                            (requireActivity() as MainActivity).setFragment(accountFragment, "account")
                            requireDevNicoVideoFragment().viewBinding.fragmentNicovideoMotionLayout.transitionToState(R.id.fragment_nicovideo_transition_end)
                        } else {
                            //チャンネルの時、ch以外にもそれぞれアニメの名前を入れても通る。例：te-kyu2 / gochiusa など
                            openBrowser("https://ch.nicovideo.jp/$userId")
                        }
                    }
                }

                // 投稿者アイコン。インターネット接続時
                if (isConnectionInternet(context) && iconURL.isNotEmpty()) {
                    // ダークモード対策
                    viewBinding.fragmentNicovideoInfoOwnerImageview.imageTintList = null
                    Glide.with(viewBinding.fragmentNicovideoInfoOwnerImageview)
                        .load(iconURL)
                        .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                        .into(viewBinding.fragmentNicovideoInfoOwnerImageview)
                }

                //たぐ
                viewBinding.fragmentNicovideoInfoTitleLinearlayout.removeAllViews()
                for (i in 0 until tagArray.length()) {
                    val tag = tagArray.getJSONObject(i)
                    val name = tag.getString("name")
                    val isDictionaryExists = tag.getBoolean("isDictionaryExists") //大百科があるかどうか
                    val linearLayout = LinearLayout(context)
                    linearLayout.orientation = LinearLayout.HORIZONTAL
                    //ボタン
                    val button = Button(context)
                    //大きさとか
                    val linearLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
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
                    viewBinding.fragmentNicovideoInfoTitleLinearlayout.addView(linearLayout)

                    // タグ検索FragmentをViewPagerに追加する
                    button.setOnClickListener {
                        // オフライン時は動かさない
                        if (isConnectionInternet(context)) {
                            val searchFragment = NicoVideoSearchFragment().apply {
                                arguments = Bundle().apply {
                                    putString("search", name)
                                    putBoolean("search_hide", true)
                                    putBoolean("sort_show", true)
                                }
                            }
                            // 追加位置
                            val addPos = requireDevNicoVideoFragment().viewPagerAdapter.fragmentList.size
                            // ViewPager追加
                            requireDevNicoVideoFragment().viewPagerAdapter.addFragment(searchFragment, "${getString(R.string.tag)}：$name")
                            // ViewPager移動
                            requireDevNicoVideoFragment().viewBinding.fragmentNicovideoViewpager.currentItem = addPos
                        }
                        // 動画IDのとき。例：「後編→sm」とか
                        val id = IDRegex(name)
                        if (id != null) {
                            Snackbar.make(button, "${getString(R.string.find_video_id)} : $id", Snackbar.LENGTH_SHORT).apply {
                                setAction(R.string.play) {
                                    (requireActivity() as? MainActivity)?.setNicovideoFragment(videoId = videoId)
                                }
                                show()
                            }
                        }
                    }
                }
/*
                //ユーザーページ
                fragment_nicovideo_info_owner_textview.setOnClickListener {
                    if (!userId.contains("co")) {
                        openBrowser("https://www.nicovideo.jp/user/$userId")
                    } else {
                        //チャンネルの時、ch以外にもそれぞれアニメの名前を入れても通る。例：te-kyu2 / gochiusa など
                        openBrowser("https://ch.nicovideo.jp/$userId")
                    }
                }
*/
                // いいね機能
                setLike()
            }
        }
    }

    private fun setLike() {
        if (viewModel.isOfflinePlay.value == false && isLoginMode(context)) {
            // キャッシュじゃない　かつ　ログイン必須モード
            viewBinding.fragmentNicovideoInfoLikeChip.isVisible = true
            // LiveData監視
            viewModel.isLikedLiveData.observe(viewLifecycleOwner) { isLiked ->
                setLikeChipStatus(isLiked)
            }
            // お礼メッセージ監視
            viewModel.likeThanksMessageLiveData.observe(viewLifecycleOwner) {
                if(!viewModel.isAlreadyShowThanksMessage) {
                    val thanksMessageBottomFragment = NicoVideoLikeThanksMessageBottomFragment()
                    thanksMessageBottomFragment.show(parentFragmentManager, "thanks")
                }
            }
            // いいね押したとき
            viewBinding.fragmentNicovideoInfoLikeChip.setOnClickListener {
                if (viewModel.isLikedLiveData.value == true) {
                    requireDevNicoVideoFragment().showSnackbar(getString(R.string.unlike), getString(R.string.torikesu)) {
                        // いいね解除API叩く
                        viewModel.removeLike()
                    }
                } else {
                    // いいね開く
                    val nicoVideoLikeBottomFragment = NicoVideoLikeBottomFragment()
                    nicoVideoLikeBottomFragment.show(parentFragmentManager, "like")
                }
            }
        }
    }

    /**
     * MultilineなSnackbar。Material Design的にはよろしくない
     * https://stackoverflow.com/questions/30705607/android-multiline-snackbar
     * */
    private fun multiLineSnackbar(view: View, message: String, time: Int = Snackbar.LENGTH_SHORT): Snackbar {
        val snackbar = Snackbar.make(view, message, time)
        val snackbarView = snackbar.view
        val textView = snackbarView.findViewById(R.id.snackbar_text) as TextView
        textView.maxLines = Int.MAX_VALUE
        return snackbar
    }

    /** ハートのアイコン色とテキストを変更する関数 */
    private fun setLikeChipStatus(liked: Boolean) {
        requireActivity().runOnUiThread {
            // いいね済み
            if (liked) {
                viewBinding.fragmentNicovideoInfoLikeChip.apply {
                    chipIconTint = ColorStateList.valueOf(Color.parseColor("#ffc0cb")) // ピンク
                    text = getString(R.string.liked) // いいね済み
                }
            } else {
                viewBinding.fragmentNicovideoInfoLikeChip.apply {
                    chipIconTint = ColorStateList.valueOf(getThemeTextColor(context)) // テーマの色
                    text = getString(R.string.like) // いいね済み
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
        // 動画ID押せるように。ちなみに↓の変数はニコ動の動画ID正規表現
        val mather = NICOVIDEO_ID_REGEX.toPattern().matcher(text)
        while (mather.find()) {
            // 動画ID取得
            val id = mather.group()
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 再生画面表示
                    (requireActivity() as? MainActivity)?.setNicovideoFragment(videoId = id, isCache = false)
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
                    val mylistFragment = NicoVideoMyListListFragment().apply {
                        arguments = Bundle().apply {
                            putString("mylist_id", mylist.replace("mylist/", ""))// IDだけくれ
                            putBoolean("is_other", true)
                        }
                    }
                    requireDevNicoVideoFragment().apply {
                        // ViewPager追加
                        viewPagerAdapter.addFragment(mylistFragment, "${getString(R.string.mylist)}：$mylist")
                        // ViewPager移動
                        viewBinding.fragmentNicovideoViewpager.currentItem = viewPagerAdapter.fragmentTabName.size
                    }
                }
            }, mylistMatcher.start(), mylistMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // シリーズ押せるように
        val seriesMatcher = NICOVIDEO_SERIES_ID_REGEX.toPattern().matcher(text)
        while (seriesMatcher.find()) {
            val series = seriesMatcher.group()
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // マイリスト表示FragmentをViewPagerに追加する
                    val seriesFragment = NicoVideoSeriesFragment().apply {
                        arguments = Bundle().apply {
                            putString("series_id", series.replace("series/", "")) // IDだけくれ
                            putString("series_title", series) // シリーズのタイトル知らないのでIDでごめんね
                        }
                    }
                    requireDevNicoVideoFragment().apply {
                        // ViewPager追加
                        viewPagerAdapter.addFragment(seriesFragment, "${getString(R.string.series)}：$series")
                        // ViewPager移動
                        viewBinding.fragmentNicovideoViewpager.currentItem = viewPagerAdapter.fragmentTabName.size
                    }
                }
            }, seriesMatcher.start(), seriesMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
                    requireDevNicoVideoFragment().apply {
                        // 分：秒　を ミリ秒へ
                        val minute = time.split(":")[0].toLong() * 60
                        val second = time.split(":")[1].toLong()
                        exoPlayer.seekTo((minute + second) * 1000)
                    }
                }
            }, seekTimeMatcher.start(), seekTimeMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        textView.text = span
        textView.movementMethod = LinkMovementMethod.getInstance();
    }

}
