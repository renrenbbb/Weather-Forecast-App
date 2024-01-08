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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherforecast.ui.theme.WeatherForecastTheme

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
        Common.checkAndRequestLocationPermission(this)

        // ViewModelでデータをロード
        viewModel.loadData()

        setContent {
            WeatherForecastTheme {
                val cities by viewModel.cities.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeDisplay(cities) { city ->
                        // 都市項目クリックイベント
                        onItemClicked(city)
                    }
                }
            }
        }
    }

    /**
     * 天気画面アクティビティをスタート
     * @param city 都市
     */
    private fun startMainActivity(city: String) {
        // 天気画面アクティビティをスタートする
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("selectedCity", city)
        intent.putExtra("currentCity", currentCity)
        startActivity(intent)
    }

    //region イベント
    /**
     * 都市項目クリックイベント
     * @param city 都市
     */
    private fun onItemClicked(city: String) {
        startMainActivity(city)
    }
    //endregion
}
//endregion

//region ファクトリクラス
/**
 * HomeViewModelのファクトリクラス
 */
class HomeViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            HomeViewModel(application) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
//endregion

//region Compose
/**
 * ホーム画面のCompose
 * @param cities 表示する都市のリスト
 * @param onItemClicked 都市がクリックされたときに呼び出されるコールバック
 */
@Composable
fun HomeDisplay(cities: List<String>?, onItemClicked: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize()
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
 * @param city 表示する都市名
 * @param onItemClicked 都市がクリックされたときに呼び出されるコールバック
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
        // 都市項目
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