package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.app.ShareCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import io.github.takusan23.droppopalert.DropPopAlert
import io.github.takusan23.droppopalert.toDropPopAlert
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.DropPopAlertMotionLayoutFix
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.NicoLive.CommentViewFragment
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.PlayerBaseFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.startLivePlayService
import io.github.takusan23.tatimidroid.Tool.*
import io.github.takusan23.tatimidroid.databinding.IncludeNicoliveEnquateBinding
import io.github.takusan23.tatimidroid.databinding.IncludeNicolivePlayerBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ニコ生再生Fragment。一部Jetpack Composeで作る。新UI
 *
 * 一部の関数は[PlayerBaseFragment]に実装しています
 *
 * 入れてほしいもの
 *
 * liveId       | String | 番組ID
 * watch_mode   | String | 現状 comment_post のみ
 * */
class JCNicoLiveFragment : PlayerBaseFragment() {

    /** 保存するやつ */
    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    /** プレイヤー部分のUI */
    private val nicolivePlayerUIBinding by lazy { IncludeNicolivePlayerBinding.inflate(layoutInflater) }

    /** そうですね、やっぱり僕は、王道を征く、ExoPlayerですか */
    private val exoPlayer by lazy { SimpleExoPlayer.Builder(requireContext()).build() }

    /** 共有 */
    val contentShare = ContentShare(this)

    /** ViewModel初期化。ネットワークとかUI関係ないやつはこっちに書いていきます。 */
    val viewModel by lazy {
        val liveId = arguments?.getString("liveId")!!
        ViewModelProvider(this, NicoLiveViewModelFactory(requireActivity().application, liveId, true)).get(NicoLiveViewModel::class.java)
    }

    @ExperimentalAnimationApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // プレイヤー追加など
        setPlayerUI()

        // フォント設定
        setFont()

        // LiveData監視
        setLiveData()

        // Fragment設置
        setFragment()

        // コメント送信用UI（Jetpack Compose）設定
        setCommentPostUI()

        // スリープにしない
        caffeine()

