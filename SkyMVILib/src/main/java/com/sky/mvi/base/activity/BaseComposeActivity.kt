package com.sky.mvi.base.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.sky.mvi.SkyMVILib
import com.sky.mvi.network.manager.NetState
import com.sky.mvi.network.manager.NetworkStateManager
import kotlinx.coroutines.flow.StateFlow

/**
 * @Class: BaseComposeActivity
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: Compose Activity 基类，统一初始化校验、边到边显示与网络状态注入
 *
 * 用法：
 * ```
 * @AndroidEntryPoint
 * class MainActivity : BaseComposeActivity() {
 *     @Composable
 *     override fun Content() {
 *         AppTheme { HomeRoute() }
 *     }
 * }
 * ```
 */
abstract class BaseComposeActivity : ComponentActivity() {

    /**
     * 是否开启边到边（沉浸式）显示，子类可重写
     */
    protected open val enableEdgeToEdge: Boolean = true

    /**
     * 页面内容，子类实现
     */
    @Composable
    protected abstract fun Content()

    /**
     * setContent 之前的初始化时机，子类可重写
     */
    protected open fun beforeSetContent(savedInstanceState: Bundle?) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        if (enableEdgeToEdge) enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SkyMVILib.requireInit()
        beforeSetContent(savedInstanceState)
        setContent {
            // 向下层 Composable 提供全局网络状态，子树可通过
            // LocalNetworkState.current 直接读取，无需层层透传
            CompositionLocalProvider(
                LocalNetworkState provides NetworkStateManager.instance.networkState
            ) {
                Content()
            }
        }
    }
}

/**
 * 全局网络状态 CompositionLocal。
 *
 * ```
 * val netState by LocalNetworkState.current.collectAsStateWithLifecycle()
 * if (!netState.isSuccess) NoNetworkBanner()
 * ```
 */
val LocalNetworkState = staticCompositionLocalOf<StateFlow<NetState>> {
    error("LocalNetworkState 未提供，请确保页面挂载在 BaseComposeActivity 之下")
}
