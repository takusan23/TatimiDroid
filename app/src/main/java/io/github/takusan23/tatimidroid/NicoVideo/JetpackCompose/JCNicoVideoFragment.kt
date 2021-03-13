package io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.SeekBar
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import io.github.takusan23.droppopalert.DropPopAlert
import io.github.takusan23.droppopalert.toDropPopAlert
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoCommentFragment
import io.github.takusan23.tatimidroid.NicoVideo.PlayerBaseFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.Factory.NicoVideoViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.PlayerParentFrameLayout
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import io.github.takusan23.tatimidroid.Tool.*
import io.github.takusan23.tatimidroid.databinding.IncludeNicovideoPlayerBinding
import kotlinx.coroutines.*
import kotlin.math.roundToInt

/**
 * 開発中のニコ動クライアント（？）
 *
 * 一部の関数は[PlayerBaseFragment]の方に書いてあるのでなかったらそっちも見に行ってね
 *
 * id           |   動画ID。必須
 * --- 任意 ---
 * cache        |   キャッシュ再生ならtrue。なければfalse
 * eco          |   エコノミー再生するなら（?eco=1）true
 * internet     |   キャッシュ有っても強制的にインターネットを利用する場合はtrue
 * fullscreen   |   最初から全画面で再生する場合は true。
 * video_list   |   連続再生する場合は[NicoVideoData]の配列を[Bundle.putSerializable]使って入れてね
 * start_sec    |   開始位置。秒で
 * */
class JCNicoVideoFragment : PlayerBaseFragment() {

    /** 保存するやつ */
    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    /** プレイヤー部分のUI */
    private val nicovideoPlayerUIBinding by lazy { IncludeNicovideoPlayerBinding.inflate(layoutInflater) }

    /** そうですね、やっぱり僕は、王道を征く、ExoPlayerですか */
    private val exoPlayer by lazy { SimpleExoPlayer.Builder(requireContext()).build() }

    /** シーク操作中かどうか */
    private var isTouchSeekBar = false

    /** 共有 */
    val contentShare = ContentShare(this)

