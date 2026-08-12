package com.sky.mvi.network.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.sky.mvi.util.NetworkUtil

/**
 * @Class: NetworkStateReceive
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: 网络状态监听。基于 [ConnectivityManager.NetworkCallback] 实现，
 * 取代已废弃的 CONNECTIVITY_ACTION 广播，适配 targetSdk 34+。
 */

class NetworkStateReceive(context: Context) {

    private val appContext = context.applicationContext
    private val manager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            NetworkStateManager.instance.updateNetworkState(true)
        }

        override fun onLost(network: Network) {
            NetworkStateManager.instance.updateNetworkState(false)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            // 能力变化（如从受限网络切到正常网络）也需要同步一次
            val hasInternet =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            NetworkStateManager.instance.updateNetworkState(hasInternet)
        }
    }

    /**
     * 注册监听。首次注册时立即同步一次当前状态。
     */
    fun register() {
        manager.registerNetworkCallback(request, callback)
        NetworkStateManager.instance.updateNetworkState(
            NetworkUtil.isNetworkAvailable(appContext)
        )
    }

    /**
     * 注销监听，避免内存泄漏
     */
    fun unregister() {
        runCatching { manager.unregisterNetworkCallback(callback) }
    }
}
