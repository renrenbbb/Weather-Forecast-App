package com.example.weatherforecastapp

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Date

/**
 * 天気情報クラス
 */
class Weather {

    //region インターフェース
    /**
     * Weather API用のインターフェース
     */
    interface WeatherApiService {
        @GET("forecast")
        fun get5dayWeather(
            @Query("q") city: String,
            @Query("APPID") apiKey: String,
            @Query("units") units: String = "metric",
            @Query("lang") lang: String = "ja"
        ): Call<WeatherResponse>
    }
    //endregion

    //region データクラス
    //region Weather API用データクラス
    data class WeatherResponse(
        @SerializedName("list") val list: List<Forecast>
    )

    data class Forecast(
        @SerializedName("dt") val dt: Long,
        @SerializedName("main") val main: Main,
        @SerializedName("weather") val weather: List<Weather>
    )

    data class Main(
        @SerializedName("temp") val temp: Double
    )

    data class Weather(
        @SerializedName("main") val main: String,
        @SerializedName("icon") val icon: String
    )
    //endregion
    //endregion

    //region 静的メンバー
    companion object {

        //region アプリへの保存関連
        /**
         * 5日間の天気予報情報をアプリへ保存するためにJSON文字列を作成
         * @param weatherInfo 天気情報クラス
         * @return 生成されたJSON文字列
         */
        fun createJsonToSaveWeather(weatherInfo: WeatherInfo): String {
            val jsonBuilder = StringBuilder()
            jsonBuilder.append("{\n")
            jsonBuilder.append("\"city\": \"${weatherInfo.city}\",\n")

            weatherInfo.weatherDataList.forEachIndexed { index, weatherData ->
                jsonBuilder.append(
                    "\"date$index\": \"${
                        Common.convertDateToString(
                            weatherData.date,
                            "yyyy/MM/dd"
                        )
                    }\",\n"
                )
                jsonBuilder.append("\"weather$index\": \"${weatherData.weather}\",\n")
                jsonBuilder.append("\"weathericon$index\": \"${weatherData.weatherIcon}\",\n")
                jsonBuilder.append("\"temperature$index\": \"${weatherData.temperature}\"")

                // 最後の要素以外はカンマと改行を追加
                if (index < weatherInfo.weatherDataList.size - 1) {
                    jsonBuilder.append(",\n")
                }
            }

            jsonBuilder.append("\n}")

            val json = jsonBuilder.toString()
            return json
        }

        /**
         * アプリへ保存した5日間の天気予報を取得
         * @param savedJson 保存されたJSON文字列
         * @return 復元された天気情報クラス、取得できない場合はnull
         */
        private fun getSaveWeather(savedJson: String): WeatherInfo? {

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
                        val date = Common.convertStringToDate(dateString, "yyyy/MM/dd")
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

        /**
         * OpenWeatherから5日間の天気を取得
         * @param city 天気を取得する都市
         * @return APIから取得した天気データクラス
         */
        suspend fun get5dayWeather(city: String, savedJson: String): WeatherInfo? {

            try {
                // 天気取得API処理の実行
                val weatherResponse = withContext(Dispatchers.IO) { exeGet5dayWeather(city) }

                if (weatherResponse != null) {
                    // JSONデータから天気データを抽出
                    val weatherData = extractWeatherDataFromJSON(weatherResponse)
                    // 天気情報クラスを作成して返す
                    return WeatherInfo(city, weatherData)
                }

                // 天気情報を取得できなかった場合はアプリへ保存した5日間の天気予報を取得
                return getSaveWeather(savedJson)
            } catch (e: Exception) {
                println(e.message)
            }

            return null
        }

        /**
         * 天気取得API処理の実行
         * @param city 天気を取得する都市
         * @return APIから取得したJSON形式の天気情報
         */
        private fun exeGet5dayWeather(city: String): WeatherResponse? {
            val retrofit = Retrofit.Builder()
                .baseUrl(OPENWEATHER_BASEURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val weatherApiService = retrofit.create(WeatherApiService::class.java)

            try {
                val response = weatherApiService.get5dayWeather(city, OPENWEATHER_API_KEY).execute()
                if (response.isSuccessful) {
                    return response.body()
                }
            } catch (e: Exception) {
                println(e.message)
            }
            return null
        }

        /**
         * JSONデータから天気データを抽出
         * @param response APIからのJSON形式の応答データ
         * @return 抽出した天気データのリスト
         */
        private fun extractWeatherDataFromJSON(
            weatherResponse: WeatherResponse
        ): MutableList<WeatherData> {
            val listArray = weatherResponse.list

            // 日付ごとの気温と天気を格納するMap
            val dateMap: MutableMap<String, TotalWeatherData> = mutableMapOf()
            val dateList: MutableList<Date> = mutableListOf()

            for (item in listArray) {
                val utcTimestamp = item.dt
                // 日付はUTCからJSTへ変換
                val jstDateTime = Common.convertUTCtoJST(utcTimestamp)
                val date = Date.from(jstDateTime.toInstant())
                // 気温
                val temperature = item.main.temp.toInt()
                // 天気
                val weather = item.weather.firstOrNull()?.main ?: ""
                // 天気アイコン
                val weatherIcon = item.weather.firstOrNull()?.icon ?: ""

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

            /// 日付ごとに平均気温と平均天気を計算し、リストに追加
            val weatherDataList: MutableList<WeatherData> = mutableListOf()
            for ((_, value) in dateMap) {
                // 平均気温を計算
                val averageTemperature = value.totalTemperature / value.weatherList.size
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
                        dateList[weatherDataList.size], // 対応する日付を取得
                        averageFrequentWeather,
                        averageFrequentWeatherIcon,
                        averageTemperature
                    )
                )
            }

            return weatherDataList
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
            return if (isToday) {
                "https://openweathermap.org/img/wn/$editIcon@4x.png"
            } else {
                "https://openweathermap.org/img/wn/$editIcon.png"
            }
        }
    }
}