package com.sky.mvi.core.common

import android.annotation.SuppressLint
import android.content.Context

/**
 * 全局 Context 持有者。在 Application#onCreate 中调用 [init] 完成初始化。
 */
@SuppressLint("StaticFieldLeak")
object AppContext {
    private lateinit var context: Context

    fun init(context: Context) {
        AppContext.context = context
    }

    val appContext: Context
        get() = context
}