        // アニメーション
        if (viewModel.isFirst) {
            viewModel.isFirst = false
            miniPlayerAnimation()
        }
    }

    /** Jetpack Composeで作成したコメント投稿UIを追加する */
    @ExperimentalAnimationApi
    private fun setCommentPostUI() {
        // コメント一覧展開ボタンを設置する
        bottomComposeView.apply {
            setContent {
                // コメント展開するかどうか
                val isComment = viewModel.commentListShowLiveData.observeAsState(initial = false)
                // コルーチン
                val scope = rememberCoroutineScope()
                // コメント本文
                val commentPostText = remember { mutableStateOf("") }
                // 匿名で投稿するか
                val isTokumeiPost = remember { mutableStateOf(viewModel.nicoLiveHTML.isPostTokumeiComment) }
                // 文字の大きさ
                val commentSize = remember { mutableStateOf("medium") }
                // 文字の位置
                val commentPos = remember { mutableStateOf("naka") }
                // 文字の色
                val commentColor = remember { mutableStateOf("white") }

                NicoLiveCommentInputButton(
                    onClick = {
                        viewModel.commentListShowLiveData.postValue(!isComment.value)
                    },
                    isComment = isComment.value,
                    comment = commentPostText.value,
                    commentChange = { commentPostText.value = it },
                    onPostClick = {
                        // コメント投稿
                        scope.launch {
                            viewModel.sendComment(commentPostText.value, commentColor.value, commentSize.value, commentPos.value, false)
                            commentPostText.value = "" // クリアに
                        }
                    },
                    position = commentPos.value,
                    size = commentSize.value,
                    color = commentColor.value,
                    onPosValueChange = { commentPos.value = it },
                    onSizeValueChange = { commentSize.value = it },
                    onColorValueChange = { commentColor.value = it },
                    is184 = isTokumeiPost.value,
                    onTokumeiChange = {
                        // 匿名、生ID切り替わった時
                        isTokumeiPost.value = !isTokumeiPost.value
                        prefSetting.edit { putBoolean("nicolive_post_tokumei", it) }
                        viewModel.nicoLiveHTML.isPostTokumeiComment = it
                    },
                )
            }
        }
    }

    /** Fragment設置 */
    private fun setFragment() {
        // 動画情報Fragment、コメントFragment設置
        childFragmentManager.beginTransaction().replace(fragmentHostFrameLayout.id, JCNicoLiveInfoFragment()).commit()
        childFragmentManager.beginTransaction().replace(fragmentCommentHostFrameLayout.id, CommentViewFragment()).commit()
        // ダークモード
        fragmentCommentHostFrameLayout.background = ColorDrawable(getThemeColor(requireContext()))
        // コメント一覧Fragmentを表示するかどうかのやつ
        viewModel.commentListShowLiveData.observe(viewLifecycleOwner) { isShow ->
            // アニメーション？自作ライブラリ
            val dropPopAlert = fragmentCommentHostFrameLayout.toDropPopAlert()
            if (isShow) {
                dropPopAlert.showAlert(DropPopAlert.ALERT_UP)
            } else {
                dropPopAlert.hideAlert(DropPopAlert.ALERT_UP)
            }
        }
    }

    /** LiveData監視 */
    private fun setLiveData() {
        // ミニプレイヤーなら
        viewModel.isMiniPlayerMode.observe(viewLifecycleOwner) { isMiniPlayerMode ->
            // アイコン直す
            nicolivePlayerUIBinding.includeNicolivePlayerCloseImageView.setImageDrawable(getCurrentStateIcon())
            // 画面回転前がミニプレイヤーだったらミニプレイヤーにする
            if (isMiniPlayerMode) {
                toMiniPlayer() // これ直したい
            }
        }
        // Activity終了などのメッセージ受け取り
        viewModel.messageLiveData.observe(viewLifecycleOwner) {
            when (it) {
                getString(R.string.encryption_video_not_play) -> finishFragment()
            }
        }
        // SnackBarを表示しろメッセージを受け取る
        viewModel.snackbarLiveData.observe(viewLifecycleOwner) {
            showSnackBar(it, null, null)
        }
        // 番組情報
        viewModel.nicoLiveProgramData.observe(viewLifecycleOwner) { data ->
            setLiveInfo(data)
        }
        // 新ニコニコ実況の番組と発覚した場合
        viewModel.isNicoJKLiveData.observe(viewLifecycleOwner) { nicoJKId ->
            // バックグラウンド再生無いので非表示
//            nicolivePlayerUIBinding.includeNicolivePlayerBackgroundImageView.isVisible = false
            // 映像を受信しない。
            showSnackBar(getString(R.string.nicolive_jk_not_live_receive), null, null)
            // 映像を受信しないモードをtrueへ
            viewModel.isNotReceiveLive.postValue(true)
        }
        // 映像を受信しないモード。映像なしだと3分で620KBぐらい？
        viewModel.isNotReceiveLive.observe(viewLifecycleOwner) { isNotReceiveLive ->
            if (isNotReceiveLive) {
                // 背景真っ暗へ
                nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.background = ColorDrawable(Color.BLACK)
                exoPlayer.release()
            } else {
                // 生放送再生
                viewModel.hlsAddressLiveData.value?.let { playExoPlayer(it) }
            }
        }
        // うんこめ
        viewModel.unneiCommentLiveData.observe(viewLifecycleOwner) { unnkome ->
            showInfoOrUNEIComment(CommentJSONParse(unnkome, getString(R.string.room_integration), viewModel.liveIdOrCommunityId).comment)
        }
        // あんけーと
        viewModel.startEnquateLiveData.observe(viewLifecycleOwner) { enquateList ->
            setStartEnquateLayout(enquateList)
        }
        viewModel.openEnquateLiveData.observe(viewLifecycleOwner) { perList ->
            setResultEnquateLayout(perList)
        }
        viewModel.stopEnquateLiveData.observe(viewLifecycleOwner) { stop ->
            nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.removeAllViews()
        }
        // 統計情報
        viewModel.statisticsLiveData.observe(viewLifecycleOwner) { statistics ->

        }
        // アクティブユーザー？
        viewModel.activeCommentPostUserLiveData.observe(viewLifecycleOwner) { active ->

        }
        // 経過時間
        viewModel.programTimeLiveData.observe(viewLifecycleOwner) { programTime ->
            nicolivePlayerUIBinding.includeNicolivePlayerCurrentTimeTextView.text = programTime
        }
        // 終了時刻
        viewModel.formattedProgramEndTime.observe(viewLifecycleOwner) { endTime ->
            nicolivePlayerUIBinding.includeNicolivePlayerDurationTextView.text = endTime
        }
        // HLSアドレス取得
        viewModel.hlsAddressLiveData.observe(viewLifecycleOwner) { address ->
            playExoPlayer(address)
        }
        // 画質変更
        viewModel.changeQualityLiveData.observe(viewLifecycleOwner) { quality ->
            showSnackBar("${getString(R.string.successful_quality)}\n→${quality}", null, null)
        }
        // コメントうけとる
        viewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { commentJSONParse ->
            // 豆先輩とか
            if (!commentJSONParse.comment.contains("\n")) {
                nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.postComment(commentJSONParse.comment, commentJSONParse)
            } else {
                // https://stackoverflow.com/questions/6756975/draw-multi-line-text-to-canvas
                // 豆先輩！！！！！！！！！！！！！！！！！！
                // 下固定コメントで複数行だとAA（アスキーアートの略 / CA(コメントアート)とも言う）がうまく動かない。配列の中身を逆にする必要がある
                // Kotlinのこの書き方ほんと好き
                val asciiArtComment = if (commentJSONParse.mail.contains("shita")) {
                    commentJSONParse.comment.split("\n").reversed() // 下コメントだけ逆順にする
                } else {
                    commentJSONParse.comment.split("\n")
                }
                // 複数行対応Var
                nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.postCommentAsciiArt(asciiArtComment, commentJSONParse)
            }
        }

    }

    /** アンケート開票のUIをセットする */
    private fun setResultEnquateLayout(perList: List<String>) {
        // 選択肢：パーセンテージのPairを作成する
        if (viewModel.startEnquateLiveData.value != null) {
            // 選択肢
            val enquateList = viewModel.startEnquateLiveData.value!!.drop(1)
            // Pair作成 + Button作成
            val buttonList = perList.mapIndexed { index, percent ->
                MaterialButton(requireContext()).apply {
                    // テキスト
                    text = "${enquateList[index]}\n$percent"
                    // Paddingとか
                    val linearLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    linearLayoutParams.weight = 1F
                    linearLayoutParams.setMargins(10, 10, 10, 10)
                    layoutParams = linearLayoutParams
                }
            }
            // アンケ用レイアウト読み込み
            val nicoliveEnquateLayout = IncludeNicoliveEnquateBinding.inflate(layoutInflater)
            nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.removeAllViews()
            nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.addView(nicoliveEnquateLayout.root)
            nicoliveEnquateLayout.enquateTitle.text = enquateList[0] // 0番目はアンケタイトル
            // ボタン配置
            buttonList.forEachIndexed { index, materialButton ->
                when {
                    index in 0..2 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
                    index in 3..5 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
                    index in 6..8 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
                }
            }
            // アンケ結果共有Snackbar
            val shareText = perList.mapIndexed { index, percent -> "${enquateList[index]}\n$percent" }.joinToString(separator = "\n")
            showSnackBar(getString(R.string.enquate_result), getString(R.string.share)) {
                ShareCompat.IntentBuilder.from(requireActivity()).apply {
                    setChooserTitle(viewModel.startEnquateLiveData.value!![0])
                    setSubject(shareText)
                    setText(shareText)
                    setType("text/plain")
                }.startChooser()
            }
        }
    }

    /** アンケート開始のUIをセットする。引数の配列は0番目がタイトル、それ以降がアンケートの選択肢 */
    private fun setStartEnquateLayout(enquateList: List<String>) {
        // 何回目か教えてくれるmapIndexed
        val buttonList = enquateList
            .drop(1)
            .mapIndexed { i, enquate ->
                MaterialButton(requireContext()).apply {
                    // テキスト
                    text = enquate
                    // Paddingとか
                    val linearLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    linearLayoutParams.weight = 1F
                    linearLayoutParams.setMargins(10, 10, 10, 10)
                    layoutParams = linearLayoutParams
                    setOnClickListener {
                        // 投票
                        viewModel.enquatePOST(i - 1)
                        // アンケ画面消す
                        nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.removeAllViews()
                        // Snackbar
                        showSnackBar("${getString(R.string.enquate)}：$enquate", null, null)
                    }
                }
            }
        // アンケ用レイアウト読み込み
        val nicoliveEnquateLayout = IncludeNicoliveEnquateBinding.inflate(layoutInflater)
        nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.removeAllViews()
        nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.addView(nicoliveEnquateLayout.root)
        nicoliveEnquateLayout.enquateTitle.text = enquateList[0] // 0番目はアンケタイトル
        // ボタン配置
        buttonList.forEachIndexed { index, materialButton ->
            when {
                index in 0..2 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
                index in 3..5 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
                index in 6..8 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
            }
        }
    }


    /**
     * Info（ニコニ広告、ランクイン等）と、運営コメントを表示する関数
     * */
    private fun showInfoOrUNEIComment(comment: String) {
        nicolivePlayerUIBinding.root.doOnLayout {
            val isNicoad = comment.contains("/nicoad")
            val isInfo = comment.contains("/info")
            val isUadPoint = comment.contains("/uadpoint")
            val isSpi = comment.contains("/spi")
            val isGift = comment.contains("/gift")
            // エモーション。いらない
            val isHideEmotion = prefSetting.getBoolean("setting_nicolive_hide_emotion", false)
            val isEmotion = comment.contains("/emotion")
            // アニメーション
            val infoAnim = nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.toDropPopAlert() // 自作ライブラリ
            val uneiAnim = nicolivePlayerUIBinding.includeNicolivePlayerUneiCommentTextView.toDropPopAlert()
            when {
                isInfo || isUadPoint -> {
                    // info
                    val message = comment.replace("/info \\d+ ".toRegex(), "")
                    nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.text = message
                    infoAnim.alert(DropPopAlertMotionLayoutFix.ALERT_UP)
                }
                isNicoad -> {
                    // 広告
                    val json = JSONObject(comment.replace("/nicoad ", ""))
                    val message = json.getString("message")
                    nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.text = message
                    infoAnim.alert(DropPopAlertMotionLayoutFix.ALERT_UP)
                }
                isSpi -> {
                    // ニコニコ新市場
                    val message = comment.replace("/spi ", "")
                    nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.text = message
                    infoAnim.alert(DropPopAlertMotionLayoutFix.ALERT_UP)
                }
                isGift -> {
                    // 投げ銭。スペース区切り配列
                    val list = comment.replace("/gift ", "").split(" ")
                    val userName = list[2]
                    val giftPoint = list[3]
                    val giftName = list[5]
                    val message = "${userName} さんが ${giftName} （${giftPoint} pt）をプレゼントしました。"
                    nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.text = message
                    infoAnim.alert(DropPopAlertMotionLayoutFix.ALERT_UP)
                }
                isEmotion && !isHideEmotion -> {
                    // エモーション
                    val message = comment.replace("/emotion ", "エモーション：")
                    nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.text = message
                    infoAnim.alert(DropPopAlertMotionLayoutFix.ALERT_UP)
                }
                else -> {
                    // 生主コメント表示
                    nicolivePlayerUIBinding.includeNicolivePlayerUneiCommentTextView.text = comment
                    uneiAnim.alert(DropPopAlertMotionLayoutFix.ALERT_DROP)
                }
            }
        }
    }

    override fun onBottomSheetProgress(progress: Float) {
        super.onBottomSheetProgress(progress)
        aspectRatioFix()
    }

    /** ExoPlayerで生放送を再生する */
    private fun playExoPlayer(address: String) {
        // ニコ生版ニコニコ実況の場合 と 映像を受信しないモードのとき は接続しないので即return
        if (viewModel.nicoLiveHTML.getNicoJKIdFromChannelId(viewModel.communityId) != null || viewModel.isNotReceiveLive.value == true) {
            return
        }
        // 音声のみの再生はその旨（むね）を表示して、SurfaceViewを暗黒へ。わーわー言うとりますが、お時間でーす
        if (viewModel.currentQuality == "audio_high") {
            nicolivePlayerUIBinding.includeNicolivePlayerAudioOnlyTextView.visibility = View.VISIBLE
            nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.background = ColorDrawable(Color.BLACK)
        } else {
            nicolivePlayerUIBinding.includeNicolivePlayerAudioOnlyTextView.visibility = View.INVISIBLE
            nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.background = null
        }
        // アスペクト比治す
        aspectRatioFix()
        // HLS受け取り
        val mediaItem = MediaItem.fromUri(address.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        // SurfaceView
        exoPlayer.setVideoSurfaceView(nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView)
        // 再生
        exoPlayer.playWhenReady = true
        // ミニプレイヤーから通常画面へ遷移
        var isFirst = true
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                // 一度だけ
                if (isFirst) {
                    isFirst = false
                    // 通常画面へ。なおこいつのせいで画面回転前がミニプレイヤーでもミニプレイヤーにならない
                    toDefaultPlayer()
                    // コメント一覧も表示
                    lifecycleScope.launch {
                        delay(1000)
                        if (!viewModel.isFullScreenMode && !viewModel.isAutoCommentListShowOff) {
                            // フルスクリーン時 もしくは 自動で展開しない場合 は操作しない
                            viewModel.commentListShowLiveData.postValue(true)
                        }
                    }
                } else {
                    exoPlayer.removeListener(this)
                }
            }
        })

        // もしエラー出たら
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)
                error.printStackTrace()
                println("生放送の再生が止まりました。")
                //再接続する？
                //それからニコ生視聴セッションWebSocketが切断されてなければ
                if (!viewModel.nicoLiveHTML.nicoLiveWebSocketClient.isClosed) {
                    println("再度再生準備を行います")
                    activity?.runOnUiThread {
                        //再生準備
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        //SurfaceViewセット
                        exoPlayer.setVideoSurfaceView(nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView)
                        //再生
                        exoPlayer.playWhenReady = true
                        // 再生が止まった時に低遅延が有効になっていればOFFにできるように。安定して見れない場合は低遅延が有効なのが原因
                        if (viewModel.nicoLiveHTML.isLowLatency) {
                            showSnackBar(getString(R.string.error_player), getString(R.string.low_latency_off)) {
                                // 低遅延OFFを送信
                                viewModel.nicoLiveHTML.sendLowLatency(!viewModel.nicoLiveHTML.isLowLatency)
                            }
                        } else {
                            showSnackBar(getString(R.string.error_player), null, null)
                        }
                    }
                }
            }
        })
    }

    /** アスペクト比を治す。サイズ変更の度によぶ必要あり。めんどいので16:9固定で */
    private fun aspectRatioFix() {
        if (!isAdded) return
        fragmentPlayerFrameLayout.doOnLayout {
            val playerHeight = fragmentPlayerFrameLayout.height
            val playerWidth = (playerHeight / 9) * 16
            nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.updateLayoutParams {
                width = playerWidth
                height = playerHeight
            }
        }
    }

    /** 番組情報をUIに反映させる */
    private fun setLiveInfo(data: NicoLiveProgramData) {
        nicolivePlayerUIBinding.apply {
            includeNicolivePlayerTitleTextView.text = data.title
            includeNicolivePlayerVideoIdTextView.text = data.programId
        }
    }

    /** フォント設定を適用 */
    private fun setFont() {
        val font = CustomFont(requireContext())
        if (font.isApplyFontFileToCommentCanvas) {
            // 適用する設定の場合
            nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.typeface = font.typeface
        }
    }

    /** プレイヤーのUIをFragmentに追加する */
    private fun setPlayerUI() {
        // ここは動画と一緒
        addPlayerFrameLayout(nicolivePlayerUIBinding.root)
        // プレイヤー部分の表示設定
        val hideJob = Job()
        nicolivePlayerUIBinding.root.setOnClickListener {
            hideJob.cancelChildren()
            // ConstraintLayoutのGroup機能でまとめてVisibility変更。
            nicolivePlayerUIBinding.includeNicolivePlayerControlGroup.visibility = if (nicolivePlayerUIBinding.includeNicolivePlayerControlGroup.visibility == View.VISIBLE) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            // ちょっと強引
            if (isMiniPlayerMode()) {
                // ConstraintLayoutのGroup機能でまとめてVisibility変更。
                nicolivePlayerUIBinding.includeNicolivePlayerMiniPlayerGroup.visibility = View.INVISIBLE
            }
            // 遅延させて
            lifecycleScope.launch(hideJob) {
                delay(3000)
                nicolivePlayerUIBinding.includeNicolivePlayerControlGroup.visibility = View.INVISIBLE
            }
        }
        // プレイヤー右上のアイコンにWi-Fiアイコンがあるけどあれ、どの方法で再生してるかだから。キャッシュならフォルダーになる
        val playingTypeDrawable = InternetConnectionCheck.getConnectionTypeDrawable(requireContext())
        nicolivePlayerUIBinding.includeNicolivePlayerNetworkImageView.setImageDrawable(playingTypeDrawable)
        nicolivePlayerUIBinding.includeNicolivePlayerNetworkImageView.setOnClickListener { showNetworkTypeMessage() }
        // ミニプレイヤー切り替えボタン
        nicolivePlayerUIBinding.includeNicolivePlayerCloseImageView.setOnClickListener {
            if (isMiniPlayerMode()) {
                toDefaultPlayer()
            } else {
                toMiniPlayer()
            }
        }
        // コメントキャンバス非表示
        nicolivePlayerUIBinding.includeNicolivePlayerCommentHideImageView.setOnClickListener {
            nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.apply {
                isVisible = !isVisible
            }
        }
        // ポップアップ再生
        nicolivePlayerUIBinding.includeNicolivePlayerPopupImageView.setOnClickListener {
            if (viewModel.nicoLiveProgramData.value != null) {
                startLivePlayService(
                    context = requireContext(),
                    mode = "popup",
                    liveId = viewModel.nicoLiveProgramData.value!!.programId,
                    isCommentPost = true,
                    isNicocasMode = false,
                    isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment,
                    startQuality = viewModel.currentQuality
                )
                finishFragment()
            }
        }
        // バックグラウンド再生
        nicolivePlayerUIBinding.includeNicolivePlayerBackgroundImageView.setOnClickListener {
            if (viewModel.nicoLiveProgramData.value != null) {
                startLivePlayService(
                    context = requireContext(),
                    mode = "background",
                    liveId = viewModel.nicoLiveProgramData.value!!.programId,
                    isCommentPost = true,
                    isNicocasMode = false,
                    isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment,
                    startQuality = viewModel.currentQuality
                )
                finishFragment()
            }

        }
        // センサーによる画面回転
        if (prefSetting.getBoolean("setting_rotation_sensor", false)) {
            RotationSensor(requireActivity(), lifecycle)
        }
    }

    override fun onBottomSheetStateChane(state: Int, isMiniPlayer: Boolean) {
        super.onBottomSheetStateChane(state, isMiniPlayer)
        // 展開 or ミニプレイヤー のみ
        if (state == BottomSheetBehavior.STATE_COLLAPSED || state == BottomSheetBehavior.STATE_EXPANDED) {
            (requireActivity() as? MainActivity)?.setVisibilityBottomNav()
            // 一応UI表示
            nicolivePlayerUIBinding.root.performClick()
            // アイコン直す
            nicolivePlayerUIBinding.includeNicolivePlayerCloseImageView.setImageDrawable(getCurrentStateIcon())
            // ViewModelへ状態通知
            viewModel.isMiniPlayerMode.value = isMiniPlayerMode()
        }
    }

    /** 画像つき共有を行う */
    fun showShareSheetMediaAttach() {
        viewModel.nicoLiveProgramData.value?.apply {
            contentShare.shareContentAttachPicture(
                playerView = nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView,
                commentCanvas = nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas,
                programId = programId,
                programName = title
            )
        }
    }

    /** テキストのみ共有を行う */
    fun showShareSheet() {
        viewModel.nicoLiveProgramData.value?.apply {
            contentShare.shareContent(programId, title)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        caffeineUnlock()
    }
}