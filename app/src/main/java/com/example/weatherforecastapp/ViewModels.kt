package com.example.weatherforecastapp

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    // 都市リスト
    private val _cities = MutableStateFlow<List<String>>(emptyList())
    val cities: StateFlow<List<String>> = _cities

    /**
     * 初期化ブロック
     */
    init {
        // データをロード
        loadData()
    }

    /**
     * 都市データを非同期で読み込む
     */
    fun loadData() {
        viewModelScope.launch {
            try {
                // デフォルトの都市をセット
                _cities.value = listOf(
                    context.getString(R.string.hokkaido_romaji),
                    context.getString(R.string.tokyo_romaji),
                    context.getString(R.string.hyogo_romaji),
                    context.getString(R.string.oita_romaji)
                )
            } catch (e: Exception) {
                // エラーハンドリング
                println(e.message)
            }
        }
    }
}
//endregion

//region 天気画面のビューモデル
/**
 * 天気画面のビューモデルクラス
 */
class MainViewModel(private val activity: MainActivity) : ViewModel() {
    // 天気情報を保持するためのState
    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)
    val weatherInfo: StateFlow<WeatherInfo?> = _weatherInfo

    // 現在の都市
    private var currentCity: String = ""

//    @OptIn(ExperimentalMaterial3Api::class)
//    private val _stableDrawerState = MutableStateFlow(DrawerValue.Closed)
//
//    @OptIn(ExperimentalMaterial3Api::class)
//    val stableDrawerState: MutableStateFlow<DrawerValue> = _stableDrawerState

    // isVisibleをMutableStateで管理
    private var isVisible by mutableStateOf(false)

    // isVisibleの変更関数
    fun setVisibility(value: Boolean) {
        isVisible = value
    }

    /**
     * 非同期で天気情報を取得
     * @param city 対象の都市
     */
    fun getWeatherInfo(city: String = "") {
        viewModelScope.launch {
            try {
                setVisibility(false)

                var targetCity = if (city.isNotEmpty()) city else currentCity
                if (targetCity == activity.getString(R.string.currentlocation)) {
                    if (Common.checkLocationPermission(activity)) {
                        getCurrentCity()
                        targetCity = currentCity
                        //setVisibility(true)
                    }
                }

                var result = withContext(Dispatchers.IO) {
                    Common.get5dayWeather(targetCity, activity.getPrefs(targetCity))
                }

                _weatherInfo.value = result

                // 天気情報が取得できた場合はアプリへ保存
                result?.let {
                    activity.saveWeatherInfoToPrefs(targetCity, it)
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    /**
     * 位置情報から都市を取得
     */
    fun getCurrentCity() {

        viewModelScope.launch {
            val locationInfo = withContext(Dispatchers.IO) {
                Common.requestLocation(activity)
            }

            // 現在位置の都市へセット
            currentCity = withContext(Dispatchers.IO) {
                Common.getCityFromLocation(
                    locationInfo.latitude,
                    locationInfo.longitude,
                    activity
                )
            }
        }
    }
}
//endregion