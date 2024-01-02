package com.example.weatherforecastapp

import android.app.Application
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
class HomeViewModel(private val context: Application) : ViewModel() {
    private val _cities = MutableLiveData<List<String>>()
    val cities: LiveData<List<String>>
        get() = _cities

    init {
        // ロード時にAPI呼び出しを行う
        loadData()
    }

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
class MainViewModel() : ViewModel() {
    private val _weatherInfo = mutableStateOf<WeatherInfo?>(null)
    val weatherInfo: State<WeatherInfo?> = _weatherInfo

    @OptIn(ExperimentalMaterial3Api::class)
    private val _drawerState = MutableLiveData<DrawerValue>()

    @OptIn(ExperimentalMaterial3Api::class)
    val drawerState: LiveData<DrawerValue> = _drawerState

    init {
        // ロード時にAPI呼び出しを行う
        //loadData()
    }

    // 非同期で天気情報を取得するメソッド
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

    // Drawerを閉じる関数
    @OptIn(ExperimentalMaterial3Api::class)
    fun closeDrawer() {
        _drawerState.value = DrawerValue.Closed
    }

//    private fun loadData() {
//        viewModelScope.launch {
//            try {
//            } catch (e: Exception) {
//                // エラーハンドリング
//            }
//        }
//    }
//
//    // この関数を呼ぶことでAPI再読み込みが可能
//    fun reloadData() {
//        loadData()
//    }
}
//endregion