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
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.weatherforecast.ui.theme.WeatherForecastTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.TextStyle

//region 天気画面のアクティビティ
class MainActivity : ComponentActivity(), DateChangeListener {

    //region 定数・変数
    // ビューモデル
    private val viewModel: MainViewModel by viewModels()

    // 現在位置
    private lateinit var currentCity: String

    // レシーバー
    private val receiver = CustomReceiver(this)

    private var weatherInfo: WeatherInfo? = null
    //endregion

    /**
     * onCreateイベント
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 現在位置をセット
        currentCity = intent.getStringExtra("currentCity") ?: ""

        setContent {
            WeatherForecastTheme {
                // 選択された都市名を取得
                val selectedCity = intent.getStringExtra("selectedCity") ?: ""
//                // 天気情報を保持するための状態
//                var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }

                LaunchedEffect(selectedCity) {
                    // 非同期で天気情報を取得
                    weatherInfo = viewModel.getWeatherInfo(selectedCity)

                    // 天気情報を取得できなかった場合はアプリへ保存した5日間の天気予報を取得
                    if (weatherInfo == null) {
                        val sharedPreferences =
                            getSharedPreferences(WEATHER_FORECAST_APP_PREF, Context.MODE_PRIVATE)
                        weatherInfo =
                            Common.getSaveWeather(
                                sharedPreferences.getString(selectedCity, "")!!
                            )
                    }
                }

                // MainDisplayに天気情報を渡す
                MainDisplay(
                    viewModel = viewModel,
                    weatherInfo = viewModel.weatherInfo.value,
                    selectedCity = selectedCity
                ) { city ->
                    // クリック時の処理
                    var selectedCity = city
                    if (selectedCity == getString(R.string.currentlocation)) {
                        selectedCity = currentCity

                        if (selectedCity == null || selectedCity.isEmpty()) {
                            weatherInfo = null

                            if (Common.checkLocationPermission(this)) {
                                lifecycleScope.launch {
                                    //現在の位置情報を取得する
                                    val locationInfo = withContext(Dispatchers.IO) {
                                        Common.requestLocation(this@MainActivity)
                                    }
                                    // Google Maps APIを利用して座標から都道府県名を取得
                                    currentCity = withContext(Dispatchers.IO) {
                                        Common.getCityFromLocation(
                                            locationInfo.latitude,
                                            locationInfo.longitude,
                                            this@MainActivity
                                        )
                                    }
                                }
                            }

                            viewModel.setVisibility(true)
                        }
                    }

                    GlobalScope.launch {
                        try {
                            // 非同期処理を実行
                            val result = viewModel.getWeatherInfo(selectedCity)

                            // UIスレッドで更新するためにMainDispatcherを指定
                            withContext(Dispatchers.Main) {
                                // 結果をUIに反映
                                weatherInfo = result

                                // 天気情報が取得できた場合はアプリへ保存
                                if (weatherInfo != null) {
                                    val sharedPreferences =
                                        getSharedPreferences(
                                            WEATHER_FORECAST_APP_PREF,
                                            Context.MODE_PRIVATE
                                        )
                                    // 5日間の天気予報情報をアプリへ保存するためにJSON文字列を作成
                                    val json = Common.createJsonToSaveWeather(weatherInfo!!)
                                    val editor = sharedPreferences.edit()
                                    editor.putString(selectedCity, json)
                                    editor.apply()
                                }
                            }
                        } catch (e: Exception) {
                            println(e.message)
                        }
                    }
                }
            }
        }
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
            println(e.message)
        }
    }

    /**
     * 日付の変更を検知
     */
    override fun onDateChanged() {
        // 天気情報を再取得する
        GlobalScope.launch {
            try {
                val result = viewModel.getWeatherInfo("Oita")

                withContext(Dispatchers.Main) {
                    // 結果をUIに反映
                    weatherInfo = result

                    // 天気情報が取得できた場合はアプリへ保存
                    if (weatherInfo != null) {
                        val sharedPreferences =
                            getSharedPreferences(
                                WEATHER_FORECAST_APP_PREF,
                                Context.MODE_PRIVATE
                            )
                        // 5日間の天気予報情報をアプリへ保存するためにJSON文字列を作成
                        val json = Common.createJsonToSaveWeather(weatherInfo!!)
                        val editor = sharedPreferences.edit()
                        editor.putString(currentCity, json)
                        editor.apply()
                    }
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }
}
//endregion

//region Compose
/**
 * 天気画面のCompose
 * @param weatherInfo 天気情報クラス
 * @param selectedCity 選択された都市名
 * @param onItemClicked 都市がクリックされたときに呼び出されるコールバック
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDisplay(
    viewModel: MainViewModel,
    weatherInfo: WeatherInfo?,
    selectedCity: String,
    onItemClicked: (String) -> Unit
) {
    val context = LocalContext.current
    // Drawerの状態を管理するためのState
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Drawerの開閉状態を変更
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    // isVisibleの値を外部から変更する関数
    //val setVisibility: (Boolean) -> Unit = { value -> isVisible = value }
    // isVisibleを外部から変更する関数
//    val setVisibility: (Boolean) -> Unit = { value ->
//        viewModel.setVisibility(value)
//    }

    // isVisibleが変更されるたびにViewModelのsetVisibilityを呼び出す
    DisposableEffect(isVisible) {
        viewModel.setVisibility(isVisible)
        onDispose { /* リソースの解放など */ }
    }

