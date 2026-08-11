package com.sky.mvi.sample.ui.login

import androidx.lifecycle.viewModelScope
import com.sky.mvi.core.SkyBaseMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录页 ViewModel：演示「表单输入 -> Intent -> State」的 MVI 链路。
 * 此处为示例，使用延时模拟登录请求，可替换为真实 apiRequest 调用。
 */
@HiltViewModel
class LoginViewModel @Inject constructor() :
    SkyBaseMviViewModel<LoginState, LoginIntent, LoginEffect>() {

    override fun initialState() = LoginState()

    override fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.AccountChanged -> setState { copy(account = intent.value) }
            is LoginIntent.PasswordChanged -> setState { copy(password = intent.value) }
            is LoginIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val account = currentState.account
        val password = currentState.password
        if (account.isBlank() || password.isBlank()) {
            sendEffect(LoginEffect.ShowToast("账号或密码不能为空"))
            return
        }
        setState { copy(isLoading = true, loginError = null) }
        viewModelScope.launch {
            delay(800) // 模拟登录请求
            setState { copy(isLoading = false) }
            sendEffect(LoginEffect.NavigateHome)
        }
    }
}
