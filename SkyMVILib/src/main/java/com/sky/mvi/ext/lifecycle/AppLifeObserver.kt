package com.sky.mvi.ext.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @Class: AppLifeObserver
 * @Author: Henry
 * @Date: 2025/2/23 08:58
 * @Description: 应用前后台状态观察者。
 *
 * 通过 ProcessLifecycleOwner 监听 App 级生命周期（而非单个 Activity），
 * 以 [isForegroundState] [MutableStateFlow] 暴露状态，供 MVI ViewModel 订阅，
 * 例如：前台恢复刷新数据、后台暂停定时器、统计在线时长等。
 *
 * 注册方式：
 * ```kotlin
 * ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifeObserver)
 * ```
 */
object AppLifeObserver : LifecycleObserver {

    private val _isForeground = MutableStateFlow(false)
    /** 应用是否处于前台（只读流） */
    val isForegroundState = _isForeground.asStateFlow()

    val isForeground: Boolean get() = _isForeground.value
    val isBackground: Boolean get() = !_isForeground.value

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForeground() {
        _isForeground.value = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onBackground() {
        _isForeground.value = false
    }
}
