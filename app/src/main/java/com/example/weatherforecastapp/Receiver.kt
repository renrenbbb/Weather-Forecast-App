package com.example.weatherforecastapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * レシーバークラス関連
 */

//region レシーバークラス
/**
 * カスタムBroadcastReceiver
 */
class CustomReceiver(private val listener: DateChangeListener) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // 日付の変更を検知
        if (intent?.action == Intent.ACTION_DATE_CHANGED) {
            listener.onDateChanged()
        }
    }
}
//endregion

//region リスナークラス
/**
 * 日付変更リスナー
 */
interface DateChangeListener {
    fun onDateChanged()
}
//endregion