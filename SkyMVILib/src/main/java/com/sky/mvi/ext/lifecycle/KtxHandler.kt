package com.sky.mvi.ext.lifecycle

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * @Class: KtxHandler
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: 生命周期感知的Handler，随Lifecycle自动清理消息防止内存泄漏
 */

class KtxHandler(
    private val lifecycleOwner: LifecycleOwner,
    callback: Callback
) : Handler(Looper.getMainLooper(), callback), DefaultLifecycleObserver {

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        removeCallbacksAndMessages(null)
        lifecycleOwner.lifecycle.removeObserver(this)
    }
}
