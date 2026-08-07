package com.sky.mvi.network.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @Class: NetworkStateManager
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: 网络状态管理器，单例模式以StateFlow对外暴露网络连通状态
 */

class NetworkStateManager private constructor() {

    private val _networkState = MutableStateFlow(NetState(isSuccess = true))

    /**
     * 网络状态流，可在 Compose 中通过 collectAsStateWithLifecycle() 订阅，
     * 或在 ViewModel 中通过 collect 监听
     */
    val networkState: StateFlow<NetState> = _networkState.asStateFlow()

    /**
     * 同步获取当前网络是否可用
     */
    val isNetworkAvailable: Boolean
        get() = _networkState.value.isSuccess

    /**
     * 更新网络状态（内部广播接收器调用）
     */
    internal fun updateNetworkState(isSuccess: Boolean) {
        // StateFlow 自带去重，相同值不会重复通知下游
        _networkState.value = NetState(isSuccess = isSuccess)
    }

    companion object {
        val instance: NetworkStateManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            NetworkStateManager()
        }
    }
}
