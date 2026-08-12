@file:Suppress("DEPRECATION")

package com.sky.mvi.network

import android.content.Context
import android.net.ParseException
import org.json.JSONException
import retrofit2.HttpException
import java.net.ConnectException

/**
 * @Class: ExceptionHandle
 * @Author: Henry
 * @Date: 2025/2/23 10:33
 * @Description: 异常处理工具，将各类网络异常转换为统一的AppException
 */

object ExceptionHandle {

    fun handleException(context: Context, e: Throwable?): AppException {
        val ex: AppException
        e?.let {
            when (it) {
                is HttpException -> {
                    ex = AppException(context,Error.NETWORK_ERROR, e)
                    return ex
                }

                is JSONException, is ParseException -> {
                    ex = AppException(context,Error.PARSE_ERROR, e)
                    return ex
                }

                is ConnectException -> {
                    ex = AppException(context,Error.NETWORK_ERROR, e)
                    return ex
                }

                is javax.net.ssl.SSLException -> {
                    ex = AppException(context,Error.SSL_ERROR, e)
                    return ex
                }

                is java.net.SocketTimeoutException -> {
                    ex = AppException(context,Error.TIMEOUT_ERROR, e)
                    return ex
                }

                is java.net.UnknownHostException -> {
                    ex = AppException(context,Error.TIMEOUT_ERROR, e)
                    return ex
                }

                is AppException -> return it

                else -> {
                    ex = AppException(context,Error.UNKNOWN, e)
                    return ex
                }
            }
        }
        ex = AppException(context,Error.UNKNOWN, e)
        return ex
    }
}