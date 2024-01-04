package com.example.weatherforecastapp

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.weatherforecast.ui.theme.WeatherForecastTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//region ホーム画面のアクティビティ
class HomeActivity : ComponentActivity() {
    //region 定数・変数
    // ビューモデル
    private val viewModel: HomeViewModel by viewModels { HomeViewModelFactory(application) }
    // 現在位置用
    private var currentCity: String? = null
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 位置情報の権限が許可されていない場合はユーザーに許可を求める
        if (!Common.checkLocationPermission(this)) {
            Common.requestLocationPermission(this)
        } else {
            lifecycleScope.launch {
                //現在の位置情報を取得する
                val locationInfo = withContext(Dispatchers.IO) {
                    Common.requestLocation(this@HomeActivity)
                }
                // Google Maps APIを利用して座標から都道府県名を取得
                currentCity = withContext(Dispatchers.IO) {
                    Common.getCityFromLocation(
                        locationInfo.latitude,
                        locationInfo.longitude,
                        this@HomeActivity
                    )
                }
            }
        }

        setContent {
            WeatherForecastTheme {
                val cities by viewModel.cities.observeAsState(null)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeDisplay(cities) { city ->
                        // クリックしたときの処理
                        onItemClicked(city)
                    }
                }
            }
        }
    }

    // 都道府県が選択されたときの処理
    private fun onItemClicked(city: String) {
        // MainActivityを開くIntentを作成
        val intent = Intent(this, MainActivity::class.java)

        // Intentに選択した都市を追加
        intent.putExtra("selectedCity", city)
        intent.putExtra("currentCity", currentCity)

        // MainActivityを開始
        startActivity(intent)
    }
}

/**
 * HomeViewModelのファクトリクラス
 */
class HomeViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
//endregion

//region Compose
/**
 * ホーム画面のCompose
 * @param cities 表示する都道府県のリスト
 * @param onItemClicked 都道府県がクリックされたときに呼び出されるコールバック
 */
@Composable
fun HomeDisplay(cities: List<String>?, onItemClicked: (String) -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxSize()
    ) {
        cities?.forEachIndexed { index, city ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                WeatherItem(city, onItemClicked, isOdd = index % 2 == 1)
            }
        }
    }
}

/**
 * 天気アイテムのCompose
 * @param city 表示する都道府県名
 * @param onItemClicked 都道府県がクリックされたときに呼び出されるコールバック
 * @param isOdd アイテムが奇数番目かのフラグ
 */
@Composable
fun WeatherItem(city: String, onItemClicked: (String) -> Unit, isOdd: Boolean = false) {
    // 奇数・偶数番目で色変更
    val backgroundColor = if (isOdd) Color.Black else Color.White
    val textColor = if (isOdd) Color.White else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onItemClicked(city) }
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // 都道府県
        Text(
            // 縦書きにする
            text = buildAnnotatedString {
                (cityMap[city]?.let { stringResource(it) } ?: "").forEachIndexed { index, char ->
                    append("$char\n")
                }
            },
            fontSize = 30.sp,
            color = textColor
        )
    }
}
//endregion
