package com.example.weatherforecastapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import java.util.Properties
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 共通の変数・関数群
 */

//region 定数・変数
val LOCATION_PERMISSION_REQUEST_CODE = 1001

val WEATHER_FORECAST_APP_PREF = "WeatherForecastAppPref"

val OPENWEATHER_API_KEY = Config.getOpenWeatherApiKey()
val GOOGLEMAPS_API_KEY = Config.getGoogleMapsApiKey()

//緯度・経度
val LATITUDE_HOKKAIDO = 43.0643
val LONGITUDE_HOKKAIDO = 141.3468
val LATITUDE_TOKYO = 35.6895
val LONGITUDE_TOKYO = 139.6917
val LATITUDE_HYOGO = 34.6912
val LONGITUDE_HYOGO = 135.1830
val LATITUDE_OITA = 33.2381
val LONGITUDE_OITA = 131.6126

/**
 * デフォルト都道府県座標マップ
 */
val coordinateMap = mapOf(
    "Hokkaido" to Coordinate(LATITUDE_HOKKAIDO, LONGITUDE_HOKKAIDO),
    "Tokyo" to Coordinate(LATITUDE_TOKYO, LONGITUDE_TOKYO),
    "Hyogo" to Coordinate(LATITUDE_HYOGO, LONGITUDE_HYOGO),
    "Oita" to Coordinate(LATITUDE_OITA, LONGITUDE_OITA)
)

/**
 * 都道府県マップ
 */
val cityMap = mapOf(
    "Hokkaido" to R.string.hokkaido,
    "Aomori" to R.string.aomori,
    "Iwate" to R.string.iwate,
    "Miyagi" to R.string.miyagi,
    "Akita" to R.string.akita,
    "Yamagata" to R.string.yamagata,
    "Fukushima" to R.string.fukushima,
    "Ibaraki" to R.string.ibaraki,
    "Tochigi" to R.string.tochigi,
    "Gunma" to R.string.gunma,
    "Saitama" to R.string.saitama,
    "Chiba" to R.string.chiba,
    "Tokyo" to R.string.tokyo,
    "Kanagawa" to R.string.kanagawa,
    "Niigata" to R.string.niigata,
    "Toyama" to R.string.toyama,
    "Ishikawa" to R.string.ishikawa,
    "Fukui" to R.string.fukui,
    "Yamanashi" to R.string.yamanashi,
    "Nagano" to R.string.nagano,
    "Gifu" to R.string.gifu,
    "Shizuoka" to R.string.shizuoka,
    "Aichi" to R.string.aichi,
    "Mie" to R.string.mie,
    "Shiga" to R.string.shiga,
    "Kyoto" to R.string.kyoto,
    "Osaka" to R.string.osaka,
    "Hyogo" to R.string.hyogo,
    "Nara" to R.string.nara,
    "Wakayama" to R.string.wakayama,
    "Tottori" to R.string.tottori,
    "Shimane" to R.string.shimane,
    "Okayama" to R.string.okayama,
    "Hiroshima" to R.string.hiroshima,
    "Yamaguchi" to R.string.yamaguchi,
    "Tokushima" to R.string.tokushima,
    "Kagawa" to R.string.kagawa,
    "Ehime" to R.string.ehime,
    "Kochi" to R.string.kochi,
    "Fukuoka" to R.string.fukuoka,
    "Saga" to R.string.saga,
    "Nagasaki" to R.string.nagasaki,
    "Kumamoto" to R.string.kumamoto,
    "Oita" to R.string.oita,
    "Miyazaki" to R.string.miyazaki,
    "Kagoshima" to R.string.kagoshima,
    "Okinawa" to R.string.okinawa
)

/**
 * 天気種類マップ
 */
val weatherKindMap = mapOf(
    "Clear" to R.string.weather_clear,
    "Clouds" to R.string.weather_clouds,
    "Rain" to R.string.weather_rain,
    "Drizzle" to R.string.weather_drizzle,
    "Thunderstorm" to R.string.weather_thunderstorm,
    "Snow" to R.string.weather_snow
)

/**
 * 曜日の文字色マップ
 */
