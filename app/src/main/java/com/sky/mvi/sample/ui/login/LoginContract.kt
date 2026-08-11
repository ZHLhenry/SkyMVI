package com.sky.mvi.sample.ui.login

import androidx.navigation.NavHostController
import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState
import com.sky.mvi.core.navigation.SkyNavigationEffect
import com.sky.mvi.core.navigation.skyNavigateTo
import com.sky.mvi.sample.ui.Routes
import com.sky.mvi.widget.toast.SkyToastEffect

/**
 * 登录页 MVI 契约
 */
data class LoginState(
    val account: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val loginError: String? = null
) : SkyUiState

sealed interface LoginIntent : SkyUiIntent {
    data class AccountChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
    data object Submit : LoginIntent
}

sealed interface LoginEffect : SkyUiEffect {
    data class ShowToast(val msg: String) : LoginEffect, SkyToastEffect {
        override val message: String get() = msg
    }

    /** 跳转首页，由 [com.sky.mvi.core.rememberSkyMviEffectHandler] 自动分发 */
    data object NavigateHome : LoginEffect, SkyNavigationEffect {
        override fun handle(controller: NavHostController) =
            controller.skyNavigateTo(Routes.Home.pattern)
    }
}
