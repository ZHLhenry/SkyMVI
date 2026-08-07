package com.sky.mvi.sample.ui.login

import androidx.navigation.NavHostController
import com.sky.mvi.mvi.UiEffect
import com.sky.mvi.mvi.UiIntent
import com.sky.mvi.mvi.UiState
import com.sky.mvi.mvi.navigation.NavigationEffect
import com.sky.mvi.mvi.navigation.navigateTo
import com.sky.mvi.sample.ui.Routes
import com.sky.mvi.widget.toast.ToastEffect

/**
 * 登录页 MVI 契约
 */
data class LoginState(
    val account: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val loginError: String? = null
) : UiState

sealed interface LoginIntent : UiIntent {
    data class AccountChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
    data object Submit : LoginIntent
}

sealed interface LoginEffect : UiEffect {
    data class ShowToast(val msg: String) : LoginEffect, ToastEffect {
        override val message: String get() = msg
    }

    /** 登录成功：跳转首页并清空登录栈，由 [com.sky.mvi.mvi.rememberMviEffectHandler] 自动分发 */
    data object NavigateHome : LoginEffect, NavigationEffect {
        override fun handle(controller: NavHostController) =
            controller.navigateTo(Routes.Home.pattern, popUpToRoute = Routes.Login.pattern, inclusive = true)
    }
}