    /** ViewModel。データ取得など */
    val viewModel by lazy {
        // 動画ID
        val videoId = arguments?.getString("id")
        // キャッシュ再生
        val isCache = arguments?.getBoolean("cache")
        // エコノミー再生
        val isEconomy = arguments?.getBoolean("eco") ?: false
        // 強制的にインターネットを利用して取得
        val useInternet = arguments?.getBoolean("internet") ?: false
        // 全画面で開始
        val isStartFullScreen = arguments?.getBoolean("fullscreen") ?: false
        // 連続再生
        val videoList = arguments?.getSerializable("video_list") as? ArrayList<NicoVideoData>
        // 開始位置
        val startPos = arguments?.getInt("start_pos")
        // ViewModel用意
        ViewModelProvider(this, NicoVideoViewModelFactory(requireActivity().application, videoId, isCache, isEconomy, useInternet, isStartFullScreen, videoList, startPos)).get(NicoVideoViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // プレイヤー追加など
        setPlayerUI()

        // ExoPlayer初期化
        initExoPlayer()

        // コメント描画設定。フォント設定など
        setCommentCanvas()

        // LiveData監視
        setLiveData()

        // コメント動かす
        setTimer()

        // 動画情報Fragment設置
        setFragment()

        // スリープにしない
        caffeine()

    }

    /** [JCNicoVideoInfoFragment] / [NicoVideoCommentFragment] を設置する */
    private fun setFragment() {
        // 動画情報Fragment、コメントFragment設置
        childFragmentManager.beginTransaction().replace(fragmentHostFrameLayout.id, JCNicoVideoInfoFragment()).commit()
        childFragmentManager.beginTransaction().replace(fragmentCommentHostFrameLayout.id, NicoVideoCommentFragment()).commit()
        // ダークモード
        fragmentCommentLinearLayout.background = ColorDrawable(getThemeColor(requireContext()))
        // コメント一覧展開ボタンを設置する
        bottomComposeView.apply {
            setContent {
                val isComment = viewModel.commentListShowLiveData.observeAsState(initial = false)
                NicoVideoCommentButton(
                    click = {
                        viewModel.commentListShowLiveData.postValue(!isComment.value)
                    },
                    isComment = isComment.value
                )
            }
        }
        // コメント一覧展開など
        viewModel.commentListShowLiveData.observe(viewLifecycleOwner) { isShow ->
            // アニメーション？自作ライブラリ
            val dropPopAlert = fragmentCommentLinearLayout.toDropPopAlert()
            if (isShow) {
                dropPopAlert.showAlert(DropPopAlert.ALERT_UP)
            } else {
                dropPopAlert.hideAlert(DropPopAlert.ALERT_UP)
            }
        }
    }

    /** コメントと経過時間を定期的に更新していく */
    private fun setTimer() {
        // 勝手に終了してくれるコルーチンコンテキスト
        lifecycleScope.launch {
            while (true) {
                delay(100)
                // 再生時間をコメント描画Canvasへ入れ続ける
                nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.currentPos = viewModel.playerCurrentPositionMs
                nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.isPlaying = viewModel.playerIsPlaying.value!!
                // 再生中のみ
                if (viewModel.playerIsPlaying.value == true) {
                    // ExoPlayerが利用できる場合は再生時間をViewModelへ渡す
                    if (viewModel.isNotPlayVideoMode.value == false) {
                        viewModel.playerCurrentPositionMs = exoPlayer.currentPosition
                    }
                    // シークバー動かす + ViewModelの再生時間更新
                    if (!isTouchSeekBar) {
                        // シークバー操作中でなければ
                        nicovideoPlayerUIBinding.includeNicovideoPlayerSeekBar.progress = (viewModel.playerCurrentPositionMs / 1000L).toInt()
                        viewModel.currentPosition = viewModel.playerCurrentPositionMs
                        // 再生時間TextView
                        val formattedTime = DateUtils.formatElapsedTime(viewModel.playerCurrentPositionMs / 1000L)
                        nicovideoPlayerUIBinding.includeNicovideoPlayerCurrentTimeTextView.text = formattedTime
                    }
                }
            }
        }
    }

    /** LiveDataを監視する。ViewModelの結果を受け取る */
    private fun setLiveData() {
        // ミニプレイヤーなら
        viewModel.isMiniPlayerMode.observe(viewLifecycleOwner) { isMiniPlayerMode ->
            // アイコン直す
            nicovideoPlayerUIBinding.includeNicovideoPlayerCloseImageView.setImageDrawable(getCurrentStateIcon())
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
        // 動画情報
        viewModel.nicoVideoData.observe(viewLifecycleOwner) { nicoVideoData ->
            // ViewPager
            setVideoInfo(nicoVideoData)
        }
        // コメント
        viewModel.commentList.observe(viewLifecycleOwner) { commentList ->
            nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.rawCommentList = commentList
        }
        // 動画再生 or 動画なしモード
        if (viewModel.isCommentOnlyMode) {
            /** コメントのみは [JCNicoVideoCommentOnlyFragment] にあります。 */
        } else {
            // 動画再生
            viewModel.contentUrl.observe(viewLifecycleOwner) { contentUrl ->
                val oldPosition = exoPlayer.currentPosition
                playExoPlayer(contentUrl)
                // 画質変更時は途中から再生。動画IDが一致してないとだめ
                if (oldPosition > 0 && exoPlayer.currentMediaItem?.mediaId == viewModel.playingVideoId.value) {
                    exoPlayer.seekTo(oldPosition)
                }
                exoPlayer.setVideoSurfaceView(nicovideoPlayerUIBinding.includeNicovideoPlayerSurfaceView)
            }
        }
        // 一時停止、再生になったとき
        viewModel.playerIsPlaying.observe(viewLifecycleOwner) { isPlaying ->
            exoPlayer.playWhenReady = isPlaying
            val drawable = if (isPlaying) {
                context?.getDrawable(R.drawable.ic_pause_black_24dp)
            } else {
                context?.getDrawable(R.drawable.ic_play_arrow_24px)
            }
            nicovideoPlayerUIBinding.includeNicovideoPlayerPauseImageView.setImageDrawable(drawable)
            nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.isPlaying = isPlaying
        }
        // シークしたとき
        viewModel.playerSetSeekMs.observe(viewLifecycleOwner) { seekPos ->
            if (0 <= seekPos) {
                viewModel.playerCurrentPositionMs = seekPos
                exoPlayer.seekTo(seekPos)
            } else {
                // 負の値に突入するので０
                viewModel.playerCurrentPositionMs = 0
            }
            // シークさせる
            nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.seekComment()
        }
        // 動画の再生時間
        viewModel.playerDurationMs.observe(viewLifecycleOwner) { duration ->
            if (duration > 0) {
                nicovideoPlayerUIBinding.includeNicovideoPlayerSeekBar.max = (duration / 1000).toInt()
                nicovideoPlayerUIBinding.includeNicovideoPlayerDurationTextView.text = DateUtils.formatElapsedTime(duration / 1000)
            }
        }
        // リピートモードが変わったとき
        viewModel.playerIsRepeatMode.observe(viewLifecycleOwner) { isRepeatMode ->
            if (isRepeatMode) {
                // リピート有効時
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                nicovideoPlayerUIBinding.includeNicovideoPlayerRepeatImageView.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_one_24px))
                prefSetting.edit { putBoolean("nicovideo_repeat_on", true) }
            } else {
                // リピート無効時
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                nicovideoPlayerUIBinding.includeNicovideoPlayerRepeatImageView.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_black_24dp))
                prefSetting.edit { putBoolean("nicovideo_repeat_on", false) }
            }
        }
        // 音量調整
        viewModel.volumeControlLiveData.observe(viewLifecycleOwner) { volume ->
            exoPlayer.volume = volume
        }
        // 次の動画、前の動画
        viewModel.isPlayListMode.observe(viewLifecycleOwner) { isPlaylist ->
            val prevIcon = if (isPlaylist) requireContext().getDrawable(R.drawable.ic_skip_previous_black_24dp) else requireContext().getDrawable(R.drawable.ic_undo_black_24dp)
            val nextIcon = if (isPlaylist) requireContext().getDrawable(R.drawable.ic_skip_next_black_24dp) else requireContext().getDrawable(R.drawable.ic_redo_black_24dp)
            nicovideoPlayerUIBinding.includeNicovideoPlayerPrevImageView.setImageDrawable(prevIcon)
            nicovideoPlayerUIBinding.includeNicovideoPlayerNextImageView.setImageDrawable(nextIcon)
        }
    }

    /** UIに動画情報を反映させる */
    private fun setVideoInfo(nicoVideoData: NicoVideoData) {
        nicovideoPlayerUIBinding.apply {
            includeNicovideoPlayerTitleTextView.text = nicoVideoData.title
            includeNicovideoPlayerTitleTextView.isSelected = true // marquee動かすために
            includeNicovideoPlayerVideoIdTextView.text = nicoVideoData.videoId
        }
    }

    /** ExoPlayerで動画を再生する */
    private fun playExoPlayer(contentUrl: String) {
        // キャッシュ再生と分ける
        when {
            // キャッシュを優先的に利用する　もしくは　キャッシュ再生時
            viewModel.isOfflinePlay.value ?: false -> {
                // キャッシュ再生
                val dataSourceFactory = DefaultDataSourceFactory(requireContext(), "TatimiDroid;@takusan_23")
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.Builder().setUri(contentUrl.toUri()).setMediaId(viewModel.playingVideoId.value).build())
                exoPlayer.setMediaSource(videoSource)
            }
            // それ以外：インターネットで取得
            else -> {
                // SmileサーバーはCookieつけないと見れないため
                val dataSourceFactory = DefaultHttpDataSourceFactory("TatimiDroid;@takusan_23", null)
                dataSourceFactory.defaultRequestProperties.set("Cookie", viewModel.nicoHistory)
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.Builder().setUri(contentUrl.toUri()).setMediaId(viewModel.playingVideoId.value).build())
                exoPlayer.setMediaSource(videoSource)
            }
        }
        // 準備と再生
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        // プログレスバー動かす。View.GONEだとなんかレイアウト一瞬バグる
        nicovideoPlayerUIBinding.includeNicovideoPlayerProgress.visibility = View.VISIBLE

        // 一回だけ動いてほしいのでフラグ
        var isFirst = true
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                if (isFirst) {
                    isFirst = false
                    if (exoPlayer.currentPosition == 0L) {
                        // 画面回転時に２回目以降表示されると邪魔なので制御
                        val progress = prefSetting.getLong("progress_${viewModel.playingVideoId.value}", 0)
                        if (progress != 0L && viewModel.isOfflinePlay.value == true) {
                            // 継承元に実装あり
                            lifecycleScope.launch {
                                delay(500)
                                showSnackBar("${getString(R.string.last_time_position_message)}(${DateUtils.formatElapsedTime(progress / 1000L)})", getString(R.string.play)) {
                                    viewModel.playerSetSeekMs.postValue(progress)
                                }
                            }
                        }
                    }
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
    }

    /** ExoPlayerを初期化する */
    private fun initExoPlayer() {
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                // 動画時間をセットする
                viewModel.playerDurationMs.postValue(exoPlayer.duration)
                // 再生
                viewModel.playerIsPlaying.postValue(exoPlayer.playWhenReady)
                // プログレスバー
                if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                    nicovideoPlayerUIBinding.includeNicovideoPlayerProgress.visibility = View.INVISIBLE
                } else {
                    nicovideoPlayerUIBinding.includeNicovideoPlayerProgress.visibility = View.VISIBLE
                }
                // 動画おわった。連続再生時なら次の曲へ
                if (state == Player.STATE_ENDED && exoPlayer.playWhenReady) {
                    viewModel.nextVideo()
                }
            }
        })
        // 縦、横取得
        exoPlayer.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                // DMCのJSONからも幅とかは取れるけどキャッシュ再生でJSONがない場合をサポートしたいため
                if (isAdded) { // コールバックなのでこの時点でもう無いかもしれない
                    viewModel.apply {
                        videoHeight = height
                        videoWidth = width
                    }
                    aspectRatioFix(width, height)
                }
            }
        })
    }

    override fun onBottomSheetProgress(progress: Float) {
        super.onBottomSheetProgress(progress)
        aspectRatioFix(viewModel.videoWidth, viewModel.videoHeight)
    }

    /**
     * アスペクト比を治す。サイズ変更の度によぶ必要あり。
     * @param height 動画の高さ
     * @param width 動画の幅
     * */
    private fun aspectRatioFix(videoWidth: Int, videoHeight: Int) {
        if (!isAdded) return
        fragmentPlayerFrameLayout.doOnNextLayout {
            val playerHeight = fragmentPlayerFrameLayout.height
            val playerWidth = viewModel.nicoVideoHTML.calcVideoWidthDisplaySize(videoWidth, videoHeight, playerHeight).roundToInt()
            nicovideoPlayerUIBinding.includeNicovideoPlayerSurfaceView.updateLayoutParams {
                width = playerWidth
                height = playerHeight
            }
        }
    }

    /** コメント描画設定。フォント設定など */
    private fun setCommentCanvas() {
        val font = CustomFont(requireContext())
        if (font.isApplyFontFileToCommentCanvas) {
            // 適用する設定の場合
            nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.typeFace = font.typeface
        }
    }

    /** プレイヤーFrameLayoutにUIを追加する */
    @SuppressLint("ClickableViewAccessibility")
    private fun setPlayerUI() {
        addPlayerFrameLayout(nicovideoPlayerUIBinding.root)
        // プレイヤー部分の表示設定
        val hideJob = Job()
        nicovideoPlayerUIBinding.root.setOnClickListener {
            hideJob.cancelChildren()
            // ConstraintLayoutのGroup機能でまとめてVisibility変更。
            nicovideoPlayerUIBinding.includeNicovideoPlayerControlGroup.visibility = if (nicovideoPlayerUIBinding.includeNicovideoPlayerControlGroup.visibility == View.VISIBLE) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            if (isMiniPlayerMode()) {
                // ConstraintLayoutのGroup機能でまとめてVisibility変更。
                nicovideoPlayerUIBinding.includeNicovideoPlayerMiniPlayerGroup.visibility = View.INVISIBLE
            }
            // 遅延させて
            lifecycleScope.launch(hideJob) {
                delay(3000)
                nicovideoPlayerUIBinding.includeNicovideoPlayerControlGroup.visibility = View.INVISIBLE
            }
        }
        // 初回時もちょっとまってから消す
        lifecycleScope.launch(hideJob) {
            delay(3000)
            nicovideoPlayerUIBinding.includeNicovideoPlayerControlGroup.visibility = View.INVISIBLE
        }
        // プレイヤー右上のアイコンにWi-Fiアイコンがあるけどあれ、どの方法で再生してるかだから。キャッシュならフォルダーになる
        val playingTypeDrawable = when {
            viewModel.isOfflinePlay.value ?: false -> requireContext().getDrawable(R.drawable.ic_folder_open_black_24dp)
            else -> InternetConnectionCheck.getConnectionTypeDrawable(requireContext())
        }
        nicovideoPlayerUIBinding.includeNicovideoPlayerNetworkImageView.setImageDrawable(playingTypeDrawable)
        nicovideoPlayerUIBinding.includeNicovideoPlayerNetworkImageView.setOnClickListener { showNetworkTypeMessage() }
        // ミニプレイヤー切り替えボタン
        nicovideoPlayerUIBinding.includeNicovideoPlayerCloseImageView.setOnClickListener {
            if (isMiniPlayerMode()) {
                toDefaultPlayer()
            } else {
                toMiniPlayer()
            }
        }
        // 一時停止
        nicovideoPlayerUIBinding.includeNicovideoPlayerPauseImageView.setOnClickListener {
            viewModel.playerIsPlaying.postValue(!viewModel.playerIsPlaying.value!!)
        }
        // リピートモード変更
        nicovideoPlayerUIBinding.includeNicovideoPlayerRepeatImageView.setOnClickListener {
            viewModel.playerIsRepeatMode.postValue(!viewModel.playerIsRepeatMode.value!!)
        }
        // シーク
        nicovideoPlayerUIBinding.includeNicovideoPlayerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // シークいじったら時間反映されるように
                    val formattedTime = DateUtils.formatElapsedTime((seekBar?.progress ?: 0).toLong())
                    nicovideoPlayerUIBinding.includeNicovideoPlayerCurrentTimeTextView.text = formattedTime
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isTouchSeekBar = true

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isTouchSeekBar = false
                // コメントシークに対応させる
                nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.seekComment()
                // ExoPlayer再開
                viewModel.playerSetSeekMs.postValue((seekBar?.progress ?: 0) * 1000L)
            }
        })
        // ダブルタップ
        nicovideoPlayerUIBinding.root.setOnDoubleClickListener { motionEvent, isDoubleClick ->
            if (motionEvent != null && isDoubleClick) {
                val isLeft = motionEvent.x <= nicovideoPlayerUIBinding.root.width / 2
                // どれだけシークするの？
                val seekValue = prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5
                if (isLeft) {
                    viewModel.playerSetSeekMs.postValue(viewModel.currentPosition - (seekValue * 1000))
                } else {
                    viewModel.playerSetSeekMs.postValue(viewModel.currentPosition + (seekValue * 1000))
                }
            }
        }
        // コメントキャンバス非表示
        nicovideoPlayerUIBinding.includeNicovideoPlayerCommentHideImageView.setOnClickListener {
            val drawable = if (!nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.isVisible) requireContext().getDrawable(R.drawable.ic_comment_on) else requireContext().getDrawable(R.drawable.ic_comment_off)
            nicovideoPlayerUIBinding.includeNicovideoPlayerCommentHideImageView.setImageDrawable(drawable)
            nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.apply {
                isVisible = !isVisible
            }
        }
        // ポップアップ再生
        nicovideoPlayerUIBinding.includeNicovideoPlayerPopupImageView.setOnClickListener {
            if (viewModel.playingVideoId.value != null) {
                startVideoPlayService(
                    context = requireContext(),
                    mode = "popup",
                    videoId = viewModel.playingVideoId.value!!,
                    isCache = viewModel.isOfflinePlay.value!!,
                    seek = viewModel.currentPosition,
                    videoQuality = viewModel.currentVideoQuality,
                    audioQuality = viewModel.currentAudioQuality,
                    playlist = viewModel.playlistLiveData.value
                )
                finishFragment()
            }
        }
        // バックグラウンド再生
        nicovideoPlayerUIBinding.includeNicovideoPlayerBackgroundImageView.setOnClickListener {
            if (viewModel.playingVideoId.value != null) {
                startVideoPlayService(
                    context = requireContext(),
                    mode = "background",
                    videoId = viewModel.playingVideoId.value!!,
                    isCache = viewModel.isOfflinePlay.value!!,
                    seek = viewModel.currentPosition,
                    videoQuality = viewModel.currentVideoQuality,
                    audioQuality = viewModel.currentAudioQuality,
                    playlist = viewModel.playlistLiveData.value
                )
                finishFragment()
            }
        }
        // 押した時
        nicovideoPlayerUIBinding.includeNicovideoPlayerPrevImageView.setOnClickListener { viewModel.prevVideo() }
        nicovideoPlayerUIBinding.includeNicovideoPlayerNextImageView.setOnClickListener { viewModel.nextVideo() }
        // 全画面UI
        nicovideoPlayerUIBinding.includeNicovideoFullScreenImageView.setOnClickListener {
            if (viewModel.isFullScreenMode) {
                setDefaultScreen()
            } else {
                setFullScreen()
            }
        }
        // 全画面モードなら
        if (viewModel.isFullScreenMode) {
            setFullScreen()
        }
        // センサーによる画面回転
        if (prefSetting.getBoolean("setting_rotation_sensor", false)) {
            RotationSensor(requireActivity(), lifecycle)
        }
    }

    /** 全画面UIへ切り替える。非同期です */
    private fun setFullScreen() {
        lifecycleScope.launch {
            viewModel.isFullScreenMode = true
            nicovideoPlayerUIBinding.includeNicovideoFullScreenImageView.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_exit_black_24dp))
            // コメント / 動画情報Fragmentを非表示にする
            toFullScreen()
            // アスペクト比治すなど
            aspectRatioFix(viewModel.videoWidth, viewModel.videoHeight)
        }
    }

    /** 全画面UIを戻す。非同期です */
    private fun setDefaultScreen() {
        lifecycleScope.launch {
            viewModel.isFullScreenMode = false
            nicovideoPlayerUIBinding.includeNicovideoFullScreenImageView.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_black_24dp))
            // コメント / 動画情報Fragmentを表示にする
            toDefaultScreen()
            // アスペクト比治すなど
            aspectRatioFix(viewModel.videoWidth, viewModel.videoHeight)
        }
    }

    /** 画像つき共有をする */
    fun showShareSheetMediaAttach() {
        // 親のFragment取得
        contentShare.shareContentAttachPicture(
            playerView = nicovideoPlayerUIBinding.includeNicovideoPlayerSurfaceView,
            commentCanvas = nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas,
            programId = viewModel.playingVideoId.value,
            programName = viewModel.nicoVideoData.value?.title,
            fromTimeSecond = (exoPlayer.currentPosition / 1000L).toInt()
        )
    }

    /** 共有する */
    fun showShareSheet() {
        // 親のFragment取得
        contentShare.shareContent(
            programId = viewModel.playingVideoId.value,
            programName = viewModel.nicoVideoData.value?.title,
            fromTimeSecond = (exoPlayer.currentPosition / 1000L).toInt()
        )
    }

    override fun onPause() {
        super.onPause()
        viewModel.playerIsPlaying.value = false
        // キャッシュ再生の場合は位置を保存する
        if (viewModel.isOfflinePlay.value == true) {
            prefSetting.edit {
                putLong("progress_${viewModel.playingVideoId.value}", viewModel.playerCurrentPositionMs)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 再生位置を保管。画面回転後LiveDataで受け取る
        viewModel.playerSetSeekMs.value = exoPlayer.currentPosition
        exoPlayer.release()
        caffeineUnlock()
    }

    override fun onBottomSheetStateChane(state: Int, isMiniPlayer: Boolean) {
        super.onBottomSheetStateChane(state, isMiniPlayer)
        // 展開 or ミニプレイヤー のみ
        if (state == PlayerParentFrameLayout.PLAYER_STATE_DEFAULT || state == PlayerParentFrameLayout.PLAYER_STATE_MINI) {
            // (requireActivity() as? MainActivity)?.setVisibilityBottomNav()
            // 一応UI表示
            nicovideoPlayerUIBinding.root.performClick()
            // アイコン直す
            nicovideoPlayerUIBinding.includeNicovideoPlayerCloseImageView.setImageDrawable(getCurrentStateIcon())
            // ViewModelへ状態通知
            viewModel.isMiniPlayerMode.value = isMiniPlayerMode()
        }
    }

}