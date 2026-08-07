package com.sky.mvi.ext.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @Class: KtxAppLifeObserver
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: 应用前后台生命周期观察者，以StateFlow对外暴露App前后台状态
 */

object KtxAppLifeObserver : DefaultLifecycleObserver {

    private val _isForeground = MutableStateFlow(false)

    /**
     * App 是否处于前台，可在 Compose 中通过 collectAsStateWithLifecycle() 订阅
     */
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    /**
     * 同步获取当前前后台状态
     */
    val isAppForeground: Boolean
        get() = _isForeground.value

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        _isForeground.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        _isForeground.value = false
    }
}