val weekColorMap = mapOf(
    "SUNDAY" to Color.Red,
    "MONDAY" to Color.Black,
    "TUESDAY" to Color.Black,
    "WEDNESDAY" to Color.Black,
    "THURSDAY" to Color.Black,
    "FRIDAY" to Color.Black,
    "SATURDAY" to Color.Blue
)
//endregion

//region データクラス
/**
 * 天気情報クラス(WeatherDataは5日間のデータ)
 */
data class WeatherInfo(val city: String, val weatherDataList: MutableList<WeatherData>)

/**
 * 天気データクラス
 */
data class WeatherData(
    val date: Date,
    val weather: String,
    val weatherIcon: String,
    val temperature: Int
)

/**
 * 天気データ集計クラス
 */
data class TotalWeatherData(
    val totalTemperature: Int,
    val weatherList: MutableList<String>,
    val weatherIconList: MutableList<String>,
)

/**
 * 座標クラス
 */
data class Coordinate(val latitude: Double, val longitude: Double)
//endregion

//region 共通クラス
class Common {

    //region 静的メンバー
    companion object {

        //region アプリへの保存関連
        /**
         * 5日間の天気予報情報をアプリへ保存するためにJSON文字列を作成
         * @param weatherInfo 天気情報クラス
         * @return 生成されたJSON文字列
         */
        fun createJsonToSaveWeather(weatherInfo: WeatherInfo): String {
            var json = ""

            if (weatherInfo != null) {
                val weatherData0 = weatherInfo.weatherDataList[0]
                val weatherData1 = weatherInfo.weatherDataList[1]
                val weatherData2 = weatherInfo.weatherDataList[2]
                val weatherData3 = weatherInfo.weatherDataList[3]
                val weatherData4 = weatherInfo.weatherDataList[4]

                json = """{
                    "city": "${weatherInfo.city}",
                    "date0": "${convertDateToString(weatherData0.date, "yyyy/MM/dd")}",
                    "weather0": "${weatherData0.weather}",
                    "weathericon0": "${weatherData0.weatherIcon}",
                    "temperature0": "${weatherData0.temperature}",
                    "date1": "${convertDateToString(weatherData1.date, "yyyy/MM/dd")}",
                    "weather1": "${weatherData1.weather}",
                    "weathericon1": "${weatherData1.weatherIcon}",
                    "temperature1": "${weatherData1.temperature}",
                    "date2": "${convertDateToString(weatherData2.date, "yyyy/MM/dd")}",
                    "weather2": "${weatherData2.weather}",
                    "weathericon2": "${weatherData2.weatherIcon}",
                    "temperature2": "${weatherData2.temperature}",
                    "date3": "${convertDateToString(weatherData3.date, "yyyy/MM/dd")}",
                    "weather3": "${weatherData3.weather}",
                    "weathericon3": "${weatherData3.weatherIcon}",
                    "temperature3": "${weatherData3.temperature}",
                    "date4": "${convertDateToString(weatherData4.date, "yyyy/MM/dd")}",
                    "weather4": "${weatherData4.weather}",
                    "weathericon4": "${weatherData4.weatherIcon}",
                    "temperature4": "${weatherData4.temperature}"
                   }"""
            }

            return json
        }

        /**
         * アプリへ保存した5日間の天気予報を取得
         * @param savedJson 保存されたJSON文字列
         * @return 復元された天気情報クラス、取得できない場合はnull
         */
        fun getSaveWeather(savedJson: String): WeatherInfo? {

            if (savedJson.isNotEmpty()) {
                val weatherDataList: MutableList<WeatherData> = mutableListOf()

                try {
                    val jsonData = JSONObject(savedJson)

                    // キーを使用してデータを取得
                    val city = jsonData.getString("city")

                    // 5日間分ループしてデータを取得
                    for (i in 0 until 5) {

                        val dateKey = "date$i"
                        val weatherKey = "weather$i"
                        val weatherIconKey = "weathericon$i"
                        val temperatureKey = "temperature$i"

                        val dateString = jsonData.getString(dateKey)
                        val weather = jsonData.getString(weatherKey)
                        val weatherIcon = jsonData.getString(weatherIconKey)
                        val temperature = jsonData.getString(temperatureKey)

                        // 文字列を日付型へ変換
                        val date = convertStringToDate(dateString, "yyyy/MM/dd")
                        // データクラス作成・追加
                        weatherDataList.add(
                            WeatherData(
                                date,
                                weather,
                                weatherIcon,
                                temperature.toInt()
                            )
                        )
                    }

                    return WeatherInfo(city, weatherDataList)
                } catch (e: Exception) {
                    println(e.message)
                }
            }

            return null
        }
        //endregion

        //region データ型変換
        /**
         * 日付型を文字列へ変換
         * @param date 対象の日付
         * @param format フォーマット文字列
         * @return 変換された文字列
         */
        fun convertDateToString(date: Date, format: String): String {
            val localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern(format)

            return localDateTime.format(formatter)
        }

        /**
         * 文字列を日付型へ変換
         * @param dateString 変換対象の文字列
         * @param format 変換する日付のフォーマット
         * @return 変換された日付型オブジェクト
         */
        private fun convertStringToDate(dateString: String, format: String): Date {
            val formatter = DateTimeFormatter.ofPattern(format)
            val localDate = LocalDate.parse(dateString, formatter)

            return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        }
        //endregion

        //region 天気取得処理
        /**
         * OpenWeatherから5日間の天気を取得
         * @param city 天気を取得する都市
         * @return APIから取得した天気データクラス
         */
        suspend fun get5dayWeather(city: String): WeatherInfo? {
            // OpenWeatherAPIのURLをセット
            val apiUrl =
                "http://api.openweathermap.org/data/2.5/forecast?q=$city&APPID=$OPENWEATHER_API_KEY&units=metric&lang=ja"

            //非同期で天気を取得
            return suspendCoroutine { continuation ->
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        // 天気取得API処理の実行
                        val response = exeGet5dayWeather(apiUrl)

                        if (response != null) {
                            // JSONデータから天気データを抽出
                            val weatherData = extractWeatherDataFromJSON(response)

                            // 天気情報クラスを作成して返す
                            val weatherInfo = WeatherInfo(city, weatherData)
                            continuation.resume(weatherInfo)
                        }
                    } catch (e: Exception) {
                        println(e.message)
                        // 例外が発生した場合はnull
                        continuation.resume(null)
                    }
                }
            }
        }

        /**
         * 天気取得API処理の実行
         * @param apiUrl OpenWeatherAPIのURL
         * @return APIから取得したJSON形式の天気情報
         */
        private fun exeGet5dayWeather(apiUrl: String): String? {
            // OpenWeatherAPIのURLをセット
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"

            try {
                // APIへのリクエストを実行
                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    var line: String?
                    val response = StringBuilder()
                    // レスポンスデータをまとめて取得
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    return response.toString()
                } else {
                    // レスポンスコードが異常な場合はnullを返す
                    return null
                }
            } catch (e: Exception) {
                println(e.message)
                // 例外が発生した場合はnull
                return null
            }
        }

        /**
         * JSONデータから天気データを抽出
         * @param response APIからのJSON形式の応答データ
         * @return 抽出した天気データのリスト
         */
        private fun extractWeatherDataFromJSON(
            response: String
        ): MutableList<WeatherData> {
            val jsonObject = JSONObject(response)
            val listArray = jsonObject.getJSONArray("list")

            // 日付ごとの気温と天気を格納するMap
            val dateMap: MutableMap<String, TotalWeatherData> = mutableMapOf()
            val dateList: MutableList<Date> = mutableListOf()

            for (i in 0 until listArray.length()) {
                val listItem = listArray.getJSONObject(i)
                val utcTimestamp = listItem.getLong("dt")
                // 日付はUTCからJSTへ変換
                val jstDateTime = convertUTCtoJST(utcTimestamp)
                val date = Date.from(jstDateTime.toInstant())
                // 気温
                val temperature = listItem.getJSONObject("main").getDouble("temp").toInt()
                // 天気
                val weather =
                    listItem.getJSONArray("weather").getJSONObject(0).getString("main")
                // 天気アイコン
                val weatherIcon =
                    listItem.getJSONArray("weather").getJSONObject(0).getString("icon")

                // 日付ごとに気温と天気を格納
                val dateKey = date.toString().substring(0, 10)
                if (dateMap.containsKey(dateKey)) {
                    // すでに同じ日付あれば、同日のデータ編集
                    val totalWeatherData = dateMap[dateKey]!!
                    // 平均値を求めるために追加しておく
                    dateMap[dateKey] =
                        TotalWeatherData(
                            totalWeatherData.totalTemperature + temperature,
                            totalWeatherData.weatherList.apply { add(weather) },
                            totalWeatherData.weatherIconList.apply { add(weatherIcon) })
                } else {
                    // 日付も保持しておく
                    dateList.add(date)

                    dateMap[dateKey] = TotalWeatherData(
                        temperature,
                        mutableListOf(weather),
                        mutableListOf(weatherIcon)
                    )
                }
            }

            // 日付ごとに平均気温と平均天気を計算し、リストに追加
            val weatherDataList: MutableList<WeatherData> = mutableListOf()
            var count = 0
            for ((_, value) in dateMap) {
                // 平均気温を計算
                val averageTemperature =
                    value.totalTemperature / value.weatherList.size
                // 平均天気を計算
                val averageFrequentWeather =
                    value.weatherList.groupBy { it }
                        .maxByOrNull { it.value.size }?.key.orEmpty()
                val averageFrequentWeatherIcon =
                    value.weatherIconList.groupBy { it }
                        .maxByOrNull { it.value.size }?.key.orEmpty()

                // 日付ごとに平均値を追加する
                weatherDataList.add(
                    WeatherData(
                        dateList[count],
                        averageFrequentWeather,
                        averageFrequentWeatherIcon,
                        averageTemperature
                    )
                )
                // カウントアップ
                count += 1
            }

            return weatherDataList
        }

        /**
         * UTCからJSTへ変換
         * @param utcTime UTCのタイムスタンプ
         * @return JSTのZonedDateTime
         */
        private fun convertUTCtoJST(utcTime: Long): ZonedDateTime {
            val instant = Instant.ofEpochSecond(utcTime)
            return ZonedDateTime.ofInstant(instant, ZoneId.of("Asia/Tokyo"))
        }

        /**
         * OpenWeatherのアイコンのURLを取得
         * @param icon 天気アイコンのコード
         * @param isToday 当日フラグ（デフォルト：true）
         * @return 天気アイコンのURL
         */
        fun getWeatherIconUrl(icon: String, isToday: Boolean = true): String {
            // 日中のアイコンにする(最後の文字を「d」に変換)
            var editIcon = icon
            if (icon.last() != 'd') {
                editIcon = icon.dropLast(1) + "d"
            }

            // 当日の場合は大きいイメージを使用
            if (isToday) {
                return "https://openweathermap.org/img/wn/$editIcon@4x.png"
            } else {
                return "https://openweathermap.org/img/wn/$editIcon.png"
            }
        }
        //endregion

        //region 現在位置取得処理
        /**
         * 位置情報の権限を確認する
         * @param context 呼び出し元のActivityコンテキスト
         * @return 位置情報の権限が許可されている場合はtrue、それ以外はfalse
         */
        fun checkLocationPermission(context: Activity): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * 位置情報の権限をリクエストする
         * @param context 呼び出し元のActivityコンテキスト
         */
        fun requestLocationPermission(context: Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        /**
         * 現在の位置情報を取得する
         * @param context 呼び出し元のActivityコンテキスト
         * @return 位置情報が取得できた場合は座標クラス、取得できなかった場合は東京の座標を返す
         */
        suspend fun requestLocation(context: Activity): Coordinate =
            withContext(Dispatchers.Default) {
                try {
                    // FusedLocationProviderClientの初期化
                    var fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(context)

                    // 権限が許可されている場合の処理をコルーチンで実行
                    suspendCancellableCoroutine { continuation ->
                        // 権限チェック済み
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { location: Location? ->
                                location?.let {
                                    // 現在の位置情報を取得できた場合
                                    continuation.resume(Coordinate(it.latitude, it.longitude))
                                } ?: run {
                                    // 現在の位置情報を取得できなかった場合
                                    continuation.resume(Coordinate(LATITUDE_TOKYO, LONGITUDE_TOKYO))
                                }
                            }
                    }
                } catch (e: Exception) {
                    println(e.message)
                    // 例外が発生した場合は東京の座標を返す
                    Coordinate(LATITUDE_TOKYO, LONGITUDE_TOKYO)
                }
            }

        /**
         * Google Maps APIを利用して座標から都道府県名を取得
         * @param latitude 緯度
         * @param longitude 経度
         * @param context 呼び出し元のActivityコンテキスト
         * @return 座標から取得した都道府県名、取得できない場合は「不明」
         */
        fun getCityFromLocation(latitude: Double, longitude: Double, context: Activity): String {
            // GoogleMapsGeocodingAPIのURLをセット
            val geocodingApiUrl =
                "https://maps.googleapis.com/maps/api/geocode/json?latlng=$latitude,$longitude&key=$GOOGLEMAPS_API_KEY"

            try {
                // APIへのリクエストを実行
                val response = URL(geocodingApiUrl).readText()
                val jsonObject = JSONObject(response)

                if (jsonObject.getString("status") == "OK") {
                    val results = jsonObject.getJSONArray("results")
                    if (results.length() > 0) {
                        val addressComponents =
                            results.getJSONObject(0).getJSONArray("address_components")
                        for (i in 0 until addressComponents.length()) {
                            val component = addressComponents.getJSONObject(i)
                            val types = component.getJSONArray("types")
                            // 都道府県情報が含まれる場合
                            if (types.toString().contains("administrative_area_level_1")) {
                                // 都道府県名を返す
                                return convertToShiftJIS(component.getString("long_name"))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println(e.message)
            }

            // 失敗した場合は「不明」を返す
            return context.resources.getString(R.string.unknown)
        }

        /**
         * UTF-8からShift-JISに変換
         * @param text 対象の文字列
         * @return Shift-JISに変換された文字列
         */
        private fun convertToShiftJIS(text: String): String {
            return String(text.toByteArray(Charset.forName("UTF-8")), Charset.forName("Shift-JIS"))
        }
        //endregion

        //region その他の関数
        /**
         * 年月日から曜日を名称で取得
         * @param date 対象の日付
         * @param textStyle テキストのスタイル(文字の長さ)
         * @return 対象の日付の曜日名称
         */
        fun getDayOfWeekDisplayName(date: Date, textStyle: TextStyle = TextStyle.FULL): String {
            // 年月日から曜日を取得
            val dayOfWeek = getDayOfWeek(date)
            // TextStyleによって「月」「月曜日」
            return dayOfWeek.getDisplayName(textStyle, Locale.getDefault())
        }

        /**
         * 曜日の文字色を取得
         * @param date 対象の日付
         * @return 曜日に対応する文字色
         */
        fun getWeekColor(date: Date): Color {
            // 年月日から曜日を取得
            val dayOfWeek = getDayOfWeek(date)
            // 月～金：黒色、土：青色、日：赤色を返す
            return weekColorMap[dayOfWeek.name] ?: Color.Black
        }

        /**
         * 年月日から曜日を取得
         * @param date 対象の日付
         * @return 対象の日付の曜日
         */
        private fun getDayOfWeek(date: Date): DayOfWeek {
            val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            return localDate.dayOfWeek
        }
        //endregion
    }
}
//endregion

//region コンフィグクラス
object Config {
    // 設定情報を保持するPropertiesオブジェクト
    private val properties = Properties()

    // 初期化ブロック
    init {
        // config.propertiesファイルをリソースから読み込む
        val inputStream = Config::class.java.classLoader.getResourceAsStream("config.properties")
        properties.load(inputStream)
    }

    /**
     * OpenWeatherのAPIキーを取得
     * @return OpenWeatherのAPIキー
     */
    fun getOpenWeatherApiKey(): String {
        return properties.getProperty("openweather.api.key")
    }

    /**
     * GoogleMapsのAPIキーを取得
     * @return GoogleMapsのAPIキー
     */
    fun getGoogleMapsApiKey(): String {
        return properties.getProperty("googlemaps.api.key")
    }
}
//endregion