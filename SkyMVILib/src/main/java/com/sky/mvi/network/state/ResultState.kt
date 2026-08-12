package com.sky.mvi.network.state

import android.content.Context
import com.sky.mvi.network.AppException
import com.sky.mvi.network.BaseResponse
import com.sky.mvi.network.ExceptionHandle
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * @Class: ResultState
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: 网络请求结果密封类，封装Loading、Success、Error三种状态
 */

sealed class ResultState<out T> {

    companion object {
        fun <T> onAppSuccess(data: T): ResultState<T> = Success(data)
        fun <T> onAppLoading(loadingMessage: String): ResultState<T> = Loading(loadingMessage)
        fun <T> onAppError(error: AppException): ResultState<T> = Error(error)
    }

    data class Loading(val loadingMessage: String) : ResultState<Nothing>()
    data class Success<out T>(val data: T) : ResultState<T>()
    data class Error(val error: AppException) : ResultState<Nothing>()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    /**
     * 成功时取出数据，否则返回 null
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * 失败时取出异常，否则返回 null
     */
    fun exceptionOrNull(): AppException? = (this as? Error)?.error
}

/**
 * 链式处理结果，成功回调必传，其余可省
 *
 * ```
 * result.doSuccess { setState { copy(list = it) } }
 *       .doError { setState { copy(error = it.errorMsg) } }
 * ```
 */
inline fun <T> ResultState<T>.doSuccess(action: (T) -> Unit): ResultState<T> {
    if (this is ResultState.Success) action(data)
    return this
}

inline fun <T> ResultState<T>.doError(action: (AppException) -> Unit): ResultState<T> {
    if (this is ResultState.Error) action(error)
    return this
}

inline fun <T> ResultState<T>.doLoading(action: (String) -> Unit): ResultState<T> {
    if (this is ResultState.Loading) action(loadingMessage)
    return this
}

/**
 * 统一分发三态，等价于 MVVM 版本的 parseState
 *
 * @param onSuccess 成功回调
 * @param onError 失败回调
 * @param onLoading 加载中回调
 */
inline fun <T> ResultState<T>.parseState(
    onSuccess: (T) -> Unit,
    noinline onError: ((AppException) -> Unit)? = null,
    noinline onLoading: ((String) -> Unit)? = null
) {
    when (this) {
        is ResultState.Loading -> onLoading?.invoke(loadingMessage)
        is ResultState.Success -> onSuccess(data)
        is ResultState.Error -> onError?.invoke(error)
    }
}

/**
 * 处理返回值：校验业务码，成功脱壳，失败转 AppException
 * @param result 请求结果
 */
fun <T> MutableStateFlow<ResultState<T>>.parseResult(context: Context, result: BaseResponse<T>) {
    value = when {
        result.isSucces() -> ResultState.onAppSuccess(result.getResponseData())
        else -> ResultState.onAppError(
            AppException(context, result.getResponseCode(), result.getResponseMsg())
        )
    }
}

/**
 * 不处理返回值，直接返回请求结果
 * @param result 请求结果
 */
fun <T> MutableStateFlow<ResultState<T>>.parseResult(result: T) {
    value = ResultState.onAppSuccess(result)
}

/**
 * 异常转换异常处理
 */
fun <T> MutableStateFlow<ResultState<T>>.parseException(context: Context, e: Throwable) {
    value = ResultState.onAppError(ExceptionHandle.handleException(context, e))
}
