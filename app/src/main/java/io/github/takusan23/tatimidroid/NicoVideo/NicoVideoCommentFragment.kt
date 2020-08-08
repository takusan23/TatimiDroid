package io.github.takusan23.tatimidroid.NicoVideo

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_comment.*


/**
 * コメント表示Fragment
 * */
class NicoVideoCommentFragment : Fragment() {

    var recyclerViewList = arrayListOf<CommentJSONParse>()
    private lateinit var nicoVideoAdapter: NicoVideoAdapter
    private lateinit var prefSetting: SharedPreferences

    private var usersession = ""
    private var id = "sm157"

    lateinit var recyclerView: RecyclerView

    /**
     * 自動追従（自動でコメント一覧スクロール機能）を利用するか。
     * 上方向へスクロールすることでこの値はfalseになる。（RecyclerView#addOnScrollListener()の部分）
     * falseになったら「追いかけるボタン」を押すことで再度trueになります。
     * */
    var isAutoScroll = true

    /** [NicoVideoFragment]。このFragmentが置いてあるFragment。by lazy で使われるまで初期化しないように */
    private val devNicoVideoFragment by lazy {
        val videoId = arguments?.getString("id")
        parentFragmentManager.findFragmentByTag(videoId) as? NicoVideoFragment
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        //動画ID受け取る（sm9とかsm157とか）
        id = arguments?.getString("id") ?: "sm157"
        usersession = prefSetting.getString("user_session", "") ?: ""
        recyclerView = view.findViewById(R.id.activity_nicovideo_recyclerview)

        // 画面回転復帰時
        if (savedInstanceState?.getSerializable("comment") != null) {
            recyclerViewList = savedInstanceState.getSerializable("comment") as ArrayList<CommentJSONParse>
            initRecyclerView(recyclerViewList)
        }

        // スクロールボタン。追従するぞい
        dev_nicovideo_comment_fragment_following_button.setOnClickListener {
            // Fragmentはクソ！
            devNicoVideoFragment?.apply {
                // スクロール
                val currentPos = exoPlayer.currentPosition
                scroll(currentPos)
                // Visibilityゴーン。誰もカルロス・ゴーンの話しなくなったな
                setFollowingButtonVisibility(false)
                isAutoScroll = true
            }
        }
    }