    // Drawerを表示するためのコンポーネント
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Drawer内のコンテンツ
            Column(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.7f)
                    .background(colorResource(id = R.color.splightgray))
                    .graphicsLayer(
                        clip = true,
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    )
            ) {
                // 5項目を表示するリスト
                val defaultLocation = stringArrayResource(id = R.array.defaultlocation)

                for (i in 0 until 5) {
                    Text(
                        text = context.getString(
                            cityMap[defaultLocation.getOrNull(i)] ?: R.string.currentlocation_jp
                        ),
                        fontSize = 35.sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 20.dp)
                            .weight(1f)
                            .clickable {
                                // タップした地点の天気情報を取得
                                onItemClicked(defaultLocation.getOrNull(i) ?: "")
                                // ドロワーを閉じる
                                closeDrawer()
                            }
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
                if (weatherInfo != null) {
                    Text(
                        modifier = Modifier
                            .padding(end = 10.dp),
                        text = context.getString(
                            cityMap[weatherInfo.city] ?: R.string.unknown_jp
                        ),
                        fontSize = 30.sp
                    )
                    // 別のTextを追加
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
            )
            {
                if (weatherInfo != null) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 30.dp)
                            .fillMaxWidth()
                    )
                    {
                        Text(
                            modifier = Modifier
                                .padding(end = 20.dp),
                            text = Common.convertDateToString(
                                weatherInfo.weatherDataList[0].date,
                                "yyyy/M/d"
                            ),
                            fontSize = 35.sp
                        )
                        Text(
                            text = Common.getDayOfWeekDisplayName(weatherInfo.weatherDataList[0].date),
                            color = Common.getWeekColor(weatherInfo.weatherDataList[0].date),
                            fontSize = 35.sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(Common.getWeatherIconUrl(weatherInfo.weatherDataList[0].weatherIcon))
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
                                weatherKindMap[weatherInfo.weatherDataList[0].weather]
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
                        text = "${weatherInfo.weatherDataList[0].temperature}℃",
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
            )
            {
                if (weatherInfo != null) {
                    Text(
                        text = "週間予報",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
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
                                )
                                {
                                    Text(
                                        modifier = Modifier
                                            .padding(end = 5.dp),
                                        text = Common.convertDateToString(
                                            weatherInfo.weatherDataList[i].date,
                                            "M/d"
                                        ),
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = Common.getDayOfWeekDisplayName(
                                            weatherInfo.weatherDataList[i].date,
                                            TextStyle.SHORT
                                        ),
                                        color = Common.getWeekColor(weatherInfo.weatherDataList[i].date),
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .weight(1f),
                                )
                                {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(
                                                Common.getWeatherIconUrl(
                                                    weatherInfo.weatherDataList[i].weatherIcon,
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
                                            weatherKindMap[weatherInfo.weatherDataList[i].weather]
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
                                    text = "${weatherInfo.weatherDataList[i].temperature}℃",
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
            )
            {
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
                        onClick = {
                            // 天気情報を再取得
                            onItemClicked(selectedCity)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
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