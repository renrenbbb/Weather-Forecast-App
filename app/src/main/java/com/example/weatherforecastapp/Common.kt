package com.example.weatherforecastapp

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Properties

/**
 * 共通の変数・関数群
 */

//region 定数・変数
// 権限のリクエストコード
const val LOCATION_PERMISSION_REQUEST_CODE = 1001

// データファイル名
const val WEATHER_FORECAST_APP_PREF = "WeatherForecastAppPref"

// Open Weather APIキー
val OPENWEATHER_API_KEY = Config.getOpenWeatherApiKey()

// Google Maps APIキー
val GOOGLEMAPS_API_KEY = Config.getGoogleMapsApiKey()

//緯度・経度
const val LATITUDE_TOKYO = 35.6895
const val LONGITUDE_TOKYO = 139.6917

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
        fun convertStringToDate(dateString: String, format: String): Date {
            val formatter = DateTimeFormatter.ofPattern(format)
            val localDate = LocalDate.parse(dateString, formatter)

            return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        }

        /**
         * UTCからJSTへ変換
         * @param utcTime UTCのタイムスタンプ
         * @return JSTのZonedDateTime
         */
        fun convertUTCtoJST(utcTime: Long): ZonedDateTime {
            val instant = Instant.ofEpochSecond(utcTime)
            return ZonedDateTime.ofInstant(instant, ZoneId.of("Asia/Tokyo"))
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
        val inputStream = Config::class.java.classLoader?.getResourceAsStream("config.properties")
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