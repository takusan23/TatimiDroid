package io.github.takusan23.tatimidroid.Tool

import android.content.Context
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoUpload
import io.github.takusan23.tatimidroid.Room.Entity.NGUploaderUserIdEntity
import io.github.takusan23.tatimidroid.Room.Entity.NGUploaderVideoIdEntity
import io.github.takusan23.tatimidroid.Room.Init.NGUploaderUserIdDBInit
import io.github.takusan23.tatimidroid.Room.Init.NGUploaderVideoIdDBInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

/**
 * NG投稿者、NG投稿者が投稿した動画のデータベース関係のクラス
 *
 * NG機能概要 ---
 *
 * 検索結果のスクレイピングでは投稿者IDまでは取れないので、別の手を打つ必要がある
 *
 * - スマホ版サイトをスクレイピングする
 *    - はいスマホ規制
 * - 検索で返ってきた動画を一個ずつ、動画情報取得APIを叩いて投稿者情報を手に入れる
 *    - さすがにない。無駄な通信
 * - 予めNGにした投稿者の投稿動画の動画IDを控えておいて、控えたID一覧に一致しない動画のみを表示する
 *    - 更新めんどいけどこれがベストプラクティス？
 * */
class NGUploaderTool(val context: Context) {

    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", null) ?: ""

    /** 投稿動画API */
    private val nicoVideoUpload = NicoVideoUpload()

    /** NG投稿者IDデータベース */
    private val ngUserIdDB = NGUploaderUserIdDBInit.getInstance(context)

    /** NG投稿者が投稿した動画IDデータベース */
    private val ngVideoIdDB = NGUploaderVideoIdDBInit.getInstance(context)

    /** NG投稿者一覧をFlowで返す */
    fun getNGUploaderRealTime() = ngUserIdDB.ngUploaderUserIdDAO().getAllRealTime()

    /**
     * NG投稿者として登録する
     *
     * @param userId ユーザーID
     * */
    suspend fun addNGUploaderId(userId: String) = withContext(Dispatchers.Default) {
        val ngUserData = NGUploaderUserIdEntity(
            userId = userId,
            latestUpdateTime = System.currentTimeMillis(),
            addDateTime = System.currentTimeMillis(),
            description = ""
        )
        ngUserIdDB.ngUploaderUserIdDAO().insert(ngUserData)
        // NGユーザーの投稿動画を取得してデータベースへ
        // todo ここはWorkManagerで書きたい
    }

    /**
     * NG投稿者を削除する
     *
     * @param userId ユーザーID
     * */
    suspend fun deleteNGUploaderId(userId: String) = withContext(Dispatchers.Default) {
        ngUserIdDB.ngUploaderUserIdDAO().deleteFromUserId(userId)
        // 投稿動画の方も消す
        ngVideoIdDB.ngUploaderVideoIdDAO().deleteFromUserId(userId)
    }

    /**
     * 指定したユーザーの投稿動画をすべて取得してデータベースに格納する
     *
     * わざと遅延させて取得させているので時間がかかる
     *
     * @param userId ユーザーID
     * */
    suspend fun addNGUploaderAllVideoList(userId: String) = withContext(Dispatchers.Default) {
        // 投稿動画をすべて取得する。ちょっと時間がかかる
        val allVideoList = nicoVideoUpload
            .getAllUploadVideo(userSession, userId)
            .map { nicoVideoData ->
                NGUploaderVideoIdEntity(
                    videoId = nicoVideoData.videoId,
                    userId = userId,
                    addDateTime = System.currentTimeMillis(),
                    description = ""
                )
            }
        // 保存
        allVideoList.forEach { ngUploaderVideoIdEntity ->
            ngVideoIdDB.ngUploaderVideoIdDAO().insert(ngUploaderVideoIdEntity)
        }
    }

    /**
     * 定期実行用。引数に入れたユーザーIDの動画を取得してNG投稿者が投稿した動画IDデータベースを更新する
     *
     * @param userId 投稿者のユーザーID
     * @param maxCount 更新数
     * */
    suspend fun updateNGUploaderVideoList(userId: String, maxCount: Int = 5) = withContext(Dispatchers.Default) {
        val response = nicoVideoUpload.getUploadVideo(
            userId = userId,
            page = 1,
            size = maxCount,
            userSession = userSession
        )
        if (!response.isSuccessful) return@withContext
        // パース
        val videoList = nicoVideoUpload.parseUploadVideo(response.body?.string())
        videoList.forEach { nicoVideoData ->
            // すでに追加済みなら何もしない
            val isAddedVideoId = ngVideoIdDB.ngUploaderVideoIdDAO().isAddedVideoFromId(nicoVideoData.videoId)
            if (!isAddedVideoId) {
                // 登録
                val ngUploaderVideoIdEntity = NGUploaderVideoIdEntity(
                    userId = userId,
                    videoId = nicoVideoData.videoId,
                    addDateTime = System.currentTimeMillis(),
                    description = ""
                )
                ngVideoIdDB.ngUploaderVideoIdDAO().insert(ngUploaderVideoIdEntity)
            }
        }
    }

    /**
     * NG投稿者DBの最終更新日時（[NGUploaderUserIdEntity.latestUpdateTime]）を更新する
     *
     * @param userId ユーザーID
     * */
    private suspend fun updateUserIdDBLatestUpdate(userId: String) = withContext(Dispatchers.Default) {
        // データ取得
        val ngUserIdItem = ngUserIdDB.ngUploaderUserIdDAO().getItemFromUserId(userId)
        // コピーして書き換え
        val updateNGUploaderUserIdEntity = ngUserIdItem.first().copy(latestUpdateTime = System.currentTimeMillis())
        // 更新
        ngUserIdDB.ngUploaderUserIdDAO().update(updateNGUploaderUserIdEntity)
    }

    /**
     * 引数の動画配列の中からNG投稿者が投稿した動画IDがあればそれを取り除いて返す
     *
     * @param videoList 動画配列
     * @return NGを適用した動画配列
     * */
    suspend fun filterNGUploaderVideoId(videoList: List<NicoVideoData>) = withContext(Dispatchers.Default) {
        // 動画DBを取り出す
        val ngVideoId = ngVideoIdDB
            .ngUploaderVideoIdDAO()
            .getAll()
            .map { ngUploaderVideoIdEntity -> ngUploaderVideoIdEntity.videoId }
        // NG投稿者の投稿した動画を取り除いて返す
        videoList.filter { nicoVideoData -> !ngVideoId.contains(nicoVideoData.videoId) }
    }

}