    /**
     * RecyclerView初期化とか
     * @param recyclerViewList RecyclerViewに表示させる中身の配列。省略時はDevNicoVideoCommentFragment.recyclerViewListを使います。
     * @param snackbarShow Toastに取得コメント数を表示させる場合はtrue、省略時はfalse
     * */
    fun initRecyclerView(commentList: ArrayList<CommentJSONParse>) {
        recyclerViewList = commentList
        if (!isAdded) {
            return
        }
        recyclerView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = mLayoutManager
        // Adapter用意。実は第二引数渡さなくても動いたりする（ニコるできなくなるけど）
        nicoVideoAdapter = NicoVideoAdapter(commentList, devNicoVideoFragment)
        recyclerView.adapter = nicoVideoAdapter
        // スクロールイベント。上方向へスクロールをしたら自動追従を解除する設定にした。
        // これで自動スクロール止めたい場合は上方向へスクロールしてください。代わりに追いかけるボタンが表示されます。
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // 自動スクロールが有効になってるときのみ監視する。自動スクロールOFFの状態でも動くようにすると勝手にスクロールされる問題があった。
                if (isAutoScroll) {
                    isAutoScroll = dy >= 0
                }
            }
        })
    }

    /**
     * 追いかけるボタンを表示/非表示する関数。
     * @param milliSec 現在の再生時間。ミリ秒でたのんだ(ExoPlayer#currentPosition)
     * */
    fun setScrollFollowButton(milliSec: Long) {
        // Attachしてなければ落とす
        if (!isAdded) return
        // 追従有効時。この値は上方向スクロールをするとfalseになる。
        if (isAutoScroll) {
            // スクロール実行
            scroll(milliSec)
            // スクロールしたので追いかけるボタンを非表示にする
            setFollowingButtonVisibility(false)
        } else {
            // 追いかけるボタン表示
            setFollowingButtonVisibility(true)
        }
    }

    /**
     * RecyclerViewをスクロールする
     * @param millSeconds 再生時間（ミリ秒！！！）。
     * @param isCheckLastItemTime RecyclerViewに表示してるリストの中で一番最後のアイテムが現在再生中の場所に 一致していなくても スクロールする場合はtrue。名前思いつかなかったわ。
     * */
    fun scroll(milliSec: Long) {
        if (!isAdded) return
        // スクロールしない設定 / ViewPagerまだ初期化してない
        if (prefSetting.getBoolean("nicovideo_comment_scroll", false)) {
            return
        }
        // Nullチェック
        val devNicoVideoCommentFragment = this
        if (devNicoVideoCommentFragment.view?.findViewById<RecyclerView>(R.id.activity_nicovideo_recyclerview) != null) {
            val recyclerView = devNicoVideoCommentFragment.activity_nicovideo_recyclerview
            val list = devNicoVideoCommentFragment.recyclerViewList
            // findを使って条件に合うコメントのはじめの位置を取得する。ここでは 今の時間から一秒引いた時間 と同じか大きいくて最初の値。
            var currentPosCommentFirst = list.indexOfFirst { commentJSONParse -> commentJSONParse.vpos.toInt() >= milliSec / 10 }
            // 上に合わせるんじゃなくて、下に合わせて欲しい。
            val visibleCount = getVisibleCommentList()?.size ?: 0
            currentPosCommentFirst -= visibleCount
            // スクロール
            (recyclerView?.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(currentPosCommentFirst, 0)
        }
    }

    /**
     * 現在表示されているリストの中で一番下に表示されれいるコメントを返す
     * RecyclerViewが初期化されていない場合はnullになります。
     * こいつ関数名考えるの下手だな
     * */
    fun getCommentListVisibleLastItemComment(): CommentJSONParse? {
        // RecyclerView初期化してない時はnull
        if (!isInitRecyclerView()) return null
        return recyclerViewList[(recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()]
    }

    /**
     * 現在表示されているリストの中で一番下に表示されれいるコメントが現在再生中の位置のコメントの場合はtrue
     * @param sec 再生時間。10など
     * @return 表示中の中で一番最後のアイテムが 再生時間 と同じならtrue
     * */
    fun isCheckLastItemTime(sec: Long): Boolean {
        return sec / 10 == getCommentListVisibleLastItemComment()?.vpos?.toLong()
    }

    /**
     * コメント追いかけるボタンを表示、非表示する関数
     * @param visible 表示する場合はtrue。非表示にする場合はfalse
     * */
    fun setFollowingButtonVisibility(visible: Boolean) {
        dev_nicovideo_comment_fragment_following_button?.apply {
            visibility = if (visible) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    /** LayoutManager取得。書くのめんどくさくなったので */
    private fun getRecyclerViewLayoutManager(): LinearLayoutManager? {
        if (!isInitRecyclerView() || recyclerView.layoutManager !is LinearLayoutManager) return null
        return recyclerView.layoutManager as LinearLayoutManager
    }

    /** RecyclerViewが初期化済みかどうかを返す関数 */
    private fun isInitRecyclerView() = ::recyclerView.isInitialized

    /**
     * 現在コメントが表示中かどうか。
     * @param commentJSONParse コメント
     * @return 表示中ならtrue。
     * */
    fun checkVisibleItem(commentJSONParse: CommentJSONParse?): Boolean {
        val rangeItems = getVisibleCommentList() ?: return false
        return rangeItems.find { item -> item == commentJSONParse } != null
    }

    /**
     * 現在RecyclerViewに表示中のコメントを配列で取得する関数
     * 注意：LinearLayoutManager#findLastVisibleItemPosition()が中途半端に表示しているViewのことを数えないので１足してます。
     * */
    fun getVisibleCommentList(): MutableList<CommentJSONParse>? {
        val manager = getRecyclerViewLayoutManager() ?: return null
        // 一番最初+一番最後の場所
        val firstVisiblePos = manager.findFirstVisibleItemPosition()
        val lastVisiblePos = manager.findLastVisibleItemPosition() + 1
        // IndexOutOfBoundsException: fromIndex = -1 対策
        if (firstVisiblePos == -1 || lastVisiblePos == -1) {
            return null
        }
        return recyclerViewList.subList(firstVisiblePos, lastVisiblePos)
    }

    /**
     * 現在RecyclerViewに表示中のコメントがすべて同じ時間（職人のコメントとか長くなって複数行にまたがるからワンチャンある。というかあった）
     * かどうかを判断する関数。
     * 注意：秒レベルで判断します。vposとかは1s=100vposだけど秒になおして扱います。
     * */
    fun getVisibleListItemEqualsTime(): Boolean {
        // 表示中コメント
        val rangeItem = getVisibleCommentList() ?: return false
        // 一番最初の値
        val firstTime = rangeItem.first().vpos.toInt() / 100
        // 同じなら配列から消して、中身が０になれば完成。Array#none{}は全てに一致しなければtrueを返す
        return rangeItem.none { commentJSONParse -> commentJSONParse.vpos.toInt() / 100 != firstTime }
    }

    /**
     * コメント一覧を保存しておく
     * */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("comment", recyclerViewList)
    }

}

