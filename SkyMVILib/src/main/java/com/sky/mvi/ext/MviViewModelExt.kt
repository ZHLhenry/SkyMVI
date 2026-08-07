package com.sky.mvi.ext

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sky.mvi.R
import com.sky.mvi.ext.util.logE
import com.sky.mvi.mvi.BaseMviViewModel
import com.sky.mvi.mvi.UiEffect
import com.sky.mvi.mvi.UiIntent
import com.sky.mvi.mvi.UiState
import com.sky.mvi.network.AppException
import com.sky.mvi.network.BaseResponse
import com.sky.mvi.network.ExceptionHandle
import com.sky.mvi.network.state.ResultState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @Class: MviViewModelExt
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: MVI ViewModel 网络请求扩展，封装请求、状态解析与异常处理
 */

// =====================================================================
// 回调式：适合在 handleIntent 中直接 setState
// =====================================================================

/**
 * 发起网络请求，自动校验业务状态码，失败走 error 回调
 *
 * ```
 * apiRequest(
 *     context = context,
 *     block = { api.getArticleList(page) },
 *     success = { setState { copy(list = it.datas, pageState = PageState.Success) } },
 *     error = { setState { copy(pageState = PageState.error(it)) } }
 * )
 * ```
 *
 * @param context 上下文，用于读取错误文案
 * @param block 请求体，必须用 suspend 修饰
 * @param success 成功回调
 * @param error 失败回调，可不传
 * @param onStart 请求开始回调，常用于置 loading 态
 * @param onComplete 请求结束回调（无论成败都会执行）
 */
fun <S : UiState, I : UiIntent, E : UiEffect, T> BaseMviViewModel<S, I, E>.apiRequest(
    context: Context,
    block: suspend () -> BaseResponse<T>,
    success: (T) -> Unit,
    error: (AppException) -> Unit = {},
    onStart: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null
): Job {
    val appContext = context.applicationContext
    return viewModelScope.launch {
        onStart?.invoke()
        runCatching {
            block()
        }.onSuccess { response ->
            runCatching {
                executeResponse(appContext, response) { data -> success(data) }
            }.onFailure { e ->
                e.message?.logE()
                e.printStackTrace()
                error(ExceptionHandle.handleException(appContext, e))
            }
        }.onFailure { e ->
            e.message?.logE()
            e.printStackTrace()
            error(ExceptionHandle.handleException(appContext, e))
        }
        // 即使协程被取消也保证收尾逻辑执行，避免 loading 卡死
        withContext(NonCancellable) { onComplete?.invoke() }
    }
}

/**
 * 发起网络请求，不校验业务状态码，直接回传原始结果
 *
 * @param context 上下文
 * @param block 请求体，必须用 suspend 修饰
 * @param success 成功回调
 * @param error 失败回调，可不传
 * @param onStart 请求开始回调
 * @param onComplete 请求结束回调
 */
fun <S : UiState, I : UiIntent, E : UiEffect, T> BaseMviViewModel<S, I, E>.apiRequestNoCheck(
    context: Context,
    block: suspend () -> T,
    success: (T) -> Unit,
    error: (AppException) -> Unit = {},
    onStart: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null
): Job {
    val appContext = context.applicationContext
    return viewModelScope.launch {
        onStart?.invoke()
        runCatching {
            block()
        }.onSuccess {
            success(it)
        }.onFailure { e ->
            e.message?.logE()
            e.printStackTrace()
            error(ExceptionHandle.handleException(appContext, e))
        }
        withContext(NonCancellable) { onComplete?.invoke() }
    }
}

// =====================================================================
// StateFlow 式：把请求三态写入 StateFlow，由 UI 统一消费
// =====================================================================

/**
 * 发起网络请求并将三态写入 [resultState]，自动校验业务状态码
 *
 * @param context 上下文
 * @param block 请求体
 * @param resultState 承载结果的 StateFlow
 * @param isShowLoading 是否先发出 Loading 态
 * @param loadingMessage 加载提示文案
 */
fun <S : UiState, I : UiIntent, E : UiEffect, T> BaseMviViewModel<S, I, E>.apiRequest(
    context: Context,
    block: suspend () -> BaseResponse<T>,
    resultState: MutableStateFlow<ResultState<T>>,
    isShowLoading: Boolean = false,
    loadingMessage: String = context.getString(R.string.sky_mvilib_loading_message)
): Job {
    val appContext = context.applicationContext
    return viewModelScope.launch {
        if (isShowLoading) resultState.value = ResultState.onAppLoading(loadingMessage)
        runCatching {
            block()
        }.onSuccess { response ->
            resultState.value = if (response.isSucces()) {
                ResultState.onAppSuccess(response.getResponseData())
            } else {
                ResultState.onAppError(
                    AppException(appContext, response.getResponseCode(), response.getResponseMsg())
                )
            }
        }.onFailure { e ->
            e.message?.logE()
            e.printStackTrace()
            resultState.value = ResultState.onAppError(
                ExceptionHandle.handleException(appContext, e)
            )
        }
    }
}

