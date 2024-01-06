package com.example.weatherforecastapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.weatherforecast.ui.theme.WeatherForecastTheme
import kotlinx.coroutines.launch
import java.time.format.TextStyle

//region 天気画面のアクティビティ
class MainActivity : ComponentActivity(), DateChangeListener {

    //region 定数・変数
    // ビューモデル
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(this) }

    // レシーバー
    private val receiver = CustomReceiver(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 権限チェック
        if (Common.checkLocationPermission(this)) {
            // 位置情報から都市を取得
            viewModel.getCurrentCity()
        }

        setContent {
            WeatherForecastTheme {
                val selectedCity = intent.getStringExtra("selectedCity") ?: ""
                val weatherInfo by viewModel.weatherInfo.collectAsState()

                LaunchedEffect(selectedCity) {
                    viewModel.getWeatherInfo(selectedCity)
                }

                MainDisplay(
                    viewModel = viewModel,
                    weatherInfo = weatherInfo,
                    onItemClicked = { city ->
                        handleCityClick(city)
                    }
                )
            }
        }
    }

    /**
     * 5日間の天気予報情報をアプリへ保存
     * @param city 対象の都市
     * @param weatherInfo 天気情報クラス
     */
    fun saveWeatherInfoToPrefs(city: String, weatherInfo: WeatherInfo) {
        val sharedPreferences =
            getSharedPreferences(WEATHER_FORECAST_APP_PREF, Context.MODE_PRIVATE)
        val json = Common.createJsonToSaveWeather(weatherInfo)
        sharedPreferences.edit().putString(city, json).apply()
    }

    /**
     * 保存した天気情報データファイルを取得
     * @param city 対象の都市
     * @return データファイルのJSON文字列
     */
    fun getPrefs(selectedCity: String): String {
        val sharedPreferences =
            getSharedPreferences(WEATHER_FORECAST_APP_PREF, Context.MODE_PRIVATE)

        return sharedPreferences.getString(selectedCity, "")!!
    }

    /**
     * エラーハンドリング
     * @param exception 例外エラー
     */
    private fun handleException(exception: Exception) {
        println(exception.message)
    }

    /**
     * クリックイベント
     * @param city 対象の都市
     */
    private fun handleCityClick(city: String) {
        viewModel.getWeatherInfo(city)
    }

    /**
     * onDestroyイベント
     */
    override fun onDestroy() {
        try {
            super.onDestroy()
            // レシーバーを解除
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            // オフライン時に発生
            handleException(e)
        }
    }

    /**
     * 日付の変更を検知
     */
    override fun onDateChanged() {
        // 天気情報を再取得する
        viewModel.getWeatherInfo()
    }
}

/**
 * HomeViewModelのファクトリクラス
 */
class MainViewModelFactory(private val activity: MainActivity) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            MainViewModel(activity) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
//endregion

//region Compose
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDisplay(
    viewModel: MainViewModel,
    weatherInfo: WeatherInfo?,
    onItemClicked: (String) -> Unit
) {
    // コンテキスト
    val context = LocalContext.current
    // ドロワーの開閉状態
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // 対象の都市(リトライ用)
    var targetCity by remember { mutableStateOf("") }

    // ViewModelからisVisibleを取得し、Composable内で状態を保持
    val isVisible by remember { viewModel.isVisible }

    DisposableEffect(isVisible) {
        viewModel.setVisibility(isVisible)
        onDispose {}
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.7f)
                    .background(colorResource(id = R.color.splightgray))
                    .graphicsLayer(
                        clip = true,
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    )
            ) {
                val defaultLocation = stringArrayResource(id = R.array.defaultlocation)
                for (i in 0 until 5) {
                    Text(
                        text = context.getString(
                            cityMap[defaultLocation.getOrNull(i)] ?: R.string.currentlocation_jp
                        ),
                        fontSize = 35.sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .clickable {
                                // リトライ用に保持してく
                                targetCity = defaultLocation.getOrNull(i) ?: ""
                                onItemClicked(targetCity)
                                scope.launch { drawerState.close() }
                            }
                            .padding(start = 20.dp)
                            .wrapContentSize(Alignment.CenterStart)
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 1つ目のブロック
            Row(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
            ) {
                weatherInfo?.let {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = context.getString(cityMap[it.city] ?: R.string.unknown_jp),
                        fontSize = 30.sp
                    )
                    Text(
                        text = stringResource(id = R.string.is_weather),
                        fontSize = 30.sp
                    )
                }
            }

            // 2つ目のブロック
            Column(
                modifier = Modifier
                    .weight(6f)
                    .fillMaxWidth()
            ) {
                weatherInfo?.let {
                    Row(
                        modifier = Modifier.padding(bottom = 30.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(end = 20.dp),
                            text = Common.convertDateToString(
                                it.weatherDataList[0].date,
                                "yyyy/M/d"
                            ),
                            fontSize = 35.sp
                        )
                        Text(
                            text = Common.getDayOfWeekDisplayName(it.weatherDataList[0].date),
                            color = Common.getWeekColor(it.weatherDataList[0].date),
                            fontSize = 35.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(Common.getWeatherIconUrl(it.weatherDataList[0].weatherIcon))
                                .crossfade(true)
                                .build(),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .clip(CircleShape)
                                .fillMaxHeight()
                                .weight(1f),
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .wrapContentSize(Alignment.CenterStart),
                            text = context.getString(
                                weatherKindMap[it.weatherDataList[0].weather]
                                    ?: R.string.unknown_jp
                            ),
                            fontSize = 60.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        text = "${it.weatherDataList[0].temperature}℃",
                        fontSize = 60.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3つ目のブロック
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxSize()
            ) {
                weatherInfo?.let {
                    Text(
                        text = "週間予報",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        for (i in 1 until 5) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                ) {
                                    Text(
                                        modifier = Modifier.padding(end = 5.dp),
                                        text = Common.convertDateToString(
                                            it.weatherDataList[i].date,
                                            "M/d"
                                        ),
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = Common.getDayOfWeekDisplayName(
                                            it.weatherDataList[i].date,
                                            TextStyle.SHORT
                                        ),
                                        color = Common.getWeekColor(it.weatherDataList[i].date),
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .weight(1f),
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(
                                                Common.getWeatherIconUrl(
                                                    it.weatherDataList[i].weatherIcon,
                                                    false
                                                )
                                            )
                                            .crossfade(true)
                                            .build(),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .weight(1f),
                                        contentDescription = null
                                    )
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .wrapContentSize(Alignment.CenterStart),
                                        text = context.getString(
                                            weatherKindMap[it.weatherDataList[i].weather]
                                                ?: R.string.unknown_jp
                                        ),
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    text = "${it.weatherDataList[i].temperature}℃",
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 4つ目のブロック
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxSize()
            ) {
                if (isVisible) {
                    Text(
                        text = stringResource(id = R.string.networkerror),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    )

                    Button(
                        onClick = { onItemClicked(targetCity) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.retry),
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}
//endregion