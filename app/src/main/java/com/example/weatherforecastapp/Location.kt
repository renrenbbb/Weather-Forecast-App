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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.nio.charset.Charset
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * 位置情報クラス
 */
class Location {
    //region 静的メンバー
    companion object {
        /**
         * 位置情報の権限を許可する
         * @param context 呼び出し元のActivityコンテキスト
         */
        fun checkAndRequestLocationPermission(context: Activity) {
            // 位置情報の権限が許可されているか確認
            if (!checkLocationPermission(context)) {
                // 許可されていない場合は権限のリクエストを行う
                requestLocationPermission(context)
            }
        }

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
        private fun requestLocationPermission(context: Activity) {
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
                    val fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(context)

                    // 権限が許可されている場合の処理をコルーチンで実行
                    suspendCancellableCoroutine { continuation ->
                        // 権限チェック
                        if (checkLocationPermission(context)) {
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { location: Location? ->
                                    location?.let {
                                        // 現在の位置情報を取得できた場合
                                        continuation.resume(Coordinate(it.latitude, it.longitude))
                                    } ?: run {
                                        // 現在の位置情報を取得できなかった場合
                                        continuation.resume(
                                            Coordinate(
                                                LATITUDE_TOKYO,
                                                LONGITUDE_TOKYO
                                            )
                                        )
                                    }
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
         * Google Maps APIを利用して座標から都市名を取得
         * @param latitude 緯度
         * @param longitude 経度
         * @param context 呼び出し元のActivityコンテキスト
         * @return 座標から取得した都市名、取得できない場合は「不明」
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
                            // 都市情報が含まれる場合
                            if (types.toString().contains("administrative_area_level_1")) {
                                // 都市名を返す
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
            // TextStyleによって「月」「月曜日」など表示を変更
            return getDayOfWeek(date).getDisplayName(textStyle, Locale.getDefault())
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