package com.example.weatherforecastapp

import android.app.Application
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ビューモデル
 */

//region ホーム画面のビューモデル
/**
 * ホーム画面のビューモデルクラス
 * @param context アプリケーションのコンテキスト
 */
class HomeViewModel(private val context: Application) : ViewModel() {
    // LiveDataを使用して都市リストを公開
    private val _cities = MutableLiveData<List<String>>()
    val cities: LiveData<List<String>>
        get() = _cities

    /**
     * 初期化ブロック
     */
    init {
        // ロード時にAPI呼び出しを行う
        loadData()
    }

    /**
     * 都市データを非同期で読み込む
     */
    private fun loadData() {
        viewModelScope.launch {
            try {
                val result = listOf(
                    context.getString(R.string.hokkaido_romaji),
                    context.getString(R.string.tokyo_romaji),
                    context.getString(R.string.hyogo_romaji),
                    context.getString(R.string.oita_romaji)
                )

                _cities.postValue(result)
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }
}
//endregion

//region 天気画面のビューモデル
/**
 * 天気画面のビューモデルクラス
 */
class MainViewModel() : ViewModel() {
    // 天気情報を保持するためのState
    private val _weatherInfo = mutableStateOf<WeatherInfo?>(null)
    val weatherInfo: State<WeatherInfo?> = _weatherInfo

    // Drawerの状態を管理するLiveData
    @OptIn(ExperimentalMaterial3Api::class)
    private val _drawerState = MutableLiveData<DrawerValue>()

    @OptIn(ExperimentalMaterial3Api::class)
    val drawerState: LiveData<DrawerValue> = _drawerState

    // isVisibleを変更する関数
    // isVisibleをMutableStateで管理
    var isVisible by mutableStateOf(false)
        private set

    fun setVisibility(value: Boolean) {
        isVisible = value
    }

    /**
     * 初期化ブロック
     */
    init {
        // ロード時にAPI呼び出しを行う
        //loadData()
    }

    /**
     * 非同期で天気情報を取得
     * @param city 対象の都市
     * @return 取得した天気情報クラス
     */
    suspend fun getWeatherInfo(city: String): WeatherInfo? {
        try {

        } catch (e: Exception) {
            println(e.message)
        }
        return withContext(Dispatchers.IO) {
            // 非同期処理の実行
            val newWeatherInfo =
                Common.get5dayWeather(city)
            // 結果をLiveDataにセット
            _weatherInfo.value = newWeatherInfo

            newWeatherInfo
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
            } catch (e: Exception) {
                // エラーハンドリング
            }
        }
    }

    // この関数を呼ぶことでAPI再読み込みが可能
    fun reloadData() {
        loadData()
    }
}
//endregion