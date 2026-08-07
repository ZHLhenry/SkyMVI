@file:Suppress("UNCHECKED_CAST")

package com.sky.mvi.flow

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sky.mvi.SkyMVILib
import com.sky.mvi.SkyMVILib.UninitializedException
import com.sky.mvi.ext.util.logE
import com.sky.mvi.ext.util.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * @Class: SkyFlow
 * @Author: Henry
 * @Date: 2025/3/5 11:01
 * @Description: 基于SharedFlow的事件总线，支持普通事件流和粘性事件流
 */

object SkyFlow {
    private const val TAG = "SkyFlow"
    private val flowMap = ConcurrentHashMap<String, SkyFlowEvent<*>>()
    private val flowStickMap = ConcurrentHashMap<String, SkyFlowStickEvent<*>>()

    fun requireSkyFlowInit() {
        if (SkyMVILib.getConfig()?.skyFlowLibEnabled == false) {
            throw UninitializedException(
                "Please add the \"enableSkyFlowLib(true)\" attribute in the SkyMVILibConfig configuration."
            )
        }
    }

    /**
     * 获取或创建普通事件流
     */
    fun <T> with(key: String): SkyFlowEvent<T> {
        SkyMVILib.requireInit()
        requireSkyFlowInit()
        return flowMap.getOrPut(key) { SkyFlowEvent<T>(key) } as SkyFlowEvent<T>
    }

    /**
     * 获取或创建粘性事件流
     */
    fun <T> withStick(key: String): SkyFlowStickEvent<T> {
        SkyMVILib.requireInit()
        requireSkyFlowInit()
        return flowStickMap.getOrPut(key) { SkyFlowStickEvent<T>(key) } as SkyFlowStickEvent<T>
    }

    /**
     * 清理无用的 Flow
     */
    fun clearUnusedFlow() {
        SkyMVILib.requireInit()
        requireSkyFlowInit()
        flowMap.keys.removeAll { key ->
            val flow = flowMap[key]
            flow?._events?.subscriptionCount?.value == 0
        }
        flowStickMap.keys.removeAll { key ->
            val flowStick = flowStickMap[key]
            flowStick?._events?.subscriptionCount?.value == 0
        }
    }

    /**
     * 普通事件流
     */
    open class SkyFlowEvent<T>(private val key: String) : DefaultLifecycleObserver {
        // 使用默认参数的 SharedFlow
        internal open val _events by lazy {
            MutableSharedFlow<T>(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }
        private val events = _events.asSharedFlow()

        /**
         * 注册事件监听
         * @param lifecycleOwner 生命周期所有者，用于自动取消订阅
         * @param scope 协程作用域
         * @param context 协程上下文
         * @param filter 事件过滤条件
         * @param action 事件处理逻辑
         */
        fun register(
            lifecycleOwner: LifecycleOwner? = null,
            scope: CoroutineScope? = null,
            context: CoroutineContext = Dispatchers.Main,
            filter: (T) -> Boolean = { true },
            action: (t: T) -> Unit
        ) {
            val targetScope = lifecycleOwner?.lifecycleScope ?: scope
            requireNotNull(targetScope) { "Either lifecycleOwner or scope must be provided" }

            targetScope.launch(context) {
                events.filter(filter).collect { event ->
                    try {
                        action(event)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        "SkyFlowEvent - Error: $e".logE(tag = TAG)
                    }
                }
            }

            // 绑定生命周期
            lifecycleOwner?.lifecycle?.addObserver(this)
        }

        /**
         * 发送事件（挂起函数）
         */
        suspend fun post(event: T) {
            try {
                _events.emit(event)
            } catch (e: Exception) {
                e.printStackTrace()
                "SkyFlowEvent - Post Error: $e".logE(tag = TAG)
            }
        }

        /**
         * 发送事件（非挂起函数）
         */
        fun post(scope: CoroutineScope, event: T) {
            scope.launch {
                try {
                    _events.emit(event)
                } catch (e: Exception) {
                    e.printStackTrace()
                    "SkyFlowEvent - Post Error: $e".logE(tag = TAG)
                }
            }
        }

        /**
         * 生命周期结束时自动清理
         */
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            "SkyFlowEvent - Auto Destroy: $key".logI(tag = TAG)
            if (_events.subscriptionCount.value == 0) {
                flowMap.remove(key)
            }
        }

        /**
         * 手动销毁
         */
        fun destroy() {
            "SkyFlowEvent - Manual Destroy: $key".logI(tag = TAG)
            if (_events.subscriptionCount.value == 0) {
                flowMap.remove(key)
            }
        }
    }

    /**
     * 粘性事件流
     */
    class SkyFlowStickEvent<T>(key: String) : SkyFlowEvent<T>(key) {
        override val _events by lazy {
            MutableSharedFlow<T>(
                replay = 1, // 粘性事件，保留最新一个事件
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }
    }
}