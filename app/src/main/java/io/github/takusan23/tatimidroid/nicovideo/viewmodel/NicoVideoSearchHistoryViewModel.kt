package io.github.takusan23.tatimidroid.nicovideo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.takusan23.tatimidroid.room.entity.SearchHistoryDBEntity
import io.github.takusan23.tatimidroid.room.init.SearchHistoryDBInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 検索履歴を表示するBottomFragmentのViewModel
 * */
class NicoVideoSearchHistoryViewModel(application: Application) : AndroidViewModel(application) {

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
        viewModelScope.launch(Dispatchers.IO) {
            searchHistoryPinnedListLiveData.postValue(searchHistoryDAO.getPinnedSearchHistory().reversed())
            searchHistoryAllListLiveData.postValue(searchHistoryDAO.getAll().reversed())
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