/**
 * 发起网络请求并将三态写入 [resultState]，不校验业务状态码
 */
fun <S : UiState, I : UiIntent, E : UiEffect, T> BaseMviViewModel<S, I, E>.apiRequestNoCheck(
    context: Context,
    block: suspend () -> T,
    resultState: MutableStateFlow<ResultState<T>>,
    isShowLoading: Boolean = false,
    loadingMessage: String = context.getString(R.string.sky_mvilib_loading_message)
): Job {
    val appContext = context.applicationContext
    return viewModelScope.launch {
        if (isShowLoading) resultState.value = ResultState.onAppLoading(loadingMessage)
        runCatching {
            block()
        }.onSuccess {
            resultState.value = ResultState.onAppSuccess(it)
        }.onFailure { e ->
            e.message?.logE()
            e.printStackTrace()
            resultState.value = ResultState.onAppError(
                ExceptionHandle.handleException(appContext, e)
            )
        }
    }
}

// =====================================================================
// Flow 式：适合在 Repository 层组合、在 ViewModel 中 collect
// =====================================================================

/**
 * 把一次请求包装为发射 Loading → Success/Error 的冷流，
 * 适合放在 Repository 层，让 ViewModel 直接 collect 后 setState。
 *
 * ```
 * // Repository
 * fun articles(page: Int) = apiFlow(context) { api.getArticleList(page) }
 *
 * // ViewModel
 * repo.articles(1).onEach { state ->
 *     state.parseState(
 *         onSuccess = { setState { copy(list = it.datas) } },
 *         onError = { setState { copy(pageState = PageState.error(it)) } }
 *     )
 * }.launchIn(viewModelScope)
 * ```
 *
 * @param context 上下文
 * @param loadingMessage 加载提示文案，为 null 则不发射 Loading
 * @param block 请求体
 */
fun <T> apiFlow(
    context: Context,
    loadingMessage: String? = null,
    block: suspend () -> BaseResponse<T>
): Flow<ResultState<T>> {
    val appContext = context.applicationContext
    return flow {
        val response = block()
        if (response.isSucces()) {
            emit(ResultState.onAppSuccess(response.getResponseData()))
        } else {
            emit(
                ResultState.onAppError(
                    AppException(appContext, response.getResponseCode(), response.getResponseMsg())
                )
            )
        }
    }.onStart {
        if (loadingMessage != null) emit(ResultState.onAppLoading(loadingMessage))
    }.catch { e ->
        e.message?.logE()
        emit(ResultState.onAppError(ExceptionHandle.handleException(appContext, e)))
    }.flowOn(Dispatchers.IO)
}

/**
 * 同 [apiFlow]，但不校验业务状态码
 */
fun <T> apiFlowNoCheck(
    context: Context,
    loadingMessage: String? = null,
    block: suspend () -> T
): Flow<ResultState<T>> {
    val appContext = context.applicationContext
    return flow {
        emit(ResultState.onAppSuccess(block()))
    }.onStart {
        if (loadingMessage != null) emit(ResultState.onAppLoading(loadingMessage))
    }.catch { e ->
        e.message?.logE()
        emit(ResultState.onAppError(ExceptionHandle.handleException(appContext, e)))
    }.flowOn(Dispatchers.IO)
}

// =====================================================================
// 通用
// =====================================================================

/**
 * 请求结果过滤：业务码不成功则抛出 [AppException]
 */
suspend fun <T> executeResponse(
    context: Context,
    response: BaseResponse<T>,
    success: suspend CoroutineScope.(T) -> Unit
) {
    coroutineScope {
        when {
            response.isSucces() -> success(response.getResponseData())
            else -> throw AppException(
                context,
                response.getResponseCode(),
                response.getResponseMsg(),
                response.getResponseMsg()
            )
        }
    }
}

/**
 * 在 IO 线程执行耗时任务并回调结果
 *
 * @param block 耗时任务
 * @param success 成功回调（主线程）
 * @param error 失败回调（主线程）
 */
fun <S : UiState, I : UiIntent, E : UiEffect, T> BaseMviViewModel<S, I, E>.launchIO(
    block: suspend () -> T,
    success: (T) -> Unit,
    error: (Throwable) -> Unit = {}
): Job = viewModelScope.launch {
    runCatching {
        withContext(Dispatchers.IO) { block() }
    }.onSuccess {
        success(it)
    }.onFailure {
        it.printStackTrace()
        error(it)
    }
}
