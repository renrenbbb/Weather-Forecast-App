package com.example.weatherforecastapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

/**
 * 天気情報クラス
 */
class Weather {
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
            // OpenWeatherAPIのURLをセット
            val apiUrl =
                "http://api.openweathermap.org/data/2.5/forecast?q=$city&APPID=$OPENWEATHER_API_KEY&units=metric&lang=ja"

            try {
                // 天気取得API処理の実行
                val response = withContext(Dispatchers.IO) { exeGet5dayWeather(apiUrl) }

                var weatherInfo: WeatherInfo? = null

                if (response != null) {
                    // JSONデータから天気データを抽出
                    val weatherData = extractWeatherDataFromJSON(response)
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

                return if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
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
                val jstDateTime = Common.convertUTCtoJST(utcTimestamp)
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