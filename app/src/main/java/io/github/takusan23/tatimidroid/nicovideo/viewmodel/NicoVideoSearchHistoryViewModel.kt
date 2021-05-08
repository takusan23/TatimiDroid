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
            // 日付の新しい順に並び替え
            searchHistoryPinnedListLiveData.postValue(searchHistoryDAO.getPinnedSearchHistory().sortedByDescending { searchHistoryDBEntity -> searchHistoryDBEntity.addTime })
            searchHistoryAllListLiveData.postValue(searchHistoryDAO.getAll().sortedByDescending { searchHistoryDBEntity -> searchHistoryDBEntity.addTime })
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

    /** 検索履歴をすべて飛ばす */
    fun deleteAll(){
        viewModelScope.launch(Dispatchers.IO) {
            searchHistoryDAO.deleteAll()
        }
    }

}