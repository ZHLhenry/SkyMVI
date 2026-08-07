package com.sky.mvi.network.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.sky.mvi.util.NetworkUtil

/**
 * @Class: NetworkStateReceive
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: 网络状态广播接收器，监听网络连接变化并更新NetworkStateManager
 */

@Suppress("DEPRECATION")
class NetworkStateReceive : BroadcastReceiver() {

    /**
     * 注册广播时系统会立即回调一次，此处跳过首次回调，避免启动即误报网络变化
     */
    private var isInit = true

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ConnectivityManager.CONNECTIVITY_ACTION) return
        if (!isInit) {
            // StateFlow 内部按值去重，无需再手工比对上一次状态
            NetworkStateManager.instance.updateNetworkState(
                NetworkUtil.isNetworkAvailable(context)
            )
        }
        isInit = false
    }
}
