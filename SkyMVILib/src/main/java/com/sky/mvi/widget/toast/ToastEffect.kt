package com.sky.mvi.widget.toast

import android.content.Context
import android.widget.Toast
import com.sky.mvi.core.SkyUiEffect

/**
 * @Class: SkyToastEffect
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: MVI 框架下的 Toast 副作用契约
 */

/**
 * Toast 副作用标记接口。
 *
 * ViewModel 的 Effect 实现此接口后，可被 [SkyHandleToastEffects] 自动弹出 Toast。
 */
interface SkyToastEffect : SkyUiEffect {
    val message: String
    val duration: Int get() = Toast.LENGTH_SHORT
}

/**
 * 处理一个 Toast 副作用
 */
fun SkyHandleToastEffects(context: Context, effect: SkyToastEffect) {
    Toast.makeText(context, effect.message, effect.duration).show()
}
