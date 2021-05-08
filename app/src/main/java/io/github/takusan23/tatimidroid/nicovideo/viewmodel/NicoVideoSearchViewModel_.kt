package io.github.takusan23.tatimidroid.nicovideo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.takusan23.tatimidroid.room.entity.SearchHistoryDBEntity
import io.github.takusan23.tatimidroid.room.init.SearchHistoryDBInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ニコ動の検索Fragmentで使うViewModel
 *
 * 検索履歴など
 * */
class NicoVideoSearchViewModel_(application: Application) : AndroidViewModel(application) {

    /** Context */
    private val context = application.applicationContext

    /** データベース */
    private val searchHistoryDB = SearchHistoryDBInit.getInstance(context)

    /** DAO */
    private val searchHistoryDAO = searchHistoryDB.searchHistoryDAO()

    /** 検索履歴でピン止め済みの履歴を送信するLiveData */
    val searchHistoryPinnedListLiveData = MutableLiveData<List<SearchHistoryDBEntity>>()

    /** 検索履歴すべてを送信するLiveData */
    val searchHistoryAllListLiveData = MutableLiveData<List<SearchHistoryDBEntity>>()

    init {
        // データベースの中身をLiveDataで送信
        // Flowでデータベース変更通知を受け取るように
        viewModelScope.launch (Dispatchers.IO){
            searchHistoryDAO.realtimeGetAll().collect{
                searchHistoryAllListLiveData.postValue(it)
            }
        }
        viewModelScope.launch(Dispatchers.IO){
            searchHistoryDAO.realtimeGetPinnedSearchHistory().collect {
                searchHistoryPinnedListLiveData.postValue(it)
            }
        }
    }

    /**
     * 検索履歴をピン止め、解除する
     *
     * @param searchHistoryDBEntity 変更対象のデータ
     * @param isPin ピン止めするならtrue。解除するならfalse
     * */
    fun setPin(searchHistoryDBEntity: SearchHistoryDBEntity, isPin: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val insertData = searchHistoryDBEntity.copy(pin = isPin)
            searchHistoryDAO.update(insertData)
        }
    }

    /**
     * 検索履歴を削除する
     *
     * @param searchHistoryDBEntity 削除対象
     * */
    fun deleteSearchResult(searchHistoryDBEntity: SearchHistoryDBEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            searchHistoryDAO.delete(searchHistoryDBEntity)
        }
    }

}