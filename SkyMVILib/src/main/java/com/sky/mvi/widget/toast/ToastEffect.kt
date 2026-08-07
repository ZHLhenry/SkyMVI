package com.sky.mvi.widget.toast

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sky.mvi.mvi.UiEffect
import com.sky.mvi.mvi.compose.CollectEffect
import kotlinx.coroutines.flow.Flow

/**
 * 标记接口：实现该接口的 [UiEffect] 会被 [HandleToastEffects] 直接以 Toast 弹出，
 * 省去在每个屏幕里手写 `Toast.makeText`。
 *
 * ```
 * sealed interface HomeEffect : UiEffect {
 *     data class ShowToast(val msg: String) : HomeEffect, ToastEffect {
 *         override val message: String get() = msg
 *     }
 * }
 * ```
 */
interface ToastEffect : UiEffect {
    val message: String
}

/**
 * 生命周期感知地收集 [UiEffect]，自动以 Toast 弹出 [ToastEffect]，其余交回 [onEffect]。
 *
 * 注意：[UiEffect] 由 Channel 驱动，只能被「一个」收集器消费。
 * 若屏幕已通过 [com.sky.mvi.mvi.compose.MviScreen] 收集副作用，请改用
 * [com.sky.mvi.mvi.rememberMviEffectHandler] 传入 MviScreen 的 onEffect，
 * 不要再单独调用本方法。
 */
@Composable
fun HandleToastEffects(
    effectFlow: Flow<UiEffect>,
    onEffect: (UiEffect) -> Unit = {}
) {
    val context = LocalContext.current
    CollectEffect(flow = effectFlow) { effect ->
        if (effect is ToastEffect) {
            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        } else {
            onEffect(effect)
        }
    }
}
